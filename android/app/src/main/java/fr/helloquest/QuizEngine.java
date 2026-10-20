package fr.helloquest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Short listening quiz drawn only from completed theory lessons. */
final class QuizEngine {
    static final class Question {
        final String key;
        final QuestContent.Universe mission;
        final QuestContent.Lesson lesson;
        final QuestContent.TheoryCard card;
        final List<String> choices;
        final boolean wordOnly;

        Question(String key,QuestContent.Universe mission,QuestContent.Lesson lesson,
                 QuestContent.TheoryCard card,List<String> choices,boolean wordOnly) {
            this.key=key;this.mission=mission;this.lesson=lesson;
            this.card=card;this.choices=choices;this.wordOnly=wordOnly;
        }
        String answer() {return wordOnly?card.meaning:card.translation;}
        String spoken() {return wordOnly?card.word:card.example;}
    }

    static List<Question> create(QuestContent content,LearningProgress progress,String world,
                                 String topicId,int limit) {
        List<Question> available=new ArrayList<>();
        List<QuestContent.Universe> missions=new ArrayList<>();
        if("all".equals(world))missions.addAll(content.universes);
        else missions.add(content.universe(world));
        for(QuestContent.Universe mission:missions) {
            for(QuestContent.Lesson chapter:mission.lessons) {
                if(!"all".equals(world)&&!topicId.equals(chapter.topicId))continue;
                if(!progress.hasCompletedCourse(mission.universeId,chapter.topicId))continue;
                for(int i=0;i<chapter.theory.size();i++) {
                    QuestContent.TheoryCard card=chapter.theory.get(i);
                    String key=mission.universeId+"_"+chapter.topicId+"_"+i;
                    available.add(new Question(key,mission,chapter,card,Collections.emptyList(),false));
                }
            }
        }
        Collections.shuffle(available);
        available.sort(Comparator.comparingLong(q->progress.quizLast(q.key)));
        List<Question> result=new ArrayList<>();
        Set<String> usedWords=new LinkedHashSet<>();
        for(Question candidate:available) {
            if(result.size()>=limit)break;
            String word=candidate.card.word.toLowerCase(java.util.Locale.ROOT).trim();
            if(usedWords.contains(word))continue;
            boolean wordOnly=result.size()%2==0;
            LinkedHashSet<String> answers=new LinkedHashSet<>();
            String correct=wordOnly?candidate.card.meaning:candidate.card.translation;
            answers.add(correct);
            for(QuestContent.TheoryCard card:candidate.lesson.theory)
                if(card!=candidate.card)answers.add(wordOnly?card.meaning:card.translation);
            for(Question other:available)
                if(other!=candidate&&other.lesson.topicId.equals(candidate.lesson.topicId))
                    answers.add(wordOnly?other.card.meaning:other.card.translation);
            if(answers.size()<3)continue;
            List<String> distractors=new ArrayList<>(answers);
            distractors.remove(correct);
            Collections.shuffle(distractors);
            List<String> choices=new ArrayList<>();
            choices.add(correct);
            choices.add(distractors.get(0));choices.add(distractors.get(1));
            Collections.shuffle(choices);
            result.add(new Question(candidate.key,candidate.mission,candidate.lesson,
                    candidate.card,choices,wordOnly));
            usedWords.add(word);
        }
        return result;
    }
}
