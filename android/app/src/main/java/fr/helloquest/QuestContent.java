package fr.helloquest;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class QuestContent {
    final List<Topic> topics;
    final List<Universe> universes;
    final JSONObject shared;

    private QuestContent(List<Topic> topics, List<Universe> universes, JSONObject shared) {
        this.topics = topics;
        this.universes = universes;
        this.shared = shared;
    }

    static QuestContent load(Context context) throws IOException, JSONException {
        JSONObject root = new JSONObject(read(context, "dialogues-poc.json"));
        JSONObject photos = new JSONObject(read(context, "lesson-photos.json"));
        List<Topic> topics = new ArrayList<>();
        JSONArray topicItems = root.getJSONArray("topics");
        for (int i = 0; i < topicItems.length(); i++) {
            JSONObject topic = topicItems.getJSONObject(i);
            topics.add(new Topic(topic.getString("id"), topic.getString("label")));
        }
        List<Universe> universes = new ArrayList<>();
        JSONArray items = root.getJSONArray("universes");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            List<Lesson> lessons = new ArrayList<>();
            JSONArray chapterItems = item.getJSONArray("lessons");
            for (int j = 0; j < chapterItems.length(); j++) {
                JSONObject chapter = chapterItems.getJSONObject(j);
                List<TheoryCard> theory = new ArrayList<>();
                JSONArray theoryItems = chapter.getJSONArray("theory");
                JSONObject topicPhotos = photos.getJSONObject(chapter.getString("topicId"));
                for (int t = 0; t < theoryItems.length(); t++) {
                    JSONObject card = theoryItems.getJSONObject(t);
                    theory.add(new TheoryCard(card.getString("word"), card.getString("meaning"),
                            card.getString("explanation"), card.getString("example"),
                            card.getString("translation"), segments(card.getJSONArray("audio")),
                            card.optInt("stage", 1),topicPhotos.getString(card.getString("word"))));
                }
                List<Exercise> exercises = new ArrayList<>();
                JSONArray exerciseItems = chapter.getJSONArray("exercises");
                for (int k = 0; k < exerciseItems.length(); k++) {
                    JSONObject e = exerciseItems.getJSONObject(k);
                    List<String> answers = new ArrayList<>();
                    JSONArray answerItems = e.getJSONArray("answers");
                    for (int n = 0; n < answerItems.length(); n++) answers.add(answerItems.getString(n));
                    exercises.add(new Exercise(e.getString("id"), e.optString("type", "repeat"),
                            chapter.getString("topicId"),
                            e.getString("question"), e.getString("questionLocale"),
                            e.optString("context", ""), e.getString("model"),
                            e.getString("translation"), answers,
                            segments(e.getJSONArray("hint")), e.optInt("stage", 1),
                            e.has("questionAudio")?segments(e.getJSONArray("questionAudio")):
                                    singleton(e.getString("questionLocale"),e.getString("question")),
                            e.has("contextAudio")?segments(e.getJSONArray("contextAudio")):
                                    singleton("fr-FR",e.optString("context", ""))));
                }
                String topicId = chapter.getString("topicId");
                if (topics.stream().noneMatch(topic -> topic.id.equals(topicId)))
                    throw new JSONException("Unknown topic: " + topicId);
                lessons.add(new Lesson(item.getString("universeId"), topicId,
                        chapter.getString("title"),
                        segments(chapter.getJSONArray("intro")), theory, exercises));
            }
            List<MiniDialogue> dialogues = new ArrayList<>();
            JSONArray dialogueItems=item.optJSONArray("miniDialogues");
            if(dialogueItems!=null)for(int d=0;d<dialogueItems.length();d++) {
                JSONObject dialogue=dialogueItems.getJSONObject(d);
                List<Segment> lines=new ArrayList<>();
                JSONArray spoken=dialogue.getJSONArray("lines");
                for(int n=0;n<spoken.length();n++)lines.add(new Segment("en",spoken.getString(n)));
                List<String> choices=new ArrayList<>();
                JSONArray answers=dialogue.getJSONArray("choices");
                for(int n=0;n<answers.length();n++)choices.add(answers.getString(n));
                dialogues.add(new MiniDialogue(dialogue.getString("id"),
                        dialogue.getString("topicId"),dialogue.getInt("stage"),lines,
                        dialogue.getString("question"),choices,dialogue.getInt("correctIndex")));
            }
            List<ReadingPassage> passages = new ArrayList<>();
            JSONArray passageItems=item.optJSONArray("readingPassages");
            if(passageItems!=null)for(int p=0;p<passageItems.length();p++) {
                JSONObject passage=passageItems.getJSONObject(p);
                List<String> sentences=new ArrayList<>();
                JSONArray sentenceItems=passage.getJSONArray("sentences");
                for(int n=0;n<sentenceItems.length();n++)sentences.add(sentenceItems.getString(n));
                List<String> choices=new ArrayList<>();
                JSONArray answerItems=passage.getJSONArray("choices");
                for(int n=0;n<answerItems.length();n++)choices.add(answerItems.getString(n));
                passages.add(new ReadingPassage(passage.getString("id"),passage.getString("topicId"),
                        passage.getString("title"),sentences,passage.getString("question"),
                        choices,passage.getInt("correctIndex")));
            }
            universes.add(new Universe(item.getString("universeId"),
                    item.getString("universeLabel"), item.getString("title"),
                    item.getString("description"), item.getInt("unlockQuestions"),
                    item.getInt("unlockLessons"), segments(item.getJSONArray("intro")),
                    lessons, dialogues, passages, segments(item.getJSONArray("outro"))));
        }
        return new QuestContent(Collections.unmodifiableList(topics),
                Collections.unmodifiableList(universes), root.getJSONObject("shared"));
    }

    List<Segment> shared(String name) {
        try { return segments(shared.getJSONArray(name)); }
        catch (JSONException e) { return Collections.emptyList(); }
    }

    Universe universe(String id) {
        for (Universe m : universes) if (m.universeId.equals(id)) return m;
        return universes.get(0);
    }

    Topic topic(String id) {
        for (Topic topic : topics) if (topic.id.equals(id)) return topic;
        return topics.get(0);
    }

    static String read(Context context, String name) throws IOException {
        try (InputStream stream = context.getAssets().open(name)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static List<Segment> segments(JSONArray array) throws JSONException {
        List<Segment> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            result.add(new Segment(item.getString("locale"), item.getString("text")));
        }
        return result;
    }

    private static List<Segment> singleton(String locale,String text) {
        List<Segment> result=new ArrayList<>();
        if(!text.isEmpty())result.add(new Segment(locale,text));
        return result;
    }

    static final class Segment {
        final String locale;
        final String text;
        Segment(String locale, String text) { this.locale = locale; this.text = text; }
    }

    static final class Exercise {
        final String id, type, topicId, question, questionLocale, context, model, translation;
        final int stage;
        final List<String> answers;
        final List<Segment> hint;
        final List<Segment> questionAudio,contextAudio;
        Exercise(String id, String type, String topicId, String question, String questionLocale,
                 String context, String model, String translation, List<String> answers, List<Segment> hint,
                 int stage,List<Segment> questionAudio,List<Segment> contextAudio) {
            this.id = id; this.type = type; this.topicId = topicId; this.question = question;
            this.questionLocale = questionLocale; this.context = context; this.model = model;
            this.translation = translation; this.answers = answers; this.hint = hint; this.stage = stage;
            this.questionAudio=questionAudio;this.contextAudio=contextAudio;
        }
    }

    static final class Topic {
        final String id, label;
        Topic(String id, String label) { this.id = id; this.label = label; }
    }

    static final class Lesson {
        final String id, universeId, topicId, title;
        final List<Segment> intro;
        final List<TheoryCard> theory;
        final List<Exercise> exercises;
        Lesson(String universeId, String topicId, String title, List<Segment> intro,
                List<TheoryCard> theory, List<Exercise> exercises) {
            this.id = universeId + ":" + topicId;
            this.universeId = universeId; this.topicId = topicId;
            this.title = title; this.intro = intro;
            this.theory = theory; this.exercises = exercises;
        }
    }

    static final class TheoryCard {
        final String word, meaning, explanation, example, translation, visual;
        final int stage;
        final List<Segment> audio;
        TheoryCard(String word, String meaning, String explanation, String example,
                   String translation, List<Segment> audio, int stage, String visual) {
            this.word = word; this.meaning = meaning; this.explanation = explanation;
            this.example = example; this.translation = translation; this.audio = audio;
            this.stage = stage; this.visual = visual;
        }
    }

    static final class Universe {
        final String universeId, label, title, description;
        final int unlockQuestions, unlockLessons;
        final List<Segment> intro, outro;
        final List<Lesson> lessons;
        final List<MiniDialogue> dialogues;
        final List<ReadingPassage> passages;
        Universe(String universeId, String label, String title, String description,
                int unlockQuestions, int unlockLessons,
                List<Segment> intro, List<Lesson> lessons,List<MiniDialogue> dialogues,
                List<ReadingPassage> passages,List<Segment> outro) {
            this.universeId = universeId; this.label = label;
            this.title = title; this.description = description;
            this.unlockQuestions=unlockQuestions;this.unlockLessons=unlockLessons;
            this.intro = intro; this.lessons = lessons; this.dialogues=dialogues; this.outro = outro;
            this.passages=passages;
        }
        Lesson lesson(String topicId) {
            for (Lesson lesson : lessons) if (lesson.topicId.equals(topicId)) return lesson;
            throw new IllegalArgumentException("No lesson for " + universeId + ":" + topicId);
        }
    }

    static final class MiniDialogue {
        final String id,topicId,question;
        final int stage,correctIndex;
        final List<Segment> lines;
        final List<String> choices;
        MiniDialogue(String id,String topicId,int stage,List<Segment> lines,
                     String question,List<String> choices,int correctIndex) {
            this.id=id;this.topicId=topicId;this.stage=stage;this.lines=lines;
            this.question=question;this.choices=choices;this.correctIndex=correctIndex;
        }
    }

    static final class ReadingPassage {
        final String id,topicId,title,question;
        final List<String> sentences,choices;
        final int correctIndex;
        ReadingPassage(String id,String topicId,String title,List<String> sentences,
                       String question,List<String> choices,int correctIndex) {
            this.id=id;this.topicId=topicId;this.title=title;this.sentences=sentences;
            this.question=question;this.choices=choices;this.correctIndex=correctIndex;
        }
    }
}
