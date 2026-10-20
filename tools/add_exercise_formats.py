"""Add the two interactive exercise formats to every subject/universe lesson.

The lesson cards remain the source of truth for the English model and its
French translation. Re-running this script does not duplicate exercises.
"""
import json
from pathlib import Path

CATALOG = Path(__file__).resolve().parents[1] / "content/dialogues-poc.json"
catalog = json.loads(CATALOG.read_text())

for universe in catalog["universes"]:
    for lesson in universe["lessons"]:
        existing = {exercise["id"] for exercise in lesson["exercises"]}
        ordered = [card for card in lesson["theory"]
                   if 3 <= len(card["example"].split()) <= 8]
        replies = [card for card in lesson["theory"] if card["word"].strip()]
        assert len(ordered) >= 2 and len(replies) >= 4, (
            universe["universeId"], lesson["topicId"])
        # Spread the prompts across the lesson's notions rather than using
        # the same first word for both formats.
        for kind, cards in (("order", [ordered[0], ordered[len(ordered)//2]]),
                            ("reply", [replies[1], replies[len(replies)//2+1]])):
            for number, card in enumerate(cards, 1):
                exercise_id = (f"{universe['universeId']}-{lesson['topicId']}-"
                               f"{kind}-{number}")
                if exercise_id in existing:
                    continue
                if kind == "order":
                    entry = {
                        "id": exercise_id, "type": "order",
                        "question": "Écoute la phrase, puis remets les mots dans l’ordre.",
                        "questionLocale": "fr-FR",
                        "model": card["example"], "translation": card["translation"],
                        "answers": [card["example"]], "hint": [],
                        "stage": card.get("stage", 1),
                        "questionAudio": [
                            {"locale": "fr-FR", "text": "Écoute la phrase, puis remets les mots dans l’ordre."},
                            {"locale": "en", "text": card["example"]},
                        ],
                    }
                else:
                    prompt = f"Un personnage te demande : comment dit-on « {card['meaning']} » en anglais ?"
                    entry = {
                        "id": exercise_id, "type": "reply",
                        "question": prompt, "questionLocale": "fr-FR",
                        "model": card["word"], "translation": card["meaning"],
                        "answers": [card["word"]], "hint": [],
                        "stage": card.get("stage", 1),
                    }
                lesson["exercises"].append(entry)

CATALOG.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n")
