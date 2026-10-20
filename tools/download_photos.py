"""Download hand-selected CC0 photographs from Wikimedia Commons.

The generated manifest keeps the exact source and author for every bundled file.
Candidates are reviewed by concept and license; unsuitable search hits are omitted.
"""
import html
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

from curate_photos import ROOT, terms

USER_AGENT = "HelloQuest educational Android prototype (photo research)"
PHOTO_DIR = ROOT / "android/app/src/main/assets/photos"
CANDIDATES = json.loads((ROOT / "content/photo-candidates.json").read_text())

# A numbered result is used only when its title describes the intended concept.
SELECT = {
    "mother": 0, "father": 0, "pilot": 0,
    "run": 0, "walk": 0, "jump": 0, "read": 1, "swim": 0,
    "draw": 1, "write": 0, "sleep": 0, "smile": 1, "climb": 0,
    "sail": 0, "pack": 0, "hide": 2, "close": 0,
    "repair": 2, "explore": 0, "wait": 2, "think": 2,
    "look": 0, "cat": 0, "dog": 0, "rabbit": 1,
    "bird": 2, "fish": 0, "bear": 0, "duck": 1,
    "fox": 2, "turtle": 0, "owl": 0, "butterfly": 0,
    "banana": 1, "bread": 0, "beach": 2, "castle": 0,
    "bridge": 2, "city": 0, "moon": 0, "star": 0,
    "robot": 0, "helmet": 1, "book": 1, "passport": 0,
    "suitcase": 0, "train": 1, "station": 0, "candle": 1,
    "scarf": 1, "hat": 0, "shoes": 2, "coat": 1,
    "shirt": 2, "foot": 0, "desk": 0, "notebook": 1,
    "pen": 2, "pencil": 1, "door": 0, "window": 2,
    "table": 0, "chair": 0, "park": 2, "library": 0,
    "museum": 0, "rainy": 0, "sunny": 0, "winter": 2,
    "summer": 0, "cold": 0, "happy": 1, "fast": 0,
    "small": 2, "big": 0, "night": 0, "green_light": 0,
    "wings": 2,
}
EXTRA = {
    "apple": "File:Apple-2924531 1920.jpg",
    "sister": "File:Sisters playing on a swing.jpg",
    "brother": "File:Photo Of Two Brothers.jpg",
}
# Visual review: these results contained an unrelated subject, a barely visible
# subject, or (for the suitcase packing result) an alcohol advertisement.
REJECTED = {
    "walk", "look", "robot", "candle", "coat", "notebook",
    "museum", "small", "big", "green_light", "wings", "pack", "star",
}


def title_for(key):
    if key in EXTRA:
        return EXTRA[key]
    raw = CANDIDATES[key][SELECT[key]]
    match = re.search(r"File:.*?\.(?:jpg|jpeg|png)", raw, re.IGNORECASE)
    if not match:
        raise ValueError(f"Unexpected search title: {raw}")
    return match.group()


def details_batch(titles):
    query = urllib.parse.urlencode({
        "action": "query", "format": "json", "prop": "imageinfo",
        "iiprop": "url|extmetadata", "iiurlwidth": "720", "titles": "|".join(titles),
    })
    url = "https://commons.wikimedia.org/w/api.php?" + query
    for attempt in range(6):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=45) as response:
                return json.load(response)["query"]["pages"]
        except urllib.error.HTTPError as error:
            if error.code != 429 or attempt == 5:
                raise
            delay = max(int(error.headers.get("Retry-After", "45")), 45)
            print(f"Commons rate limit; retrying in {delay}s", flush=True)
            time.sleep(delay)


def details(page):
    info = page.get("imageinfo", [{}])[0]
    meta = info.get("extmetadata", {})
    license_name = html.unescape(re.sub("<[^>]+>", "", meta.get("LicenseShortName", {}).get("value", ""))).strip()
    if license_name not in ("CC0", "Public domain"):
        raise ValueError(f"{page['title']}: license is {license_name!r}")
    author = html.unescape(re.sub("<[^>]+>", "", meta.get("Artist", {}).get("value", ""))).strip()
    return {
        "title": page["title"], "author": author,
        "license": license_name,
        "source": info.get("descriptionurl", "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(page["title"].replace(" ", "_"))),
        "download": info.get("thumburl", info["url"]),
    }


def main():
    PHOTO_DIR.mkdir(parents=True, exist_ok=True)
    credits = {}
    word_map = {}
    keys = [key for key in list(SELECT) + list(EXTRA) if key not in REJECTED]
    pages = {}
    for start in range(0, len(keys), 40):
        pages.update(details_batch([title_for(key) for key in keys[start:start+40]]))
        time.sleep(1)
    by_title = {page["title"]: page for page in pages.values()}
    for index, key in enumerate(keys):
        try:
            title=title_for(key)
            page=by_title.get(title)
            if page is None:
                raise ValueError(f"No image metadata for {title}")
            credit = details(page)
            path = PHOTO_DIR / f"{key}.jpg"
            if not path.exists():
                request = urllib.request.Request(credit["download"], headers={
                    "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) Firefox/125.0",
                    "Referer": "https://commons.wikimedia.org/",
                })
                with urllib.request.urlopen(request, timeout=45) as response:
                    content = response.read()
                if not content.startswith(b"\xff\xd8"):
                    raise ValueError(f"{key}: response is not a JPEG")
                path.write_bytes(content)
            credit.pop("download")
            credits[key] = credit
            for word in terms()[key][1]:
                word_map[word] = key
            print(f"{index+1}: {key}: {path.stat().st_size} bytes", flush=True)
        except Exception as error:
            print(f"SKIP {key}: {error}", flush=True)
        time.sleep(0.35)
    (ROOT / "content/photo-map.json").write_text(json.dumps(word_map, ensure_ascii=False, indent=2) + "\n")
    (ROOT / "content/photo-credits.json").write_text(json.dumps(credits, ensure_ascii=False, indent=2) + "\n")
    print(f"Downloaded {len(credits)} photographs, mapped {len(word_map)} words", flush=True)


if __name__ == "__main__":
    main()
