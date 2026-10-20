"""Add the proposed CE2 subjects to the canonical catalogue, without changing existing lessons.

Each subject has shared examples and one example adapted to each universe.
Run this before exporting dialogues or generating the prepared audio.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "content/dialogues-poc.json"

WORLDS = {
    "star-wars": {"hero": "Luke", "hero_fr": "Luke", "hero_to_fr": "à Luke", "item": "the droid", "item_fr": "le droïde",
                  "place": "the ship", "place_fr": "le vaisseau"},
    "harry-potter": {"hero": "Harry", "hero_fr": "Harry", "hero_to_fr": "à Harry", "item": "the owl", "item_fr": "la chouette",
                     "place": "the castle", "place_fr": "le château"},
    "chess": {"hero": "the king", "hero_fr": "le roi", "hero_to_fr": "au roi", "item": "the pawn", "item_fr": "le pion",
              "place": "the board", "place_fr": "l'échiquier"},
    "video-games": {"hero": "Nova", "hero_fr": "Nova", "hero_to_fr": "à Nova", "item": "the coin", "item_fr": "la pièce",
                    "place": "the level", "place_fr": "le niveau"},
    "travel": {"hero": "Mia", "hero_fr": "Mia", "hero_to_fr": "à Mia", "item": "the train", "item_fr": "le train",
               "place": "the station", "place_fr": "la gare"},
    "animals": {"hero": "Luna", "hero_fr": "Luna", "hero_to_fr": "à Luna", "item": "the fox", "item_fr": "le renard",
                "place": "the shelter", "place_fr": "le refuge"},
}


def lines(value):
    result = [tuple(part.strip() for part in line.split("|"))
              for line in value.strip().splitlines()]
    assert all(len(row) == 4 for row in result)
    return result


def small_numbers():
    names = ["zero", "one", "two", "three", "four", "five", "six", "seven",
             "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen",
             "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"]
    french = ["zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept",
              "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze",
              "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"]
    result = []
    for number, (word, meaning) in enumerate(zip(names, french)):
        example = f"I see {word} stars." if number != 1 else "I see one star."
        translation = f"Je vois {meaning} étoiles." if number != 1 else "Je vois une étoile."
        if number == 0:
            translation = "Je ne vois aucune étoile."
        result.append((word, meaning, example, translation))
    return result


def alphabet():
    return [(f"letter {letter}", f"la lettre {letter}",
             f"I say the letter {letter}.", f"Je dis la lettre {letter}.")
            for letter in "ABCDEFGHIJKLMNOPQRSTUVWXY"]


def calendar():
    days = [("Monday", "lundi"), ("Tuesday", "mardi"), ("Wednesday", "mercredi"),
            ("Thursday", "jeudi"), ("Friday", "vendredi"), ("Saturday", "samedi"),
            ("Sunday", "dimanche")]
    months = [("January", "janvier"), ("February", "février"), ("March", "mars"),
              ("April", "avril"), ("May", "mai"), ("June", "juin"),
              ("July", "juillet"), ("August", "août"), ("September", "septembre"),
              ("October", "octobre"), ("November", "novembre"),
              ("December", "décembre")]
    return ([(word, meaning, f"It is {word}.", f"Nous sommes {meaning}.")
             for word, meaning in days] +
            [(word, meaning, f"It is {word}.", f"Nous sommes en {meaning}.")
             for word, meaning in months])


# id, label, short spoken lesson introduction, core examples, one universe example.
SUBJECTS = [
    ("colors", "Couleurs", "Une couleur décrit ce que nous voyons. Écoute chaque couleur dans une petite phrase.", lines("""
        red|rouge|The light is red.|La lumière est rouge.
        green|vert|The leaf is green.|La feuille est verte.
        yellow|jaune|The sun is yellow.|Le soleil est jaune.
        black|noir|The cat is black.|Le chat est noir.
    """), ("blue", "bleu", "I see blue near {item}.", "Je vois du bleu près de {item_fr}.")),
    ("numbers_0_20", "Nombres de 0 à 20", "On compte de petites quantités. Écoute le nombre, puis retrouve-le dans la phrase.", small_numbers(),
     ("twenty", "vingt", "I count twenty steps with {hero}.", "Je compte vingt pas avec {hero}.")),
    ("numbers_21_100", "Nombres de 21 à 100", "Les dizaines permettent de compter plus loin. Écoute bien leur début et leur fin.", lines("""
        twenty-one|vingt et un|I count twenty-one points.|Je compte vingt et un points.
        thirty-two|trente-deux|There are thirty-two pages.|Il y a trente-deux pages.
        forty-five|quarante-cinq|We have forty-five minutes.|Nous avons quarante-cinq minutes.
        sixty|soixante|I see sixty lights.|Je vois soixante lumières.
    """), ("one hundred", "cent", "I count one hundred points with {hero}.", "Je compte cent points avec {hero}.")),
    ("alphabet", "Alphabet et épellation", "Les lettres servent à épeler un nom. Écoute leur nom anglais, différent du français.", alphabet(),
     ("letter Z", "la lettre Z", "{hero} says the letter Z.", "{hero} dit la lettre Z.")),
    ("greetings", "Salutations et politesse", "Ces expressions servent à commencer et terminer une rencontre avec gentillesse.", lines("""
        hello|bonjour|Hello, my friend!|Bonjour, mon ami !
        good morning|bonjour le matin|Good morning, everyone!|Bonjour à tous !
        good evening|bonsoir|Good evening, Dad!|Bonsoir, papa !
        goodbye|au revoir|Goodbye, see you soon!|Au revoir, à bientôt !
    """), ("thank you", "merci", "I say thank you to {hero}.", "Je dis merci {hero_to_fr}.")),
    ("introductions", "Se présenter", "Pour se présenter, on peut dire son nom, son âge et où l'on habite.", lines("""
        my name is|je m'appelle|My name is Alex.|Je m'appelle Alex.
        I am eight|j'ai huit ans|I am eight years old.|J'ai huit ans.
        I live in|j'habite à|I live in Paris.|J'habite à Paris.
        nice to meet you|enchanté|Nice to meet you!|Enchanté de te rencontrer !
    """), ("this is", "voici", "This is {hero}.", "Voici {hero}.")),
    ("family", "Famille", "Écoute les mots pour parler des personnes de ta famille.", lines("""
        mother|mère|My mother is here.|Ma mère est ici.
        father|père|My father can help.|Mon père peut aider.
        sister|sœur|My sister likes music.|Ma sœur aime la musique.
        brother|frère|My brother is tall.|Mon frère est grand.
    """), ("cousin", "cousin", "{hero} has a cousin.", "{hero} a un cousin.")),
    ("body", "Parties du corps", "Le corps a des parties que l'on peut nommer avec des mots courts.", lines("""
        head|tête|My head is warm.|Ma tête est chaude.
        hand|main|I raise my hand.|Je lève la main.
        foot|pied|My foot is on the floor.|Mon pied est sur le sol.
        eyes|yeux|I close my eyes.|Je ferme les yeux.
    """), ("ears", "oreilles", "{hero} has two ears.", "{hero} a deux oreilles.")),
    ("clothes", "Vêtements", "Les vêtements protègent du froid ou du soleil. Écoute leurs noms.", lines("""
        hat|chapeau|I wear a hat.|Je porte un chapeau.
        shoes|chaussures|My shoes are blue.|Mes chaussures sont bleues.
        coat|manteau|This coat is warm.|Ce manteau est chaud.
        shirt|chemise|The shirt is white.|La chemise est blanche.
    """), ("scarf", "écharpe", "{hero} has a scarf.", "{hero} a une écharpe.")),
    ("food", "Aliments et boissons", "Voici des aliments et des boissons faciles à reconnaître au quotidien.", lines("""
        apple|pomme|I eat an apple.|Je mange une pomme.
        bread|pain|The bread is fresh.|Le pain est frais.
        water|eau|I drink water.|Je bois de l'eau.
        milk|lait|The milk is cold.|Le lait est froid.
    """), ("banana", "banane", "{hero} has a banana.", "{hero} a une banane.")),
    ("school", "Objets de l’école", "À l'école, on utilise des objets pour lire, écrire et apprendre.", lines("""
        book|livre|I open my book.|J'ouvre mon livre.
        pen|stylo|The pen is on the desk.|Le stylo est sur le bureau.
        pencil|crayon|I draw with a pencil.|Je dessine avec un crayon.
        desk|bureau|My desk is small.|Mon bureau est petit.
    """), ("notebook", "cahier", "{hero} has a notebook.", "{hero} a un cahier.")),
    ("home", "Maison et pièces", "Une maison contient des pièces et des objets que l'on voit souvent.", lines("""
        door|porte|The door is open.|La porte est ouverte.
        window|fenêtre|I see a window.|Je vois une fenêtre.
        room|pièce|This room is big.|Cette pièce est grande.
        table|table|The book is on the table.|Le livre est sur la table.
    """), ("chair", "chaise", "{hero} sits on a chair.", "{hero} s'assoit sur une chaise.")),
    ("animals_vocab", "Animaux", "Écoute le nom de quelques animaux et retrouve-les dans les phrases.", lines("""
        cat|chat|The cat is sleeping.|Le chat dort.
        dog|chien|The dog can run.|Le chien peut courir.
        bird|oiseau|The bird is singing.|L'oiseau chante.
        fish|poisson|The fish can swim.|Le poisson peut nager.
    """), ("rabbit", "lapin", "{hero} sees a rabbit.", "{hero} voit un lapin.")),
    ("city", "Lieux de la ville", "Dans une ville, on peut chercher un parc, une gare ou un autre lieu.", lines("""
        park|parc|The park is near my home.|Le parc est près de chez moi.
        station|gare|The station is busy.|La gare est animée.
        hospital|hôpital|The hospital is in town.|L'hôpital est en ville.
        library|bibliothèque|I read at the library.|Je lis à la bibliothèque.
    """), ("museum", "musée", "{hero} goes to the museum.", "{hero} va au musée.")),
    ("countries", "Pays et nationalités", "Un pays a un nom ; une nationalité dit d'où vient une personne.", lines("""
        France|la France|France is a country.|La France est un pays.
        French|français|My friend is French.|Mon ami est français.
        England|l'Angleterre|England is an island country.|L'Angleterre est un pays insulaire.
        Spanish|espagnol|I can speak Spanish.|Je peux parler espagnol.
    """), ("country", "pays", "{hero} visits a new country.", "{hero} visite un nouveau pays.")),
    ("weather", "Météo et saisons", "Le temps qu'il fait change selon le jour et la saison.", lines("""
        sunny|ensoleillé|It is sunny today.|Il y a du soleil aujourd'hui.
        rainy|pluvieux|It is rainy outside.|Il pleut dehors.
        cold|froid|It is cold in winter.|Il fait froid en hiver.
        winter|hiver|Winter comes after autumn.|L'hiver vient après l'automne.
    """), ("summer", "été", "{hero} likes summer.", "{hero} aime l'été.")),
    ("calendar", "Jours, mois et dates", "Les jours et les mois aident à raconter quand quelque chose se passe.", calendar(),
     ("tomorrow", "demain", "{hero} will come tomorrow.", "{hero} viendra demain.")),
    ("time", "Lire l’heure", "Écoute comment annoncer une heure entière, une demi-heure ou un quart d'heure.", lines("""
        one o'clock|une heure|It is one o'clock.|Il est une heure.
        two o'clock|deux heures|It is two o'clock.|Il est deux heures.
        half past three|trois heures et demie|It is half past three.|Il est trois heures et demie.
        quarter past four|quatre heures et quart|It is quarter past four.|Il est quatre heures et quart.
    """), ("noon", "midi", "I meet {hero} at noon.", "Je retrouve {hero} à midi.")),
    ("directions", "Directions", "Pour guider quelqu'un, écoute les mots qui indiquent une direction.", lines("""
        up|en haut|Look up at the sky.|Regarde le ciel en haut.
        down|en bas|The ball goes down.|La balle descend.
        left|à gauche|Turn left at the tree.|Tourne à gauche à l'arbre.
        right|à droite|Turn right at the door.|Tourne à droite à la porte.
    """), ("straight", "tout droit", "{hero} goes straight ahead.", "{hero} va tout droit.")),
    ("positions", "Positions et prépositions", "Les petits mots de position disent où se trouve un objet.", lines("""
        on|sur|The book is on the table.|Le livre est sur la table.
        under|sous|The cat is under the chair.|Le chat est sous la chaise.
        next to|à côté de|The bag is next to the door.|Le sac est à côté de la porte.
        behind|derrière|The ball is behind the box.|La balle est derrière la boîte.
    """), ("in front of", "devant", "I stand in front of {place}.", "Je me tiens devant {place_fr}.")),
    ("shapes", "Formes et tailles", "Une forme décrit le contour d'un objet ; une taille dit s'il est grand ou petit.", lines("""
        circle|cercle|I draw a circle.|Je dessine un cercle.
        square|carré|This box is a square.|Cette boîte forme un carré.
        big|grand|The tree is big.|L'arbre est grand.
        small|petit|The cup is small.|La tasse est petite.
    """), ("long", "long", "I draw a long line for {hero}.", "Je dessine une longue ligne pour {hero}.")),
    ("emotions", "Émotions", "Ces mots aident à dire comment on se sent.", lines("""
        happy|heureux|I am happy today.|Je suis heureux aujourd'hui.
        sad|triste|The child is sad.|L'enfant est triste.
        scared|effrayé|I am scared of the dark.|J'ai peur du noir.
        calm|calme|We are calm now.|Nous sommes calmes maintenant.
    """), ("excited", "enthousiaste", "{hero} is excited.", "{hero} est enthousiaste.")),
    ("hobbies", "Loisirs et sports", "On peut parler de ce que l'on aime faire pendant son temps libre.", lines("""
        play|jouer|I play after school.|Je joue après l'école.
        read|lire|I read a funny book.|Je lis un livre drôle.
        swim|nager|We swim in the pool.|Nous nageons dans la piscine.
        run|courir|I run in the park.|Je cours dans le parc.
    """), ("draw", "dessiner", "{hero} likes to draw.", "{hero} aime dessiner.")),
    ("questions", "Poser des questions", "Un mot au début d'une question indique ce que l'on cherche à savoir.", lines("""
        who|qui|Who is at the door?|Qui est à la porte ?
        what|quoi|What is in the box?|Qu'y a-t-il dans la boîte ?
        where|où|Where is my bag?|Où est mon sac ?
        when|quand|When is the game?|Quand est le jeu ?
    """), ("how", "comment", "How is {hero}?", "Comment va {hero} ?")),
    ("sentence_structure", "Structure d’une phrase", "En anglais, une phrase simple place souvent la personne, puis l'action, puis ce qu'elle concerne.", lines("""
        I see|je vois|I see a cat.|Je vois un chat.
        you have|tu as|You have a book.|Tu as un livre.
        she likes|elle aime|She likes apples.|Elle aime les pommes.
        we play|nous jouons|We play a game.|Nous jouons à un jeu.
    """), ("they find", "ils trouvent", "They find {item}.", "Ils trouvent {item_fr}.")),
    ("articles", "Articles a, an, the", "Les articles accompagnent un nom. On dit souvent a devant une consonne et an devant une voyelle.", lines("""
        a|un ou une|I see a dog.|Je vois un chien.
        an|un ou une|I eat an apple.|Je mange une pomme.
        the|le ou la|The sun is bright.|Le soleil brille.
        some|des|I have some pencils.|J'ai des crayons.
    """), ("an old", "un vieux", "{hero} has an old map.", "{hero} a une vieille carte.")),
    ("plurals", "Singulier et pluriel", "Le pluriel parle de plusieurs choses. Souvent, un s apparaît à la fin du nom.", lines("""
        cats|chats|I see two cats.|Je vois deux chats.
        boxes|boîtes|There are three boxes.|Il y a trois boîtes.
        children|enfants|The children can play.|Les enfants peuvent jouer.
        feet|pieds|My feet are cold.|J'ai froid aux pieds.
    """), ("books", "livres", "{hero} has two books.", "{hero} a deux livres.")),
    ("negation", "Négation simple", "Pour dire non dans une phrase, on peut utiliser not, do not ou cannot.", lines("""
        not|ne pas|I am not tired.|Je ne suis pas fatigué.
        is not|n'est pas|The door is not open.|La porte n'est pas ouverte.
        do not|ne ... pas|I do not like rain.|Je n'aime pas la pluie.
        cannot|ne peux pas|I cannot fly.|Je ne peux pas voler.
    """), ("never", "jamais", "{hero} never stops.", "{hero} ne s'arrête jamais.")),
    ("possessives", "Adjectifs possessifs", "Ces petits mots disent à qui appartient quelque chose.", lines("""
        my|mon ou ma|My bag is blue.|Mon sac est bleu.
        your|ton ou ta|Your book is here.|Ton livre est ici.
        his|son à lui|His hat is red.|Son chapeau à lui est rouge.
        her|son à elle|Her shoes are clean.|Ses chaussures à elle sont propres.
    """), ("our", "notre", "This is our team with {hero}.", "C'est notre équipe avec {hero}.")),
    ("to_be", "Le verbe to be", "Le verbe être change selon la personne : am, are ou is.", lines("""
        I am|je suis|I am ready.|Je suis prêt.
        you are|tu es|You are my friend.|Tu es mon ami.
        he is|il est|He is here.|Il est ici.
        she is|elle est|She is kind.|Elle est gentille.
    """), ("we are", "nous sommes", "We are with {hero}.", "Nous sommes avec {hero}.")),
    ("to_have", "Le verbe to have", "Le verbe avoir s'écrit have avec je, tu ou nous, mais has avec il ou elle.", lines("""
        I have|j'ai|I have a map.|J'ai une carte.
        you have|tu as|You have a key.|Tu as une clé.
        he has|il a|He has a ball.|Il a une balle.
        she has|elle a|She has a book.|Elle a un livre.
    """), ("we have", "nous avons", "We have a plan with {hero}.", "Nous avons un plan avec {hero}.")),
    ("can", "Can et can’t", "Can dit ce qu'on peut faire ; can't dit ce qu'on ne peut pas faire.", lines("""
        I can|je peux|I can jump.|Je peux sauter.
        you can|tu peux|You can help.|Tu peux aider.
        he can|il peut|He can swim.|Il peut nager.
        she can|elle peut|She can sing.|Elle peut chanter.
    """), ("we can't", "nous ne pouvons pas", "We can't see {item}.", "Nous ne pouvons pas voir {item_fr}.")),
    ("present_simple", "Présent simple", "Le présent simple parle d'une habitude. Avec il ou elle, le verbe prend souvent un s.", lines("""
        I play|je joue|I play every day.|Je joue tous les jours.
        she plays|elle joue|She plays on Sunday.|Elle joue dimanche.
        we walk|nous marchons|We walk to school.|Nous allons à l'école à pied.
        he reads|il lit|He reads at night.|Il lit le soir.
    """), ("likes", "aime", "{hero} likes the story.", "{hero} aime l'histoire.")),
    ("third_person_s", "Le s avec he, she, it", "Au présent simple, avec he, she ou it, on ajoute souvent un s au verbe. Avec I, you, we et they, on garde le verbe sans s.", lines("""
        I play|je joue|I play every day.|Je joue tous les jours.
        he plays|il joue|He plays every day.|Il joue tous les jours.
        she reads|elle lit|She reads a book.|Elle lit un livre.
        it runs|il ou elle court|It runs fast.|Il ou elle court vite.
    """), ("he likes", "il aime", "{hero} likes {item}.", "{hero} aime {item_fr}.")),
    ("present_continuous", "Présent continu", "Cette forme avec am, is ou are et un verbe en ing décrit une action en cours.", lines("""
        I am reading|je suis en train de lire|I am reading now.|Je lis en ce moment.
        she is running|elle court maintenant|She is running fast.|Elle court vite en ce moment.
        we are playing|nous jouons maintenant|We are playing outside.|Nous jouons dehors en ce moment.
        he is walking|il marche maintenant|He is walking home.|Il rentre à pied en ce moment.
    """), ("is looking", "regarde en ce moment", "{hero} is looking at {item}.", "{hero} regarde {item_fr} en ce moment.")),
    ("past_simple", "Passé simple des verbes fréquents", "Le passé parle de ce qui est déjà arrivé. Certains verbes changent beaucoup de forme.", lines("""
        played|a joué|I played yesterday.|J'ai joué hier.
        walked|a marché|She walked to school.|Elle est allée à l'école à pied.
        saw|a vu|He saw a bird.|Il a vu un oiseau.
        went|est allé|We went to the park.|Nous sommes allés au parc.
    """), ("found", "a trouvé", "{hero} found {item}.", "{hero} a trouvé {item_fr}.")),
    ("future_going_to", "Futur avec going to", "Going to annonce une action prévue pour plus tard.", lines("""
        I am going to play|je vais jouer|I am going to play tomorrow.|Je vais jouer demain.
        she is going to read|elle va lire|She is going to read tonight.|Elle va lire ce soir.
        we are going to travel|nous allons voyager|We are going to travel soon.|Nous allons bientôt voyager.
        he is going to eat|il va manger|He is going to eat later.|Il va manger plus tard.
    """), ("is going to find", "va trouver", "{hero} is going to find {item}.", "{hero} va trouver {item_fr}.")),
]


def segment(locale, text):
    return {"locale": locale, "text": text}


def make_lesson(topic, world):
    topic_id, label, introduction, common, world_row = topic
    values = WORLDS[world]
    world_word, world_meaning, world_example, world_translation = world_row
    example = world_example.format(**values)
    translation = world_translation.format(**{**values, "hero": values["hero_fr"]})
    translation = translation.replace("près de le ", "près du ")
    rows = common + [(world_word, world_meaning,
                      example[0].upper() + example[1:],
                      translation[0].upper() + translation[1:])]
    assert len(rows) >= 5
    assert len({row[0].lower() for row in rows}) == len(rows), (world, topic_id)
    assert len({row[3] for row in rows}) == len(rows), (world, topic_id)
    theory = []
    exercises = []
    for number, (word, meaning, example, translation) in enumerate(rows, 1):
        if topic_id == "third_person_s":
            explanation = ("Avec I, le verbe garde sa forme simple : play, sans s."
                           if number == 1 else
                           "Avec he ou she, on ajoute s à la fin du verbe au présent simple."
                           if number in (2, 3, 5) else
                           "Avec it, on ajoute aussi s : run devient runs.")
        else:
            noun = "Ce mot" if len(word.split()) == 1 else "Cette expression"
            explanation = f"{noun} signifie {meaning}. Écoute sa phrase d'exemple."
        theory.append({
            "word": word, "meaning": meaning, "explanation": explanation,
            "example": example, "translation": translation, "stage": 1,
            "audio": [segment("en", word.rstrip(".!?") + "."),
                      segment("fr-FR", explanation), segment("en", example),
                      segment("fr-FR", "La phrase signifie : " + translation)],
        })
        exercises.append({
            "id": f"{world}-{topic_id}-{number}", "stage": 1,
            "question": "Écoute, puis répète la phrase.", "questionLocale": "fr-FR",
            "questionAudio": [segment("fr-FR", "Écoute, puis répète la phrase."),
                              segment("en", example)],
            "model": example, "translation": translation,
            "answers": [example.lower().rstrip(".!?")],
            "hint": [segment("fr-FR", "Écoute encore la phrase."),
                     segment("en", example)],
        })
    return {"topicId": topic_id, "title": f"{label} avec {values['hero_fr']}",
            "intro": [segment("fr-FR", introduction)],
            "theory": theory, "exercises": exercises}


def main():
    catalog = json.loads(PATH.read_text())
    assert len(SUBJECTS) == 37
    known_topics = {topic["id"] for topic in catalog["topics"]}
    for topic in SUBJECTS:
        topic_id, label = topic[:2]
        if topic_id not in known_topics:
            catalog["topics"].append({"id": topic_id, "label": label})
            known_topics.add(topic_id)
    added = 0
    for universe in catalog["universes"]:
        world = universe["universeId"]
        existing = {lesson["topicId"] for lesson in universe["lessons"]}
        for topic in SUBJECTS:
            if topic[0] not in existing:
                universe["lessons"].append(make_lesson(topic, world))
                added += 1
    PATH.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n")
    print(f"Added {added} lessons; {len(catalog['topics'])} topics and "
          f"{sum(len(universe['lessons']) for universe in catalog['universes'])} lessons total.")


if __name__ == "__main__":
    main()
