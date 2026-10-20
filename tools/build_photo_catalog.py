"""Associate every lesson concept with a bundled, credited real photograph."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "content/dialogues-poc.json"
WORD_PHOTOS = ROOT / "content/photo-map.json"
CREDITS = ROOT / "content/photo-credits.json"
OUTPUT = ROOT / "content/lesson-photos.json"
ASSETS = ROOT / "android/app/src/main/assets/photos"

TOPIC_PHOTOS = {
    "pronouns": "mother", "verbs": "run", "words": "book",
    "listening": "read", "colors": "apple", "numbers_0_20": "apple",
    "numbers_21_100": "apple", "alphabet": "book", "greetings": "smile",
    "introductions": "smile", "family": "mother", "body": "foot",
    "clothes": "shirt", "food": "bread", "school": "pencil",
    "home": "table", "animals_vocab": "dog", "city": "city",
    "countries": "passport", "weather": "sunny", "calendar": "summer",
    "time": "station", "directions": "bridge", "positions": "table",
    "shapes": "window", "emotions": "happy", "hobbies": "swim",
    "questions": "think", "sentence_structure": "book", "articles": "book",
    "plurals": "cat", "negation": "close", "possessives": "mother",
    "to_be": "mother", "to_have": "suitcase", "can": "climb",
    "present_simple": "run", "present_continuous": "run",
    "past_simple": "run", "future_going_to": "train",
    "third_person_s": "mother",
}

WORD_OVERRIDES = {
    "red": "apple", "green": "park", "blue": "beach",
    "yellow": "banana", "black": "night", "white": "winter",
    "brown": "bread", "orange": "summer", "pink": "shirt",
    "purple": "scarf", "i": "pilot", "you": "father",
    "he": "father", "she": "mother", "it": "dog",
    "we": "mother", "they": "mother",
    "my": "pilot", "your": "father", "his": "father", "her": "mother",
    "our": "mother", "their": "mother",
}


def photo_for(word, topic, word_photos):
    normalized = word.lower().strip()
    if normalized in WORD_OVERRIDES:
        return WORD_OVERRIDES[normalized]
    if word in word_photos:
        return word_photos[word]
    if normalized in word_photos:
        return word_photos[normalized]
    for term in sorted(word_photos, key=len, reverse=True):
        if len(term) >= 4 and re.search(r"(?<![a-z])" + re.escape(term) + r"(?![a-z])", normalized):
            return word_photos[term]
    return TOPIC_PHOTOS[topic]


def main():
    catalog = json.loads(CONTENT.read_text())
    word_photos = json.loads(WORD_PHOTOS.read_text())
    credits = json.loads(CREDITS.read_text())
    expected = {topic["id"] for topic in catalog["topics"]}
    if set(TOPIC_PHOTOS) != expected:
        raise ValueError("Missing or extra topic photograph")
    result = {topic: {} for topic in TOPIC_PHOTOS}
    for universe in catalog["universes"]:
        for chapter in universe["lessons"]:
            topic = chapter["topicId"]
            for card in chapter["theory"]:
                word = card["word"]
                photo = photo_for(word, topic, word_photos)
                if not (ASSETS / f"{photo}.jpg").is_file():
                    raise ValueError(f"Missing photograph for {topic}/{word}: {photo}")
                if photo not in credits:
                    raise ValueError(f"Missing photograph credit for {topic}/{word}: {photo}")
                result[topic][word] = f"photo:{photo}"
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(f"Mapped {sum(map(len, result.values()))} concepts in {len(result)} topics to real photographs")


if __name__ == "__main__":
    main()
