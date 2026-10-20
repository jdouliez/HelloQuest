package fr.helloquest;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Local memory for questions, rewards and review. */
final class LearningProgress {
    private final QuestContent content;
    private final SharedPreferences prefs;

    LearningProgress(QuestContent content, SharedPreferences prefs) {
        this.content=content;
        this.prefs=prefs;
        migrateLegacyStages();
        migrateWorldUnlocks();
    }

    private void migrateWorldUnlocks() {
        if(prefs.getBoolean("world_unlocks_v1",false))return;
        SharedPreferences.Editor edit=prefs.edit();
        String selected=prefs.getString("world","star-wars");
        for(QuestContent.Universe mission:content.universes)
            if(mission.universeId.equals(selected)||
                    prefs.getInt("missions_"+mission.universeId,0)>0)
                edit.putBoolean("unlocked_world_"+mission.universeId,true);
        edit.putBoolean("world_unlocks_v1",true).apply();
    }

    int worldUnlockGoal(String id) {
        if("all".equals(id))return 150;
        for(QuestContent.Universe universe:content.universes)
            if(universe.universeId.equals(id))return universe.unlockQuestions;
        return Integer.MAX_VALUE;
    }

    int worldUnlockMissions(String id) {
        if("all".equals(id))return 18;
        for(QuestContent.Universe universe:content.universes)
            if(universe.universeId.equals(id))return universe.unlockLessons;
        return Integer.MAX_VALUE;
    }

    boolean worldUnlocked(String id) {
        return (masteredQuestions()>=worldUnlockGoal(id)&&missions()>=worldUnlockMissions(id))||
                (!"all".equals(id)&&prefs.getBoolean("unlocked_world_"+id,false));
    }

    private static String seenKey(QuestContent.Exercise exercise) {
        return "exercise_seen_"+exercise.id;
    }

    private static String lastKey(QuestContent.Exercise exercise) {
        return "exercise_last_"+exercise.id;
    }

    private void migrateLegacyStages() {
        if(prefs.getBoolean("exercise_memory_v1",false))return;
        SharedPreferences.Editor edit=prefs.edit();
        for(QuestContent.Universe mission:content.universes) {
            for(QuestContent.Lesson chapter:mission.lessons) {
                int stage=prefs.getInt("stage_"+mission.universeId+"_"+chapter.topicId,1);
                int seenInStage=prefs.getInt("seen_"+mission.universeId+"_"+chapter.topicId,0);
                int stageIndex=0;
                for(QuestContent.Exercise exercise:chapter.exercises) {
                    if(exercise.stage==stage)stageIndex++;
                    if(exercise.stage<stage ||
                            (exercise.stage==stage && stageIndex<=seenInStage))
                        edit.putInt(seenKey(exercise),Math.max(1,prefs.getInt(seenKey(exercise),0)));
                }
            }
        }
        edit.putBoolean("exercise_memory_v1",true).apply();
    }

    int seenCount(QuestContent.Exercise exercise) {
        return prefs.getInt(seenKey(exercise),0);
    }

    int correctCount(QuestContent.Exercise exercise) {
        return prefs.getInt("exercise_correct_"+exercise.id,0);
    }

    int newQuestions(QuestContent.Lesson chapter) {
        int count=0;
        for(QuestContent.Exercise exercise:chapter.exercises)
            if(seenCount(exercise)==0)count++;
        return count;
    }

    static String conceptKey(QuestContent.Exercise exercise) {
        String answer=exercise.answers.isEmpty()?exercise.model:exercise.answers.get(0);
        return answer.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+"," ").trim();
    }

    private boolean conceptMastered(QuestContent.Exercise exercise) {
        String key=conceptKey(exercise);
        if(prefs.getInt("concept_correct_"+key,0)>0)return true;
        for(QuestContent.Universe mission:content.universes)
            for(QuestContent.Lesson chapter:mission.lessons)
                for(QuestContent.Exercise other:chapter.exercises)
                    if(key.equals(conceptKey(other))&&correctCount(other)>0)return true;
        return false;
    }

    List<QuestContent.Exercise> select(QuestContent.Lesson chapter,int limit,
                                       Set<String> usedConcepts) {
        List<QuestContent.Exercise> candidates=new ArrayList<>(chapter.exercises);
        Collections.shuffle(candidates);
        Comparator<QuestContent.Exercise> ranking=Comparator
                .comparingInt((QuestContent.Exercise e)->usedConcepts.contains(conceptKey(e))?1:0)
                .thenComparingInt(e->conceptMastered(e)?1:0)
                .thenComparingInt(this::seenCount)
                .thenComparingLong(e->prefs.getLong("concept_last_"+conceptKey(e),0))
                .thenComparingLong(e->prefs.getLong(lastKey(e),0));
        List<QuestContent.Exercise> chosen=new ArrayList<>();
        String[] formats={"order","reply","repeat","repeat"};
        int offset=limit<4?prefs.getInt("exercise_session_"+chapter.id,0)%formats.length:0;
        for(int slot=0;slot<Math.min(limit,formats.length);slot++) {
            String format=formats[(slot+offset)%formats.length];
            List<QuestContent.Exercise> matching=new ArrayList<>();
            for(QuestContent.Exercise exercise:candidates)
                if(format.equals(exercise.type))matching.add(exercise);
            if(matching.isEmpty())continue;
            Collections.shuffle(matching);
            matching.sort(ranking);
            QuestContent.Exercise next=matching.get(0);
            candidates.remove(next);
            chosen.add(next);
            usedConcepts.add(conceptKey(next));
        }
        while(!candidates.isEmpty()&&chosen.size()<limit) {
            candidates.sort(ranking);
            QuestContent.Exercise next=candidates.remove(0);
            chosen.add(next);
            usedConcepts.add(conceptKey(next));
        }
        candidates=chosen;
        Collections.shuffle(candidates);
        if(candidates.size()>1) {
            String key="exercise_order_"+chapter.exercises.get(0).id;
            String order=exerciseOrder(candidates);
            if(order.equals(prefs.getString(key,""))) {
                Collections.rotate(candidates,1);
                order=exerciseOrder(candidates);
            }
            prefs.edit().putString(key,order).apply();
        }
        return candidates;
    }

    private String exerciseOrder(List<QuestContent.Exercise> exercises) {
        StringBuilder order=new StringBuilder();
        for(QuestContent.Exercise exercise:exercises)order.append(exercise.id).append(',');
        return order.toString();
    }

    static final class Reward {
        final int xp;
        final boolean newQuestion;
        Reward(int xp,boolean newQuestion) {this.xp=xp;this.newQuestion=newQuestion;}
    }

    Reward record(QuestContent.Exercise exercise,boolean recognizedCorrect) {
        int seen=seenCount(exercise);
        int earned=recognizedCorrect?(seen==0?20:8):0;
        SharedPreferences.Editor edit=prefs.edit()
                .putInt(seenKey(exercise),seen+1)
                .putLong(lastKey(exercise),System.currentTimeMillis())
                .putLong("concept_last_"+conceptKey(exercise),System.currentTimeMillis())
                .putInt("xp_total",xp()+earned);
        if(recognizedCorrect)
            edit.putInt("exercise_correct_"+exercise.id,correctCount(exercise)+1)
                    .putInt("concept_correct_"+conceptKey(exercise),
                            prefs.getInt("concept_correct_"+conceptKey(exercise),0)+1)
                    .putInt("correct_"+exercise.topicId,
                    prefs.getInt("correct_"+exercise.topicId,0)+1);
        edit.apply();
        return new Reward(earned,seen==0);
    }

    void recordCourse(String world,String topicId) {
        String key=world+":"+topicId;
        prefs.edit().putInt("course_"+key,prefs.getInt("course_"+key,0)+1).apply();
    }

    boolean hasCompletedCourse(String world,String topicId) {
        String key=world+":"+topicId;
        return prefs.getInt("course_"+key,0)>0;
    }

    void recordExercises(String world,String topicId) {
        String lessonId="all".equals(world)?"all":content.universe(world).lesson(topicId).id;
        prefs.edit().putInt("missions_"+world,prefs.getInt("missions_"+world,0)+1)
                .putInt("exercise_session_"+lessonId,
                        prefs.getInt("exercise_session_"+lessonId,0)+1).apply();
    }

    int topicState(String world,String topicId) {
        if("all".equals(world)) {
            int best=0;
            for(QuestContent.Universe universe:content.universes)
                best=Math.max(best,topicState(universe.universeId,topicId));
            return best;
        }
        String key=world+":"+topicId;
        if(!hasCompletedCourse(world,topicId))return 0;
        if(prefs.getBoolean("quiz_valid_"+key,false))return 2;
        return 1;
    }

    boolean recordQuizOutcome(String world,String topicId,int correct,int total) {
        if("all".equals(world)||!hasCompletedCourse(world,topicId)||
                total<5||total>10||correct*4<=total*3)return false;
        prefs.edit().putBoolean("quiz_valid_"+world+":"+topicId,true).apply();
        return true;
    }

    long quizLast(String key) {return prefs.getLong("quiz_last_"+key,0);}

    long readingLast(String id) {return prefs.getLong("reading_last_"+id,0);}

    int recordReading(String id,boolean correct) {
        int earned=correct?8:0;
        prefs.edit().putLong("reading_last_"+id,System.currentTimeMillis())
                .putInt("xp_total",xp()+earned).apply();
        return earned;
    }

    int recordQuiz(String key,boolean correct) {
        int earned=correct?6:0;
        SharedPreferences.Editor edit=prefs.edit()
                .putLong("quiz_last_"+key,System.currentTimeMillis())
                .putInt("quiz_played",prefs.getInt("quiz_played",0)+1)
                .putInt("xp_total",xp()+earned);
        if(correct)edit.putInt("quiz_correct",quizCorrect()+1);
        edit.apply();
        return earned;
    }

    int quizPlayed() {return prefs.getInt("quiz_played",0);}
    int quizCorrect() {return prefs.getInt("quiz_correct",0);}

    long dialogueLast(String id) {return prefs.getLong("dialogue_last_"+id,0);}

    int recordDialogue(String id,boolean correct) {
        int earned=correct?8:0;
        prefs.edit().putLong("dialogue_last_"+id,System.currentTimeMillis())
                .putInt("xp_total",xp()+earned).apply();
        return earned;
    }

    int xp() {return prefs.getInt("xp_total",0);}
    int level() {return 1+xp()/100;}
    int levelProgress() {return xp()%100;}

    int uniqueQuestions() {
        int count=0;
        for(QuestContent.Universe mission:content.universes)
            for(QuestContent.Lesson chapter:mission.lessons)
                count+=chapter.exercises.size()-newQuestions(chapter);
        return count;
    }

    int masteredQuestions() {
        int count=0;
        for(QuestContent.Universe mission:content.universes)
            for(QuestContent.Lesson chapter:mission.lessons)
                for(QuestContent.Exercise exercise:chapter.exercises)
                    if(correctCount(exercise)>0)count++;
        return count;
    }

    int totalQuestions() {
        int count=0;
        for(QuestContent.Universe mission:content.universes)
            for(QuestContent.Lesson chapter:mission.lessons)
                count+=chapter.exercises.size();
        return count;
    }

    int missions() {
        int count=prefs.getInt("missions_all",0);
        for(QuestContent.Universe mission:content.universes)
            count+=prefs.getInt("missions_"+mission.universeId,0);
        return count;
    }

    int worldsVisited() {
        int count=0;
        for(QuestContent.Universe mission:content.universes)
            if(prefs.getInt("missions_"+mission.universeId,0)>0)count++;
        return count;
    }

    int correctAnswers() {
        int count=0;
        for(QuestContent.Topic topic:content.topics)
            count+=prefs.getInt("correct_"+topic.id,0);
        return count;
    }

    static final class Badge {
        final String icon,title,description;
        final int progress,goal;
        Badge(String icon,String title,String description,int progress,int goal) {
            this.icon=icon;this.title=title;this.description=description;
            this.progress=progress;this.goal=goal;
        }
        boolean earned() {return progress>=goal;}
    }

    List<Badge> badges() {
        List<Badge> result=new ArrayList<>();
        result.add(new Badge("🎒","Premier départ","Terminer 3 missions",missions(),3));
        result.add(new Badge("🧠","Mémoire vive","Découvrir 30 questions",uniqueQuestions(),30));
        result.add(new Badge("🎧","Bonne oreille","Faire reconnaître 25 réponses",correctAnswers(),25));
        result.add(new Badge("🌍","Explorateur","Visiter 4 aventures",worldsVisited(),4));
        result.add(new Badge("🌟","Étoile montante","Gagner 1 000 points",xp(),1000));
        result.add(new Badge("🎲","Grand mélange","Maîtriser 150 questions",masteredQuestions(),150));
        return result;
    }
}
