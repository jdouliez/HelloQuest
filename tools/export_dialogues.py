"""Export the canonical prepared scripts to readable Markdown and the HTML demo.

No API calls or speech synthesis. Run from any directory with Python 3.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
catalog = json.loads((ROOT / "content/dialogues-poc.json").read_text())
lines = [
    "# Dialogues préparés — PoC Hello Quest",
    "",
    "Source : [dialogues-poc.json](dialogues-poc.json). Exporter avec `python3 tools/export_dialogues.py` après une modification du JSON.",
    "",
    "Les textes et les pistes TTS sont préparés avant le build. FR désigne la voix de guidage française, EN la voix anglaise choisie (britannique ou américaine). Les scènes sont originales pour ce PoC et se déroulent dans les univers nommés.",
    "",
    "## Ordre de lecture",
    "",
    "Sur téléphone, les leçons présentent le principe du sujet et ses cartes de vocabulaire. L’enfant passe à la carte suivante lorsqu’il est prêt ; terminer la leçon rend les exercices et le quiz de ce couple sujet–univers accessibles. Les exercices mélangent plusieurs formats et gardent la traduction française visible lors des réponses orales. Le quiz pose directement cinq à dix QCM, sans cours préalable dans cette séance.",
    "Le mode avancé masque les phrases anglaises à retrouver dans les exercices oraux ; la traduction et les consignes françaises restent visibles. Les paragraphes de lecture guidée restent affichés pour que l’enfant puisse les suivre.",
    "",
    "Sur téléphone, le moteur sélectionne les retours pré-écrits selon la reconnaissance. Un court dialogue de deux ou trois répliques, suivi d’un choix multiple en français, peut conclure une mission plus avancée. Sur Android Auto média, les cours sont lus en premier, puis quinze secondes sont laissées pour répondre sans évaluation. Une pause de huit secondes suit le modèle. Les durées dépendent des chapitres et sont mesurées après assemblage.",
    "",
    "## Retours communs pré-enregistrables",
    "",
]


def dialogue(segments):
    return [f"- **{'FR' if s['locale'] == 'fr-FR' else 'EN'}** : {s['text']}" for s in segments]


for event, segments in catalog["shared"].items():
    lines.extend([f"### {event}", "", *dialogue(segments), ""])
lines.extend(["Après `correct`, `needsHelp` ou `autoAnswer`, lire le modèle EN de l’exercice. Un son ambigu ne valide ni n’invalide une compétence.", ""])
for mission in catalog["universes"]:
    lines.extend([f"## {mission['universeLabel']} — {mission['title']}", "", *dialogue(mission["intro"]), ""])
    for chapter in mission["lessons"]:
        lines.extend([f"### {chapter['title']}", "", *dialogue(chapter["intro"]), ""])
        for stage in (1, 2, 3):
            lines.extend([f"**Étape {stage} — contenu de cours et banque d’exercices**", ""])
            for card in chapter["theory"]:
                if card.get("stage", 1) != stage:
                    continue
                lines.extend([
                    f"- **{card['word']}** = {card['meaning']} — {card['explanation']}",
                    f"  Exemple EN : {card['example']} — {card['translation']}",
                ])
            lines.append("")
            for exercise in chapter["exercises"]:
                if exercise.get("stage", 1) != stage:
                    continue
                lines.extend([f"**{exercise['id']}**", ""])
                if exercise.get("context"):
                    lines.append(f"- **Consigne FR** : {exercise['context']}")
                lang = "FR" if exercise["questionLocale"] == "fr-FR" else "EN"
                lines.extend([
                    f"- **Question {lang}** : {exercise['question']}",
                    "- **Enfant** : [temps de réponse Android Auto : 15 secondes]",
                    f"- **Modèle EN** : {exercise['model']}",
                    "- **Indice préparé** :",
                    *[f"  {line}" for line in dialogue(exercise["hint"])],
                    "- **Réponses acceptées** : " + " ; ".join(exercise["answers"]),
                    "",
                ])
    for challenge in mission.get("miniDialogues", []):
        lines.extend([f"### Mini-dialogue {challenge['stage']} — {challenge['id']}", ""])
        lines.extend(f"- **EN** : {spoken}" for spoken in challenge["lines"])
        lines.extend([f"- **Question FR** : {challenge['question']}",
                      "- **Choix** : " + " / ".join(challenge["choices"]),
                      f"- **Réponse** : {challenge['choices'][challenge['correctIndex']]}", ""])
    for passage in mission.get("readingPassages", []):
        lines.extend([f"### Lecture guidée — {passage['title']}", ""])
        lines.extend(f"- **EN** : {sentence}" for sentence in passage["sentences"])
        lines.extend([f"- **Question FR** : {passage['question']}",
                      "- **Choix** : " + " / ".join(passage["choices"]),
                      f"- **Réponse** : {passage['choices'][passage['correctIndex']]}", ""])
    lines.extend(["### Conclusion et transfert", "", *dialogue(mission["outro"]), ""])
(ROOT / "content/dialogues-poc.md").write_text("\n".join(lines))
(ROOT / "maquettes/dialogues-poc.js").write_text(
    "// Generated from content/dialogues-poc.json by tools/export_dialogues.py.\n"
    "window.HELLO_QUEST_DIALOGUES = " + json.dumps(catalog, ensure_ascii=False, indent=2) + ";\n"
)
print(f"Exported {len(catalog['universes'])} universes and "
      f"{sum(len(universe['lessons']) for universe in catalog['universes'])} lessons "
      "to Markdown and browser data.")
