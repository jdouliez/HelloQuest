"""Prepare every spoken line and the Android Auto media tracks before APK build.

Requires edge-tts and ffmpeg. Cached clips are reused on subsequent runs.
The application never calls this tool or a speech synthesis service at runtime.
"""
import argparse
import asyncio
import hashlib
import json
import os
import shutil
import subprocess
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import edge_tts

ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "content/dialogues-poc.json"
CATALOG = json.loads(CATALOG_PATH.read_text())
AUDIO = ROOT / "android/app/src/main/assets/audio"
CLIPS = AUDIO / "clips"
TRACKS = ROOT / "android/audio_pack/src/main/assets/audio/tracks"
VOICES = {
    "fr-FR": "fr-FR-DeniseNeural",
    "en-GB": "en-GB-SoniaNeural",
    "en-US": "en-US-JennyNeural",
}


def key(locale, pace, text):
    return f"{locale}|{pace}|{text}"


def spoken(locale, text):
    if locale == "en":
        yield ("en-GB", text)
        yield ("en-US", text)
    else:
        yield (locale, text)


def collect_lines():
    lines = set()

    def add(locale, text, slow=False):
        for real_locale, value in spoken(locale, text):
            lines.add((real_locale, "natural", value))
            if slow and real_locale.startswith("en-"):
                lines.add((real_locale, "slow", value))

    def add_segments(segments, slow=False):
        for segment in segments:
            add(segment["locale"], segment["text"], slow)

    for segments in CATALOG["shared"].values():
        add_segments(segments)
    for mission in CATALOG["universes"]:
        add_segments(mission["intro"])
        add_segments(mission["outro"])
        for dialogue in mission.get("miniDialogues", []):
            for line in dialogue["lines"]:
                add("en", line)
            add("fr-FR", dialogue["question"])
        for passage in mission.get("readingPassages", []):
            for sentence in passage["sentences"]:
                add("en", sentence)
            add("fr-FR", passage["question"])
            add("fr-FR", "La bonne réponse est : " +
                passage["choices"][passage["correctIndex"]] + ".")
        for chapter in mission["lessons"]:
            add_segments(chapter["intro"])
            for card in chapter["theory"]:
                add_segments(card["audio"])
            for exercise in chapter["exercises"]:
                add_segments(exercise.get("questionAudio", [{"locale": exercise["questionLocale"],
                                                              "text": exercise["question"]}]))
                add("en", exercise["model"], slow=True)
                add_segments(exercise["hint"], slow=True)
                add_segments(exercise.get("contextAudio", []))
    return sorted(lines)


def clip_name(locale, pace, text):
    digest = hashlib.sha256(key(locale, pace, text).encode()).hexdigest()[:20]
    return f"{locale}_{pace}_{digest}.mp3"


async def generate_one(semaphore, locale, pace, text, force=False):
    path = CLIPS / clip_name(locale, pace, text)
    if path.exists() and path.stat().st_size > 1000 and not force:
        return
    async with semaphore:
        for attempt in range(4):
            temporary = path.with_suffix(".part.mp3")
            try:
                spoken_text = text if attempt < 2 or len(text.strip()) > 3 else text.rstrip(" .") + "."
                task = edge_tts.Communicate(
                    spoken_text, VOICES[locale], rate="-25%" if pace == "slow" else "+0%"
                )
                await asyncio.wait_for(task.save(str(temporary)), timeout=45)
                if temporary.stat().st_size < 1000:
                    raise ValueError(f"audio too small: {locale} {text!r}")
                os.replace(temporary, path)
                return
            except Exception as error:
                temporary.unlink(missing_ok=True)
                if attempt == 3:
                    if locale.startswith("en-") and pace == "natural":
                        slower = CLIPS / clip_name(locale, "slow", text)
                        if slower.is_file() and slower.stat().st_size > 1000:
                            shutil.copyfile(slower, path)
                            return
                    raise RuntimeError(f"Voice failed for {locale} {pace} {text!r}") from error
                await asyncio.sleep(2 * (attempt + 1))


def silence(seconds):
    path = AUDIO / f"silence-{seconds}.mp3"
    if not path.exists():
        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-f", "lavfi", "-i", "anullsrc=r=24000:cl=mono",
            "-t", str(seconds), "-ar", "24000", "-ac", "1", "-b:a", "48k",
            str(path),
        ], check=True)
    return path


def assemble(mission, chapters, accent, output, index):
    files = []
    delivery = CATALOG["delivery"]

    def add_segments(segments):
        for segment in segments:
            locale = accent if segment["locale"] == "en" else segment["locale"]
            files.append(ROOT / "android/app/src/main/assets" / index[key(locale, "natural", segment["text"])])

    add_segments(mission["intro"])
    for chapter in chapters:
        add_segments(chapter["intro"])
        stages = sorted({entry.get("stage", 1) for entry in chapter["theory"] + chapter["exercises"]})
        for stage_index, stage in enumerate(stages):
            if stage_index > 0:
                add_segments(CATALOG["shared"]["nextStage"])
            for card in chapter["theory"]:
                if card.get("stage", 1) != stage:
                    continue
                add_segments(card["audio"])
                files.append(silence(delivery["theoryPauseMs"] // 1000))
    for chapter in chapters:
        stages = sorted({entry.get("stage", 1) for entry in chapter["exercises"]})
        for stage in stages:
            for exercise in chapter["exercises"]:
                if exercise.get("stage", 1) != stage:
                    continue
                # Android Auto is audio-only: the ordering tiles and the
                # open-ended microphone exercise require the phone screen.
                if exercise.get("type", "repeat") != "repeat":
                    continue
                if exercise.get("context"):
                    add_segments(exercise.get("contextAudio", [{"locale": "fr-FR",
                                                                 "text": exercise["context"]}]))
                add_segments(exercise.get("questionAudio", [{"locale": exercise["questionLocale"],
                                                              "text": exercise["question"]}]))
                add_segments(CATALOG["shared"]["carThink"])
                files.append(silence(delivery["answerPauseMs"] // 1000))
                add_segments(CATALOG["shared"]["autoAnswer"])
                files.append(ROOT / "android/app/src/main/assets" /
                             index[key(accent, "natural", exercise["model"])])
                add_segments(CATALOG["shared"]["pause"])
                files.append(silence(delivery["repeatPauseMs"] // 1000))
        for passage in mission.get("readingPassages", []):
            if passage["topicId"] != chapter["topicId"]:
                continue
            for sentence in passage["sentences"]:
                add_segments([{"locale": "en", "text": sentence}])
            add_segments([{"locale": "fr-FR", "text": passage["question"]}])
            files.append(silence(delivery["answerPauseMs"] // 1000))
            add_segments([{"locale": "fr-FR", "text":
                           "La bonne réponse est : " + passage["choices"][passage["correctIndex"]] + "."}])
    add_segments(mission["outro"])
    listing = output.with_suffix(".concat.txt")
    listing.write_text("\n".join(f"file '{p}'" for p in files) + "\n")
    temporary = output.with_suffix(".part.mp3")
    try:
        subprocess.run([
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-f", "concat", "-safe", "0", "-i", str(listing),
            # Variable bitrate preserves speech while long reflection pauses
            # use fewer bytes. Phone clips retain their original quality.
            "-c:a", "libmp3lame", "-q:a", "6", "-ar", "24000", "-ac", "1",
            str(temporary),
        ], check=True)
        subprocess.run([
            "ffprobe", "-v", "error", "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1", str(temporary)
        ], stdout=subprocess.DEVNULL, check=True)
        os.replace(temporary, output)
    finally:
        listing.unlink(missing_ok=True)
        temporary.unlink(missing_ok=True)


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jobs", type=int, default=6)
    parser.add_argument("--rebuild-tracks", action="store_true")
    parser.add_argument("--resume-tracks", action="store_true")
    args = parser.parse_args()
    CLIPS.mkdir(parents=True, exist_ok=True)
    TRACKS.mkdir(parents=True, exist_ok=True)
    lines = collect_lines()
    # Include unsplit French contexts for older catalogue entries.
    for mission in CATALOG["universes"]:
        for chapter in mission["lessons"]:
            for exercise in chapter["exercises"]:
                if exercise.get("context") and not exercise.get("contextAudio"):
                    lines.append(("fr-FR", "natural", exercise["context"]))
    lines = sorted(set(lines))
    semaphore = asyncio.Semaphore(max(1, args.jobs))
    print(f"Preparing {len(lines)} unique voice clips…", flush=True)
    tasks = [generate_one(semaphore, *line) for line in lines]
    for i, task in enumerate(asyncio.as_completed(tasks), 1):
        await task
        if i % 20 == 0 or i == len(lines):
            print(f"  {i}/{len(lines)} clips ready", flush=True)
    index = {key(*line): "audio/clips/" + clip_name(*line) for line in lines}
    (AUDIO / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n")
    track_index = {}
    pending = []
    for mission in CATALOG["universes"]:
        for accent in ("en-GB", "en-US"):
            for selection, chapters in [
                (chapter["topicId"], [chapter]) for chapter in mission["lessons"]
            ]:
                track_id = f"{mission['universeId']}:{selection}:{accent}"
                output = TRACKS / (track_id.replace(":", "_") + ".mp3")
                stale = args.rebuild_tracks or not output.exists() or output.stat().st_size < 1000
                if args.resume_tracks and not stale:
                    stale = output.stat().st_mtime_ns <= CATALOG_PATH.stat().st_mtime_ns
                if stale:
                    pending.append((mission, chapters, accent, output))
                track_index[track_id] = "audio/tracks/" + output.name
    silence(CATALOG["delivery"]["theoryPauseMs"] // 1000)
    silence(CATALOG["delivery"]["answerPauseMs"] // 1000)
    silence(CATALOG["delivery"]["repeatPauseMs"] // 1000)
    with ThreadPoolExecutor(max_workers=min(max(1,args.jobs),8)) as pool:
        jobs=[pool.submit(assemble,*spec,index) for spec in pending]
        for number,job in enumerate(as_completed(jobs),1):
            job.result()
            if number%10==0 or number==len(jobs):
                print(f"  {number}/{len(jobs)} tracks rebuilt",flush=True)
    (TRACKS / "index.json").write_text(json.dumps(track_index, indent=2) + "\n")
    print(f"Ready: {len(lines)} clips, {len(track_index)} Android Auto tracks.", flush=True)


if __name__ == "__main__":
    asyncio.run(main())
