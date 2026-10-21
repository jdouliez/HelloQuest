"""Check catalogue coverage and all pre-recorded audio references."""
import json
import sys
from pathlib import Path
from add_subjects import SUBJECTS

root = Path(__file__).resolve().parents[1]
catalog_path = Path(sys.argv[1]) if len(sys.argv) > 1 else root / "content/dialogues-poc.json"
catalog = json.loads(catalog_path.read_text())
audio_root = root / "android/app/src/main/assets"
track_root = root / "android/audio_pack/src/main/assets"
clips = json.loads((audio_root / "audio/index.json").read_text())
tracks = json.loads((track_root / "audio/tracks/index.json").read_text())
photos = json.loads((root / "content/lesson-photos.json").read_text())


def has_voice(locale, text, slow=False):
    assert any(char.isalpha() for char in text), f"Unspoken punctuation: {locale} {text!r}"
    locales = ("en-GB", "en-US") if locale == "en" else (locale,)
    for variant in locales:
        for pace in (("natural", "slow") if slow and variant.startswith("en-") else ("natural",)):
            key = f"{variant}|{pace}|{text}"
            assert key in clips, f"Missing clip: {key}"
            path = audio_root / clips[key]
            assert path.is_file() and path.stat().st_size > 1000, f"Missing or empty file: {key}"


ids = set()
reading_ids = set()
dialogue_ids = set()
topic_ids = [topic["id"] for topic in catalog["topics"]]
assert len(topic_ids) == len(set(topic_ids)) >= 4
assert set(topic_ids) >= ({"pronouns", "verbs", "words", "listening"} |
                          {item[0] for item in SUBJECTS})
assert all(topic["label"].strip() for topic in catalog["topics"])
assert set(photos) == set(topic_ids)
universe_ids = [universe["universeId"] for universe in catalog["universes"]]
assert len(universe_ids) == len(set(universe_ids)) >= 6
lesson_ids = set()
for mission in catalog["universes"]:
    assert isinstance(mission["unlockQuestions"], int) and mission["unlockQuestions"] >= 0
    assert isinstance(mission["unlockLessons"], int) and mission["unlockLessons"] >= 0
    chapters = {chapter["topicId"]: chapter for chapter in mission["lessons"]}
    assert len(chapters) == len(mission["lessons"])
    assert set(chapters) == set(topic_ids)
    concepts = set()
    for chapter in chapters.values():
        lesson_id = (mission["universeId"], chapter["topicId"])
        assert lesson_id not in lesson_ids, lesson_id
        lesson_ids.add(lesson_id)
        assert len({card["translation"] for card in chapter["theory"]}) >= 5, lesson_id
        assert len(chapter["exercises"]) >= 5, lesson_id
        formats = [exercise.get("type", "repeat") for exercise in chapter["exercises"]]
        assert set(formats) == {"repeat", "order", "reply"}, lesson_id
        assert formats.count("order") >= 2 and formats.count("reply") >= 2, lesson_id
        qcm_eligible = 0
        stages = {exercise.get("stage", 1) for exercise in chapter["exercises"]}
        assert stages <= {1, 2, 3} and stages, lesson_id
        for stage in stages:
            exercises = [x for x in chapter["exercises"] if x.get("stage", 1) == stage]
            cards = [x for x in chapter["theory"] if x.get("stage", 1) == stage]
            assert len(exercises) >= 3, (mission["universeId"], chapter["topicId"], stage)
            assert cards, (mission["universeId"], chapter["topicId"], stage)
        for card in chapter["theory"]:
            visual = photos[chapter["topicId"]][card["word"]]
            assert visual.startswith("photo:"), (lesson_id,card["word"],visual)
            image = visual.split(":",1)[1]
            assert (audio_root / "photos" / (image + ".jpg")).is_file(), visual
            for segment in card["audio"]:
                has_voice(segment["locale"], segment["text"])
            spoken = [x["text"] for x in card["audio"] if x["locale"] == "en"]
            assert spoken and spoken[0].lower().rstrip(".") == card["word"].lower().rstrip(".")
        for exercise in chapter["exercises"]:
            assert exercise["id"] not in ids, exercise["id"]
            ids.add(exercise["id"])
            kind = exercise.get("type", "repeat")
            assert kind in {"repeat", "order", "reply"}, exercise["id"]
            if kind == "order":
                assert 3 <= len(exercise["model"].split()) <= 8, exercise["id"]
                assert exercise["questionAudio"][-1] == {
                    "locale": "en", "text": exercise["model"]}, exercise["id"]
            if kind == "reply":
                assert exercise["questionLocale"] == "fr-FR", exercise["id"]
                assert not any(segment["locale"] == "en"
                               for segment in exercise.get("questionAudio", [])), exercise["id"]
            assert exercise.get("translation", "").strip(), exercise["id"]
            model = exercise["model"].lower().strip(" .!?")
            matching_example = [card for card in chapter["theory"]
                                if card["example"].lower().strip(" .!?") == model]
            matching_word = [card for card in chapter["theory"]
                             if card["word"].lower().strip(" .!?") == model]
            if kind == "reply" and matching_word:
                assert any(exercise["translation"] == card["meaning"]
                           for card in matching_word), exercise["id"]
            elif matching_example:
                assert exercise["translation"] == matching_example[0]["translation"], exercise["id"]
            elif matching_word:
                assert exercise["translation"] == matching_word[0]["meaning"], exercise["id"]
            concepts.add(exercise["answers"][0].lower().strip(" .!?"))
            if any(card.get("stage",1)==exercise.get("stage",1) and
                   exercise["model"].lower().strip(" .!?") in
                   (card["word"].lower().strip(" .!?"),card["example"].lower().strip(" .!?"))
                   for card in chapter["theory"]):
                qcm_eligible += 1
            for segment in exercise.get("questionAudio", [{"locale":exercise["questionLocale"],
                                                             "text":exercise["question"]}]):
                has_voice(segment["locale"],segment["text"])
            has_voice("en", exercise["model"], slow=True)
            if exercise.get("context"):
                for segment in exercise.get("contextAudio", [{"locale":"fr-FR",
                                                                "text":exercise["context"]}]):
                    has_voice(segment["locale"],segment["text"])
            for segment in exercise["hint"]:
                has_voice(segment["locale"], segment["text"], slow=True)
            if "-extra-" in exercise["id"]:
                matching = [card for card in chapter["theory"]
                            if card.get("stage", 1) == exercise.get("stage", 1)
                            and (card["example"] in (exercise["model"], exercise["question"])
                                 or card["word"].lower() in exercise["model"].lower())]
                assert matching, f"No lesson for {exercise['id']}"
        assert qcm_eligible >= 5, (mission["universeId"],chapter["topicId"],qcm_eligible)
        assert len({card["word"].lower() for card in chapter["theory"]}) >= 5, lesson_id
    assert len(concepts) >= 16, (mission["universeId"],len(concepts))
    for dialogue in mission["miniDialogues"]:
        assert dialogue["id"] not in dialogue_ids, dialogue["id"]
        dialogue_ids.add(dialogue["id"])
        assert dialogue["topicId"] in chapters
        assert len(dialogue["lines"]) in (2, 3)
        assert len(dialogue["choices"]) == len(set(dialogue["choices"])) == 3
        assert 0 <= dialogue["correctIndex"] < 3
        for line in dialogue["lines"]:
            has_voice("en", line)
        has_voice("fr-FR", dialogue["question"])
    for passage in mission.get("readingPassages", []):
        assert passage["id"] not in reading_ids, passage["id"]
        reading_ids.add(passage["id"])
        assert passage["topicId"] in chapters
        assert 2 <= len(passage["sentences"]) <= 6
        assert len(passage["choices"]) == len(set(passage["choices"])) == 3
        assert 0 <= passage["correctIndex"] < 3
        for sentence in passage["sentences"]:
            has_voice("en", sentence)
        has_voice("fr-FR", passage["question"])
        has_voice("fr-FR", "La bonne réponse est : " +
                  passage["choices"][passage["correctIndex"]] + ".")

assert len(tracks) == len(lesson_ids) * 2
assert len(lesson_ids) == len(universe_ids) * len(topic_ids)
for universe_id, topic_id in lesson_ids:
    for accent in ("en-GB", "en-US"):
        assert f"{universe_id}:{topic_id}:{accent}" in tracks
for path in tracks.values():
    assert (track_root / path).is_file(), path
assert len(reading_ids) >= 18
for line in ("Good job!", "No, it's incorrect."):
    has_voice("en", line)
print(f"Valid: {len(ids)} unique exercises, {len(lesson_ids)} universe/topic lessons, {len(dialogue_ids)} dialogues, {len(reading_ids)} readings, {len(clips)} clips, {len(tracks)} tracks")
