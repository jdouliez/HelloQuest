"""Enrich the canonical catalogue with short, original, themed lessons and readings.

Idempotent: existing IDs are kept. Run before build_audio.py.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "content/dialogues-poc.json"
catalog = json.loads(PATH.read_text())
catalog["shared"]["qcmCorrect"] = [{"locale": "en", "text": "Good job!"}]
catalog["shared"]["qcmIncorrect"] = [{"locale": "en", "text": "No, it's incorrect."}]

# word, French meaning, English example, French translation
NEW = {
    "star-wars": {
        "verbs": [
            ("pilot", "piloter", "I can pilot the ship.", "Je peux piloter le vaisseau."),
            ("look", "regarder", "I can look at the stars.", "Je peux regarder les étoiles."),
            ("open", "ouvrir", "I can open the door.", "Je peux ouvrir la porte."),
            ("repair", "réparer", "I can repair the droid.", "Je peux réparer le droïde."),
        ],
        "words": [
            ("a droid", "un droïde", "The droid is small.", "Le droïde est petit."),
            ("a moon", "une lune", "The moon is bright.", "La lune est lumineuse."),
            ("a helmet", "un casque", "The helmet is white.", "Le casque est blanc."),
            ("a signal", "un signal", "I see a signal.", "Je vois un signal."),
        ],
        "listening": [
            ("moon", "lune", "The moon is bright.", "La lune est lumineuse."),
            ("bright", "lumineux", "The moon is bright.", "La lune est lumineuse."),
            ("ready", "prêt", "Leia is ready.", "Leia est prête."),
            ("night", "nuit", "It is night on the planet.", "C'est la nuit sur la planète."),
        ],
    },
    "harry-potter": {
        "verbs": [
            ("draw", "dessiner", "I can draw a map.", "Je peux dessiner une carte."),
            ("close", "fermer", "She can close the book.", "Elle peut fermer le livre."),
            ("carry", "porter", "I can carry the bag.", "Je peux porter le sac."),
            ("smile", "sourire", "We can smile at the owl.", "Nous pouvons sourire à la chouette."),
        ],
        "words": [
            ("a letter", "une lettre", "The letter is on the table.", "La lettre est sur la table."),
            ("a classroom", "une salle de classe", "The classroom is big.", "La salle de classe est grande."),
            ("a scarf", "une écharpe", "The scarf is red.", "L'écharpe est rouge."),
            ("a candle", "une bougie", "I see a candle.", "Je vois une bougie."),
        ],
        "listening": [
            ("quiet", "calme", "The library is quiet.", "La bibliothèque est calme."),
            ("window", "fenêtre", "The owl is near the window.", "La chouette est près de la fenêtre."),
            ("early", "tôt", "Harry is early today.", "Harry est en avance aujourd'hui."),
            ("warm", "chaud", "The room is warm.", "La pièce est chaude."),
        ],
    },
    "chess": {
        "verbs": [
            ("plan", "planifier", "I can plan my next move.", "Je peux prévoir mon prochain coup."),
            ("wait", "attendre", "I can wait for my turn.", "Je peux attendre mon tour."),
            ("count", "compter", "I can count the pieces.", "Je peux compter les pièces."),
            ("choose", "choisir", "I can choose a move.", "Je peux choisir un coup."),
        ],
        "words": [
            ("a square", "une case", "This square is white.", "Cette case est blanche."),
            ("a knight", "un cavalier", "The knight is near the king.", "Le cavalier est près du roi."),
            ("a move", "un coup", "That is a good move.", "C'est un bon coup."),
            ("a turn", "un tour", "It is my turn.", "C'est mon tour."),
        ],
        "listening": [
            ("left", "gauche", "The king moves left.", "Le roi va à gauche."),
            ("right", "droite", "The queen moves right.", "La reine va à droite."),
            ("first", "premier", "The pawn moves first.", "Le pion joue en premier."),
            ("safe", "en sécurité", "The king is safe.", "Le roi est en sécurité."),
        ],
    },
    "video-games": {
        "verbs": [
            ("hide", "se cacher", "I can hide behind the hill.", "Je peux me cacher derrière la colline."),
            ("unlock", "déverrouiller", "I can unlock the door.", "Je peux déverrouiller la porte."),
            ("collect", "ramasser", "I can collect a coin.", "Je peux ramasser une pièce."),
            ("explore", "explorer", "We can explore the cave.", "Nous pouvons explorer la grotte."),
        ],
        "words": [
            ("a bridge", "un pont", "The bridge is long.", "Le pont est long."),
            ("a button", "un bouton", "I see a button.", "Je vois un bouton."),
            ("a cave", "une grotte", "The cave is dark.", "La grotte est sombre."),
            ("a portal", "un portail", "The portal is blue.", "Le portail est bleu."),
        ],
        "listening": [
            ("behind", "derrière", "The key is behind the door.", "La clé est derrière la porte."),
            ("above", "au-dessus", "The star is above the bridge.", "L'étoile est au-dessus du pont."),
            ("shiny", "brillant", "The coin is shiny.", "La pièce est brillante."),
            ("secret", "secret", "Nova has a secret map.", "Nova a une carte secrète."),
        ],
    },
    "travel": {
        "verbs": [
            ("pack", "préparer son sac", "I can pack my bag.", "Je peux préparer mon sac."),
            ("explore", "explorer", "We can explore a city.", "Nous pouvons explorer une ville."),
            ("return", "revenir", "I can return to the station.", "Je peux revenir à la gare."),
            ("ask", "demander", "I can ask for a ticket.", "Je peux demander un billet."),
        ],
        "words": [
            ("a passport", "un passeport", "My passport is in my bag.", "Mon passeport est dans mon sac."),
            ("a station", "une gare", "The station is near.", "La gare est proche."),
            ("a suitcase", "une valise", "The suitcase is heavy.", "La valise est lourde."),
            ("a beach", "une plage", "The beach is sunny.", "La plage est ensoleillée."),
        ],
        "listening": [
            ("north", "nord", "The train goes north.", "Le train va vers le nord."),
            ("ticket", "billet", "Mia has a ticket.", "Mia a un billet."),
            ("rainy", "pluvieux", "It is rainy in the city.", "Il pleut dans la ville."),
            ("near", "près", "The hotel is near the park.", "L'hôtel est près du parc."),
        ],
    },
    "animals": {
        "verbs": [
            ("follow", "suivre", "The duck can follow Luna.", "Le canard peut suivre Luna."),
            ("hop", "sautiller", "The rabbit can hop.", "Le lapin peut sautiller."),
            ("climb", "grimper", "The cat can climb here.", "Le chat peut grimper ici."),
            ("listen", "écouter", "The dog can listen.", "Le chien peut écouter."),
        ],
        "words": [
            ("a rabbit", "un lapin", "The rabbit is small.", "Le lapin est petit."),
            ("a turtle", "une tortue", "The turtle is green.", "La tortue est verte."),
            ("a duck", "un canard", "The duck is in the pond.", "Le canard est dans la mare."),
            ("a butterfly", "un papillon", "The butterfly is yellow.", "Le papillon est jaune."),
        ],
        "listening": [
            ("slow", "lent", "The turtle is slow.", "La tortue est lente."),
            ("soft", "doux", "The rabbit is soft.", "Le lapin est doux."),
            ("hungry", "affamé", "The fox is hungry.", "Le renard a faim."),
            ("wings", "ailes", "The butterfly has wings.", "Le papillon a des ailes."),
        ],
    },
}

# Title, skill, 2–6 sentences, French question, three choices, answer index.
READINGS = {
    "star-wars": [
        ("Le petit droïde", "words", ["R2-D2 is a small droid.", "He has a blue box."], "De quelle couleur est la boîte ?", ["Bleue", "Rouge", "Verte"], 0),
        ("Une mission dans les étoiles", "verbs", ["Leia has a map.", "Luke can fly the ship.", "They look at a bright star.", "The ship goes to a new planet."], "Que peut faire Luke ?", ["Piloter le vaisseau", "Dessiner une carte", "Ouvrir une boîte"], 0),
        ("Le message de Leia", "listening", ["It is night on the planet.", "Leia is ready for the mission.", "She can find the small droid.", "The droid has a map.", "They go home together."], "Qui a une carte ?", ["Le droïde", "La lune", "Le casque"], 0),
    ],
    "harry-potter": [
        ("La chouette près de la fenêtre", "words", ["The owl is near the window.", "It has a letter."], "Qu'a la chouette ?", ["Une lettre", "Une écharpe", "Une bougie"], 0),
        ("Un matin à Poudlard", "verbs", ["Harry is early today.", "Hermione can read a book.", "Harry can carry the bag.", "They go to the classroom."], "Que peut faire Hermione ?", ["Lire un livre", "Porter le sac", "Fermer la fenêtre"], 0),
        ("La salle de classe", "listening", ["The classroom is warm.", "A candle is on the table.", "The red scarf is next to a book.", "The owl waits at the window.", "The room is quiet now."], "Où est la chouette ?", ["À la fenêtre", "Sur la table", "Sous le livre"], 0),
    ],
    "chess": [
        ("Le premier coup", "words", ["The board has black and white squares.", "A pawn makes the first move."], "Quelle pièce joue en premier ?", ["Le pion", "La reine", "Le cavalier"], 0),
        ("Le roi réfléchit", "verbs", ["The king can think before he moves.", "The queen waits for her turn.", "A knight moves to a white square.", "The king is safe."], "Qui attend son tour ?", ["La reine", "Le roi", "Le cavalier"], 0),
        ("Une partie tranquille", "listening", ["It is my turn.", "I can choose a move.", "The knight goes left.", "The queen goes right.", "Now I count my pieces."], "Dans quelle direction va le cavalier ?", ["À gauche", "À droite", "Tout droit"], 0),
    ],
    "video-games": [
        ("La pièce d'or", "words", ["Nova has a gold coin.", "The coin is near a blue bridge."], "Où est la pièce ?", ["Près d'un pont", "Dans une grotte", "Derrière une porte"], 0),
        ("Pixel explore", "verbs", ["Pixel can climb a hill.", "Nova can build a bridge.", "They explore a dark cave.", "A key is behind the door."], "Que construit Nova ?", ["Un pont", "Une porte", "Une colline"], 0),
        ("Le niveau secret", "listening", ["Nova has a secret map.", "A yellow star is above the cave.", "Pixel can collect a coin.", "The shield is blue.", "They finish the level together."], "De quelle couleur est le bouclier ?", ["Bleu", "Jaune", "Rouge"], 0),
    ],
    "travel": [
        ("À la gare", "words", ["Mia is at the station.", "Her ticket is in her bag."], "Où est le billet ?", ["Dans le sac", "Dans la valise", "Sur la plage"], 0),
        ("Le voyage commence", "verbs", ["Mia can pack her suitcase.", "She walks to the station.", "The train goes north.", "She can visit a new city."], "Où va le train ?", ["Vers le nord", "Vers le sud", "Vers la plage"], 0),
        ("Une ville nouvelle", "listening", ["It is rainy in the city.", "The hotel is near the park.", "Mia has a blue umbrella.", "Her passport is in her bag.", "Tomorrow she will see the beach."], "Quel temps fait-il en ville ?", ["Il pleut", "Il neige", "Il fait très chaud"], 0),
    ],
    "animals": [
        ("La tortue verte", "words", ["A green turtle is near the pond.", "A yellow butterfly flies above it."], "Quel animal est vert ?", ["La tortue", "Le papillon", "Le canard"], 0),
        ("Les amis du refuge", "verbs", ["The rabbit can hop.", "The duck can swim.", "The cat can sleep in the sun.", "The dog can listen to Luna."], "Quel animal peut nager ?", ["Le canard", "Le lapin", "Le chat"], 0),
        ("Le jardin de Luna", "listening", ["The rabbit is soft and small.", "The turtle is slow.", "The fox is hungry.", "Luna gives the fox some food.", "All the animals rest together."], "Quel animal a faim ?", ["Le renard", "La tortue", "Le lapin"], 0),
    ],
}


def voice(locale, text):
    return {"locale": locale, "text": text}


def normalize_old_audio(mission):
    # Full French clauses avoid rapid player switches inside a sentence. The
    # learning target and example remain separate, native-accent English clips.
    welcome = {
        "star-wars": "Bienvenue dans une aventure avec Luke, Leia et le droïde R2-D2.",
        "harry-potter": "Bienvenue à Poudlard avec Harry et Hermione.",
        "chess": "Bienvenue dans le royaume des échecs.",
        "video-games": "Bienvenue dans le jeu de Nova et Pixel.",
        "travel": "Bienvenue dans le grand voyage de Mia et Léo.",
        "animals": "Bienvenue au refuge de Luna.",
    }[mission["universeId"]]
    english_line = next((x["text"] for x in mission["intro"] if x["locale"] == "en" and len(x["text"].split()) > 3), "Hello! Welcome to the team.")
    mission["intro"] = [voice("fr-FR", welcome + " Écoute les mots du cours, puis réponds à ton rythme."),
                        voice("en", english_line)]
    mission["outro"] = [voice("fr-FR", "Bravo, tu as terminé cette aventure. Reviens bientôt découvrir d'autres mots et d'autres histoires."),
                        voice("en", "Great job! See you soon.")]
    for chapter in mission["lessons"]:
        if chapter["topicId"] not in {"pronouns", "verbs", "words", "listening"}:
            continue
        if chapter["topicId"] == "verbs":
            chapter["intro"] = [voice("fr-FR", "Découvrons des actions simples. Après le mot qui signifie pouvoir, le verbe reste tout simple.")]
        elif chapter["topicId"] == "words":
            chapter["intro"] = [voice("fr-FR", "Découvrons plusieurs mots utiles pour cette aventure. Écoute-les un par un.")]
        elif chapter["topicId"] == "listening":
            chapter["intro"] = [voice("fr-FR", "Écoute de petites phrases et retrouve le mot important. Un seul mot en anglais suffit pour répondre.")]
        for card in chapter["theory"]:
            kind = chapter["topicId"]
            meaning = card["meaning"].rstrip(".!?")
            pronoun_notes = {
                "i": "Ce pronom signifie je. Il sert à parler de soi.",
                "you": "Ce pronom signifie tu ou vous. Il sert à parler à une personne.",
                "he": "Ce pronom signifie il. Il désigne un garçon ou un personnage masculin.",
                "she": "Ce pronom signifie elle. Il désigne une fille ou un personnage féminin.",
                "we": "Ce pronom signifie nous. Il sert à parler d'un groupe dont on fait partie.",
                "they": "Ce pronom signifie ils ou elles. Il sert à parler de plusieurs personnes.",
                "i am": "Cette expression signifie je suis. Le premier mot parle de toi, et le second est une forme du verbe être.",
                "you are": "Cette expression signifie tu es ou vous êtes. Elle sert à décrire la personne à qui l'on parle.",
                "he is": "Cette expression signifie il est. Elle sert à décrire un garçon ou un personnage masculin.",
                "she is": "Cette expression signifie elle est. Elle sert à décrire une fille ou un personnage féminin.",
                "we are": "Cette expression signifie nous sommes. Elle parle d'un groupe dont tu fais partie.",
                "they are": "Cette expression signifie ils sont ou elles sont. Elle parle de plusieurs personnes.",
            }
            explanation = {
                "pronouns": pronoun_notes.get(card["word"].lower(),
                    f"Cette expression signifie {meaning}. Elle aide à parler de soi ou des autres.") + " Écoute un exemple.",
                "verbs": f"{'Ce mot' if len(card['word'].split()) == 1 else 'Cette expression'} signifie {meaning}. Écoute comment l'utiliser dans une phrase.",
                "words": f"Ce mot signifie {meaning}. Écoute maintenant une phrase d'exemple.",
                "listening": f"Cherche ce mot dans la phrase que tu vas entendre. Il signifie {meaning}.",
            }[kind]
            card["audio"] = [voice("en", card["word"].rstrip(".!?") + "."),
                             voice("fr-FR", explanation), voice("en", card["example"]),
                             voice("fr-FR", "La phrase signifie : " + card["translation"])]
        for exercise in chapter["exercises"]:
            exercise.pop("questionAudio", None)
            exercise.pop("contextAudio", None)
            if len(exercise["hint"])>2:
                english = next((part["text"] for part in reversed(exercise["hint"])
                                if part["locale"]=="en"), exercise["model"])
                exercise["hint"] = [voice("fr-FR", "Écoute le modèle en anglais."),
                                    voice("en", english)]


for mission in catalog["universes"]:
    world = mission["universeId"]
    normalize_old_audio(mission)
    for chapter in mission["lessons"]:
        skill = chapter["topicId"]
        if skill not in NEW[world]:
            continue
        own_prefix = f"{world}-{skill}-extra-"
        previous = [x for x in chapter["exercises"] if x["id"].startswith(own_prefix)]
        if previous:
            chapter["theory"] = chapter["theory"][:-len(previous)]
            chapter["exercises"] = [x for x in chapter["exercises"]
                                    if not x["id"].startswith(own_prefix)]
        known = {x["id"] for x in chapter["exercises"]}
        for number, (word, meaning, example, translation) in enumerate(NEW[world][skill], 1):
            ident = f"{world}-{skill}-extra-{number}"
            if ident in known:
                continue
            stage = (1, 1, 2, 3)[number - 1]
            explanation = {
                "verbs": f"Cette action veut dire {meaning}. Écoute la phrase, puis essaie de la dire.",
                "words": f"Ce nouveau mot veut dire {meaning}. Écoute son exemple.",
                "listening": f"Repère le mot qui signifie {meaning} dans la phrase.",
            }[skill]
            card = {"stage": stage, "word": word, "meaning": meaning,
                    "explanation": explanation, "example": example, "translation": translation,
                    "audio": [voice("en", word + "."), voice("fr-FR", explanation),
                              voice("en", example), voice("fr-FR", "La phrase signifie : " + translation)]}
            chapter["theory"].append(card)
            if skill == "verbs":
                model = example
                question = "Dis : « " + translation.rstrip(".") + ". »"
                answers = [model.lower().rstrip(".")]
                context = ""
            elif skill == "words":
                model = word[0].upper() + word[1:] + "."
                question = "Comment dit-on « " + meaning + " » en anglais ?"
                answers = [word.lower()]
                context = ""
            else:
                model = word[0].upper() + word[1:] + "."
                question = example
                answers = [word.lower()]
                context = "Écoute la phrase et retrouve le mot nouveau."
            chapter["exercises"].append({"id": ident, "stage": stage,
                "question": question, "questionLocale": "en" if skill == "listening" else "fr-FR",
                "context": context, "model": model,
                "translation": translation if skill == "verbs" else meaning,
                "answers": answers,
                "hint": [voice("fr-FR", "Écoute le modèle."), voice("en", model)]})
    mission["readingPassages"] = [
        {"id": f"{world}-reading-{i}", "title": title, "topicId": skill,
         "sentences": sentences, "question": question, "choices": choices,
         "correctIndex": answer}
        for i, (title, skill, sentences, question, choices, answer)
        in enumerate(READINGS[world], 1)
    ]

PATH.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n")
print("Added 72 themed questions and 18 guided readings; normalized lesson audio.")
