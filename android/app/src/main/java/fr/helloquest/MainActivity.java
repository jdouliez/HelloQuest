package fr.helloquest;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.media.MediaBrowserCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import org.json.JSONException;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int PAPER = Color.rgb(248,249,255);
    private static final int INK = Color.rgb(37,35,68);
    private static final int MUTED = Color.rgb(104,100,121);
    private static final int LIGHT = Color.rgb(237,239,255);
    private static final int MINT = Color.rgb(213,241,230);
    private static final int RED = Color.rgb(255,216,216);
    private static final int GOLD = Color.rgb(255,241,199);
    private static final int CARD = Color.WHITE;
    private static final int REQUEST_MIC = 201;
    private static final int DEFAULT_THINK_SECONDS = 5;
    private static final long PASSIVE_ANSWER_MS = 15000;
    private static final long QUESTION_BREAK_MS = 7000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private QuestContent content;
    private AudioClips audio;
    private SharedPreferences prefs;
    private LearningProgress progress;
    private SpeechRecognizer recognizer;
    private MediaBrowserCompat carBrowser;
    private LinearLayout body;
    private ScrollView currentScroll;
    private String screen = "home";
    private String settingsFrom = "home";
    private String world = "star-wars";
    private String accent = "en-GB";
    private String selectedTopicId = "pronouns";
    private final LinkedHashSet<String> selectedTopicIds = new LinkedHashSet<>();
    private boolean randomTopics;
    private int questionsPerTheme = 4;
    private int thinkSeconds = DEFAULT_THINK_SECONDS;
    private boolean advancedMode;
    private final List<LessonItem> lesson = new ArrayList<>();
    private final List<String> newWorlds = new ArrayList<>();
    private final LinkedHashSet<String> unlockedBefore = new LinkedHashSet<>();
    private final List<QuizEngine.Question> quiz = new ArrayList<>();
    private final List<QuestContent.TheoryCard> theoryCards = new ArrayList<>();
    private final LinkedHashSet<String> earnedBefore = new LinkedHashSet<>();
    private final List<LearningProgress.Badge> newBadges = new ArrayList<>();
    private int missionXp;
    private int missionCorrect;
    private int missionNew;
    private boolean questionRecorded;
    private int step;
    private int readingAtStep;
    private int retries;
    private boolean listening;
    private boolean micOpen;
    private boolean listenOnly;
    private boolean paused;
    private boolean betweenQuestions;
    private boolean canSpeakNow;
    private boolean quizAnswered;
    private String quizSelectedChoice="";
    private int lessonSelectedChoice=-1;
    private final List<String> orderTiles=new ArrayList<>();
    private final List<Integer> orderChosen=new ArrayList<>();
    private final List<Button> orderTileButtons=new ArrayList<>();
    private TextView orderAnswerView;
    private TextView orderErrorView;
    private LinearLayout orderAnswerBox;
    private boolean orderWrong;
    private int dialogueSelectedChoice=-1;
    private int quizStep;
    private int quizCorrect;
    private int quizXp;
    private String quizMessage="";
    private boolean quizPassRecorded;
    private String quizWorld="star-wars";
    private String quizTopicId="pronouns";
    private QuestContent.MiniDialogue activeDialogue;
    private QuestContent.Universe dialogueMission;
    private QuestContent.ReadingPassage activePassage;
    private QuestContent.Universe passageMission;
    private boolean passageShown;
    private boolean passageAnswered;
    private int passageSelectedChoice=-1;
    private int readingSentence=-1;
    private final List<TextView> readingLines=new ArrayList<>();
    private boolean dialogueChallengeShown;
    private boolean dialogueAnswered;
    private boolean dialogueCorrect;
    private String carCheckMessage="";
    private QuestContent.Lesson theoryChapter;
    private QuestContent.Universe theoryMission;
    private int theoryIndex;
    private String status = "";
    private String recognizedText = "";
    private String spokenEnglish = "";
    private TextView statusView;
    private TextView captionView;
    private int sequenceId;
    private final Runnable stopPhoneForCar=()->{
        cancelCurrent();paused=true;
        if(!"home".equals(screen))showHome();
    };

    private int primaryColor() {
        if ("all".equals(world)) return Color.rgb(126,67,151);
        if ("harry-potter".equals(world)) return Color.rgb(112,54,133);
        if ("chess".equals(world)) return Color.rgb(21,101,106);
        if ("video-games".equals(world)) return Color.rgb(174,70,58);
        if ("travel".equals(world)) return Color.rgb(27,105,160);
        if ("animals".equals(world)) return Color.rgb(57,115,74);
        return Color.rgb(72,68,180);
    }

    private int softColor() {
        if ("all".equals(world)) return Color.rgb(246,231,255);
        if ("harry-potter".equals(world)) return Color.rgb(247,234,250);
        if ("chess".equals(world)) return Color.rgb(227,247,242);
        if ("video-games".equals(world)) return Color.rgb(255,238,224);
        if ("travel".equals(world)) return Color.rgb(227,244,255);
        if ("animals".equals(world)) return Color.rgb(229,247,225);
        return LIGHT;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        new WindowInsetsControllerCompat(getWindow(),getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        new WindowInsetsControllerCompat(getWindow(),getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        try {
            content = QuestContent.load(this);
            audio = new AudioClips(this);
        } catch (IOException | JSONException e) {
            TextView error = new TextView(this);
            error.setPadding(dp(24),dp(40),dp(24),dp(24));
            error.setText("Le catalogue audio est incomplet. Réinstalle l’APK.");
            setContentView(error);
            return;
        }
        prefs = getSharedPreferences("learning", MODE_PRIVATE);
        world = prefs.getString("world", "star-wars");
        accent = prefs.getString("accent", "en-GB");
        int oldDuration=prefs.getInt("duration",8);
        questionsPerTheme=prefs.getInt("questions_per_theme",oldDuration==5?2:oldDuration==12?6:4);
        questionsPerTheme=Math.max(2,Math.min(12,questionsPerTheme));
        thinkSeconds = prefs.getInt("think_seconds", DEFAULT_THINK_SECONDS);
        thinkSeconds=Math.max(5,Math.min(20,thinkSeconds));
        advancedMode = prefs.getBoolean("advanced_mode",false);
        progress=new LearningProgress(content,prefs);
        PlaybackCoordinator.registerPhone(stopPhoneForCar);
        selectedTopicId=prefs.getString("topic_id","");
        if(selectedTopicId.isEmpty()) {
            for(QuestContent.Topic topic:content.topics)
                if(prefs.getBoolean("skill_"+topic.id,false)) {
                    selectedTopicId=topic.id;
                    break;
                }
        }
        if(selectedTopicId.isEmpty()||content.topics.stream().noneMatch(
                topic->topic.id.equals(selectedTopicId)))
            selectedTopicId=content.topics.get(0).id;
        randomTopics=prefs.getBoolean("random_topics",false);
        selectedTopicIds.addAll(prefs.getStringSet("selected_topic_ids",
                Collections.singleton(selectedTopicId)));
        selectedTopicIds.removeIf(id->content.topics.stream().noneMatch(topic->topic.id.equals(id)));
        if(selectedTopicIds.isEmpty())selectedTopicIds.add(selectedTopicId);
        if(!selectedTopicIds.contains(selectedTopicId))selectedTopicId=selectedTopicIds.iterator().next();
        showHome();
    }

    private void saveChoices() {
        SharedPreferences.Editor edit = prefs.edit().putString("world", world)
                .putString("accent", accent)
                .putString("topic_id",selectedTopicId)
                .putStringSet("selected_topic_ids",new LinkedHashSet<>(selectedTopicIds))
                .putBoolean("random_topics",randomTopics);
        edit.apply();
    }

    private String topicSelectionLabel() {
        if(randomTopics)return "🎲 Aléatoire · "+content.topics.size()+" sujets";
        if(selectedTopicIds.size()==1)return content.topic(selectedTopicIds.iterator().next()).label;
        return selectedTopicIds.size()+" sujets sélectionnés";
    }

    private void chooseSessionTopic(boolean avoidCurrent) {
        List<String> candidates=new ArrayList<>();
        if(randomTopics)for(QuestContent.Topic topic:content.topics)candidates.add(topic.id);
        else candidates.addAll(selectedTopicIds);
        if(avoidCurrent&&candidates.size()>1)candidates.remove(selectedTopicId);
        Collections.shuffle(candidates);
        selectedTopicId=candidates.get(0);
        saveChoices();
    }

    private void startSelectedExercises() {
        List<String> eligible=new ArrayList<>();
        if("all".equals(world)) {
            for(QuestContent.Universe universe:content.universes)
                for(QuestContent.Topic topic:content.topics)
                    if(progress.hasCompletedCourse(universe.universeId,topic.id))
                        eligible.add(topic.id);
            if(eligible.isEmpty()) {showNoQuestions();return;}
            beginExercises();
            return;
        }
        if(randomTopics)for(QuestContent.Topic topic:content.topics)eligible.add(topic.id);
        else eligible.addAll(selectedTopicIds);
        eligible.removeIf(id->!progress.hasCompletedCourse(world,id));
        if(eligible.isEmpty()) {showNoQuestions();return;}
        Collections.shuffle(eligible);
        selectedTopicId=eligible.get(0);
        saveChoices();
        beginExercises();
    }
    private void startSelectedQuiz() {
        List<String> eligible=new ArrayList<>();
        if("all".equals(world)) {
            startQuiz();
            return;
        }
        if(randomTopics)for(QuestContent.Topic topic:content.topics)eligible.add(topic.id);
        else eligible.addAll(selectedTopicIds);
        eligible.removeIf(id->!progress.hasCompletedCourse(world,id));
        if(eligible.isEmpty()) {showNoQuiz();return;}
        Collections.shuffle(eligible);
        selectedTopicId=eligible.get(0);
        saveChoices();
        startQuiz();
    }

    private void startAnotherCourse() {
        if(!randomTopics&&selectedTopicIds.size()==1) {
            List<String> next=new ArrayList<>();
            for(QuestContent.Topic topic:content.topics)
                if(!topic.id.equals(selectedTopicId))next.add(topic.id);
            Collections.shuffle(next);
            selectedTopicId=next.get(0);
            selectedTopicIds.clear();selectedTopicIds.add(selectedTopicId);
            saveChoices();
        } else chooseSessionTopic(true);
        beginCourse();
    }

    private LinearLayout page() {
        statusView = null;
        captionView = null;
        ScrollView scroll = new ScrollView(this);
        currentScroll=scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(view,insets)->{
            Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|
                    WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            return insets;
        });
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(26), dp(22), dp(30));
        int width=getResources().getConfiguration().screenWidthDp>720?dp(720):-1;
        ScrollView.LayoutParams bodyLp=new ScrollView.LayoutParams(width,-2);
        bodyLp.gravity=Gravity.CENTER_HORIZONTAL;
        scroll.addView(body,bodyLp);
        setContentView(scroll);
        ViewCompat.requestApplyInsets(scroll);
        return body;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(2),1);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addText(LinearLayout parent, String value, int size, int color, boolean bold, int top) {
        TextView view = text(value,size,color,bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin = dp(top);
        parent.addView(view,lp);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout card(LinearLayout parent, int color, int top) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18),dp(18),dp(18),dp(18));
        box.setBackground(shape(color,22));
        box.setElevation(dp(3));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin = dp(top);
        parent.addView(box,lp);
        return box;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? CARD : primaryColor());
        b.setBackground(shape(primary ? primaryColor() : softColor(),15));
        b.setElevation(dp(2));
        b.setMinHeight(dp(51));
        b.setPadding(dp(12),dp(8),dp(12),dp(8));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void addButton(LinearLayout parent, String label, boolean primary, int top, Runnable action) {
        Button b = button(label,primary,action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(53));
        lp.topMargin = dp(top);
        parent.addView(b,lp);
    }

    private void addAudioControls(LinearLayout parent,Runnable replay,Runnable pause) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,dp(52));
        rowLp.topMargin=dp(11);
        parent.addView(row,rowLp);
        addIconControl(row,"↻","Réécouter",replay);
        if(pause!=null)addIconControl(row,paused?"▶":"Ⅱ",paused?"Reprendre":"Mettre en pause",pause);
    }

    private void addIconControl(LinearLayout row,String symbol,String label,Runnable action) {
        TextView control=text(symbol,26,primaryColor(),true);
        control.setGravity(Gravity.CENTER);
        control.setContentDescription(label);
        control.setClickable(true);
        control.setFocusable(true);
        control.setBackground(shape(CARD,17));
        control.setElevation(dp(2));
        control.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(54),dp(48));
        lp.leftMargin=dp(9);row.addView(control,lp);
    }

    private void addHomeFooter(LinearLayout parent) {
        TextView home=text("⌂  Accueil",14,MUTED,false);
        home.setGravity(Gravity.CENTER);
        home.setPadding(dp(12),dp(10),dp(12),dp(10));
        home.setClickable(true);
        home.setFocusable(true);
        home.setOnClickListener(v->{cancelCurrent();paused=false;showHome();});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(45));
        lp.topMargin=dp(22);parent.addView(home,lp);
    }

    private void brand(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_app);
        row.addView(mark,new LinearLayout.LayoutParams(dp(40),dp(40)));
        TextView name = text("Hello Quest",20,INK,true);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(-2,-2);
        nameLp.leftMargin=dp(10);
        row.addView(name,nameLp);
        parent.addView(row);
    }

    private void addWorldArt(LinearLayout parent, int height, int top, boolean animate) {
        WorldArtView art = new WorldArtView(this,world);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(height));
        lp.topMargin = dp(top);
        parent.addView(art,lp);
        if (animate) {
            art.setAlpha(0f);
            art.setTranslationY(dp(8));
            art.animate().alpha(1f).translationY(0f).setDuration(450).start();
        }
    }

    private void addCaption(LinearLayout parent, String initial) {
        if(advancedMode) {
            addText(parent,"ÉCOUTE · SANS INDICE ÉCRIT",11,primaryColor(),true,14);
            TextView hidden=text("👂 Écoute bien la phrase",20,INK,true);
            hidden.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hiddenLp=new LinearLayout.LayoutParams(-1,-2);
            hiddenLp.topMargin=dp(9);
            parent.addView(hidden,hiddenLp);
            captionView=null;
            return;
        }
        addText(parent,"ENGLISH · ÉCOUTE ET LIS",11,primaryColor(),true,14);
        captionView = text(initial.isEmpty()?"Écoute la phrase…":initial,24,INK,true);
        captionView.setGravity(Gravity.CENTER);
        captionView.setTextIsSelectable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin = dp(9);
        parent.addView(captionView,lp);
    }

    private void updateStatus(String value) {
        status = value;
        if (statusView != null) statusView.setText(value);
    }

    private void playSpoken(List<QuestContent.Segment> segments, boolean slow, Runnable done) {
        // The caption describes the full teaching sentence, not an individual audio clip.
        if(PlaybackCoordinator.carActive())return;
        audio.play(segments,accent,slow,done);
    }

    private void playSpoken(String value, String locale, boolean slow, Runnable done) {
        List<QuestContent.Segment> segments = new ArrayList<>();
        segments.add(new QuestContent.Segment(locale,value));
        playSpoken(segments,slow,done);
    }

    private void showHome() {
        screen = "home";
        LinearLayout page = page();
        brand(page);
        addText(page,"APPRENDS L’ANGLAIS EN T’AMUSANT",11,primaryColor(),true,22);
        addText(page,"Prêt pour une nouvelle\naventure ?",29,INK,true,5);
        addWorldArt(page,145,17,true);
        LinearLayout selection=card(page,CARD,14);
        selection.setOnClickListener(v->showSetup());
        selection.setClickable(true);
        selection.setFocusable(true);
        selection.setContentDescription("Changer l’univers et le sujet d’apprentissage");
        addText(selection,"TON AVENTURE  ·  MODIFIER ›",11,primaryColor(),true,0);
        addText(selection,worldLabel()+"  ·  "+topicSelectionLabel(),18,INK,true,6);
        if(!"all".equals(world)&&!randomTopics&&selectedTopicIds.size()==1)addText(selection,
                topicStateLabel(progress.topicState(world,selectedTopicId)),12,MUTED,false,4);
        LinearLayout progressRow=new LinearLayout(this);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams progressLp=new LinearLayout.LayoutParams(-1,-2);
        progressLp.topMargin=dp(22);page.addView(progressRow,progressLp);
        TextView choose=text("À TOI DE CHOISIR",12,primaryColor(),true);
        progressRow.addView(choose,new LinearLayout.LayoutParams(0,-2,1));
        TextView xp=text("✦  Niveau "+progress.level()+" · "+progress.xp()+" pts",12,INK,true);
        xp.setPadding(dp(10),dp(6),dp(10),dp(6));xp.setBackground(shape(0xffe4e1ff,14));
        xp.setOnClickListener(v->showProgress());
        progressRow.addView(xp);
        addHomeAction(page,"01","📖","Leçons","Regarde, écoute et découvre les mots.",GOLD,this::requestStart);
        addHomeAction(page,"02","🎙","Exercices","Parle, écoute et essaie à ton rythme.",0xffe4e1ff,this::startSelectedExercises);
        addHomeAction(page,"03","🏆","Quiz","Teste tes acquis et gagne un diplôme.",MINT,this::startSelectedQuiz);
        LinearLayout links=new LinearLayout(this);
        links.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams linksLp=new LinearLayout.LayoutParams(-1,-2);
        linksLp.topMargin=dp(13);page.addView(links,linksLp);
        addHomeLink(links,"📚  Mes sujets",this::showTopics);
        addHomeLink(links,"⚙  Réglages",this::showSettings);
    }

    private void addHomeAction(LinearLayout parent,String step,String symbol,String title,String detail,
                               int color,Runnable action) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14),dp(13),dp(14),dp(13));
        row.setBackground(shape(color,22));row.setElevation(dp(3));
        row.setClickable(true);row.setFocusable(true);
        row.setContentDescription(title+" : "+detail);
        row.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(91));
        lp.topMargin=dp(11);parent.addView(row,lp);
        TextView icon=text(symbol,31,INK,true);
        icon.setGravity(Gravity.CENTER);icon.setBackground(shape(CARD,17));
        row.addView(icon,new LinearLayout.LayoutParams(dp(57),dp(57)));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyLp=new LinearLayout.LayoutParams(0,-2,1);
        copyLp.leftMargin=dp(14);row.addView(copy,copyLp);
        addText(copy,step+"  "+title,20,INK,true,0);
        addText(copy,detail,13,MUTED,false,3);
        TextView arrow=text("›",30,primaryColor(),true);arrow.setGravity(Gravity.CENTER);
        row.addView(arrow,new LinearLayout.LayoutParams(dp(25),-2));
    }

    private void addHomeLink(LinearLayout parent,String label,Runnable action) {
        TextView link=text(label,14,primaryColor(),true);
        link.setGravity(Gravity.CENTER);link.setPadding(dp(8),dp(11),dp(8),dp(11));
        link.setOnClickListener(v->action.run());link.setClickable(true);link.setFocusable(true);
        parent.addView(link,new LinearLayout.LayoutParams(0,-2,1));
    }

    private void addScoreBoard(LinearLayout parent,int top) {
        LinearLayout board=card(parent,0xffe4e1ff,top);
        addText(board,"🌟  NIVEAU "+progress.level()+"   ·   "+progress.xp()+" POINTS",18,INK,true,0);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);bar.setProgress(progress.levelProgress());
        bar.setProgressTintList(ColorStateList.valueOf(primaryColor()));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(8));
        lp.topMargin=dp(13);board.addView(bar,lp);
        addText(board,progress.levelProgress()+" / 100 vers le prochain niveau  ·  "+
                progress.uniqueQuestions()+" / "+progress.totalQuestions()+" questions découvertes",12,MUTED,false,10);
    }

    private void showSettings() {
        if(!"settings".equals(screen))settingsFrom=screen;
        screen="settings";
        LinearLayout page=page();
        if("setup".equals(settingsFrom))
            addButton(page,"← Préparer l’aventure",false,9,this::showSetup);
        addText(page,"Réglages",29,INK,true,27);
        addText(page,"Temps de réflexion",18,INK,true,22);
        addText(page,"Après chaque question, prends ce temps pour chercher ta réponse. L’application te dira ensuite quand parler.",14,MUTED,false,8);
        addSettingSlider(page,5,20,thinkSeconds," secondes",true);
        addText(page,"Questions par sujet",18,INK,true,24);
        addText(page,"Choisis la longueur des exercices. Le grand mélange peut parcourir plusieurs sujets.",14,MUTED,false,8);
        addSettingSlider(page,2,12,questionsPerTheme," questions",false);
        addText(page,"Accent anglais",18,INK,true,24);
        addText(page,"Choisis la prononciation des mots anglais. Les consignes restent en français.",14,MUTED,false,8);
        addButton(page,(accent.equals("en-GB")?"✓  ":"   ")+"Britannique",false,8,()->{
            accent="en-GB";saveChoices();showSettings();
        });
        addButton(page,(accent.equals("en-US")?"✓  ":"   ")+"Américain",false,8,()->{
            accent="en-US";saveChoices();showSettings();
        });
        addText(page,"Mode avancé",18,INK,true,24);
        addText(page,"L’enfant écoute l’anglais sans le lire. La traduction française reste visible pendant les exercices oraux.",14,MUTED,false,8);
        addButton(page,(advancedMode?"✓  ":"   ")+"Masquer l’anglais écrit",false,11,()->{
            advancedMode=!advancedMode;
            prefs.edit().putBoolean("advanced_mode",advancedMode).apply();
            showSettings();
        });
        addText(page,"Ce réglage s’applique aux séances sur le téléphone. Les pistes Android Auto gardent une pause audio fixe pour répondre.",12,MUTED,false,18);
        addText(page,"Options de connexion",18,INK,true,25);
        addButton(page,"🚗 Android Auto · aide et test",false,10,this::showAutoHelp);
        addHomeFooter(page);
    }

    private void addSettingSlider(LinearLayout page,int min,int max,int current,
                                  String suffix,boolean thinking) {
        TextView value=text(current+suffix,17,primaryColor(),true);
        LinearLayout.LayoutParams labelLp=new LinearLayout.LayoutParams(-1,-2);
        labelLp.topMargin=dp(11);page.addView(value,labelLp);
        SeekBar slider=new SeekBar(this);
        slider.setMin(min);slider.setMax(max);slider.setProgress(current);
        slider.setProgressTintList(ColorStateList.valueOf(primaryColor()));
        slider.setThumbTintList(ColorStateList.valueOf(primaryColor()));
        slider.setContentDescription(thinking?"Temps de réflexion":"Questions par sujet");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser) {
                value.setText(progress+suffix);
                if(!fromUser)return;
                if(thinking) {
                    thinkSeconds=progress;prefs.edit().putInt("think_seconds",progress).apply();
                } else {
                    questionsPerTheme=progress;
                    prefs.edit().putInt("questions_per_theme",progress).apply();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        page.addView(slider,new LinearLayout.LayoutParams(-1,dp(50)));
    }

    private void showSetup() {
        screen = "setup";
        LinearLayout page=page();
        addText(page,"Préparer l’aventure",28,INK,true,26);
        addText(page,"Choisis un univers pour l’histoire et un sujet à apprendre. Leur association forme une leçon.",14,MUTED,false,9);
        addText(page,"Univers à débloquer",17,INK,true,24);
        addText(page,"Réussis des questions et termine des leçons pour ouvrir les univers suivants.",13,MUTED,false,7);
        for (QuestContent.Universe mission : content.universes) {
            boolean selected=world.equals(mission.universeId);
            int goal=progress.worldUnlockGoal(mission.universeId);
            if(progress.worldUnlocked(mission.universeId))
                addButton(page,(selected?"✓  ":"   ")+mission.label,false,8,() -> {
                    world=mission.universeId; saveChoices(); showSetup();
                });
            else {
                addLockedButton(page,"🔒 "+mission.label,8);
                addText(page,progress.missions()+"/"+progress.worldUnlockMissions(mission.universeId)+
                        " séances · "+progress.masteredQuestions()+"/"+goal+
                        " questions réussies",12,MUTED,false,5);
            }
        }
        if(progress.worldUnlocked("all"))
            addButton(page,("all".equals(world)?"✓  ":"   ")+"🎲 Tous les thèmes",false,8,()->{
                world="all";saveChoices();showSetup();
            });
        else {
            addLockedButton(page,"🔒 Tous les thèmes",8);
            addText(page,progress.missions()+"/"+progress.worldUnlockMissions("all")+
                    " séances · "+progress.masteredQuestions()+"/"+progress.worldUnlockGoal("all")+
                    " questions réussies",12,MUTED,false,5);
        }
        addText(page,"Sujet d’apprentissage",17,INK,true,23);
        addText(page,topicSelectionLabel()+" · une leçon traite un sujet à la fois.",
                14,MUTED,false,8);
        addButton(page,"Choisir un ou plusieurs des "+content.topics.size()+" sujets",false,11,this::showTopics);
        addText(page,questionsPerTheme+" exercices maximum par sujet. Les notions déjà maîtrisées reviennent moins souvent.",12,MUTED,false,9);
        addButton(page,"Réglages · "+questionsPerTheme+" questions/sujet · "+thinkSeconds+" s",false,12,this::showSettings);
        addButton(page,"📚 Commencer la leçon",true,23,this::requestStart);
        addButton(page,"🎙️ Faire les exercices",false,10,this::startSelectedExercises);
        addButton(page,"🎧 Passer le quiz",false,10,this::startSelectedQuiz);
        addHomeFooter(page);
    }

    private String topicStateLabel(int state) {
        if(state==2)return "✅ Validé";
        if(state==1)return "👀 Vu";
        return "○ Non vu";
    }

    private void showTopics() {
        screen="topics";
        LinearLayout page=page();
        addText(page,"Mes sujets",29,INK,true,26);
        addText(page,"Coche plusieurs sujets pour varier les leçons. Aléatoire pioche parmi les "+
                content.topics.size()+" sujets. Une leçon terminée marque son sujet Vu ; un quiz réussi le valide.",
                14,MUTED,false,9);
        addButton(page,"← Préparer l’aventure",false,15,this::showSetup);
        addButton(page,(randomTopics?"✓  ":"   ")+"🎲 Aléatoire · tous les sujets",false,14,()->{
            randomTopics=true;saveChoices();refreshTopics();
        });
        for(QuestContent.Topic topic:content.topics) {
            int state=progress.topicState(world,topic.id);
            String label=(!randomTopics&&selectedTopicIds.contains(topic.id)?"☑  ":"□  ")+
                    topic.label+"  ·  "+topicStateLabel(state);
            Button entry=button(label,false,()->{
                if(randomTopics) {
                    randomTopics=false;selectedTopicIds.clear();selectedTopicIds.add(topic.id);
                } else if(selectedTopicIds.contains(topic.id)) {
                    if(selectedTopicIds.size()>1)selectedTopicIds.remove(topic.id);
                } else selectedTopicIds.add(topic.id);
                selectedTopicId=selectedTopicIds.iterator().next();
                saveChoices();refreshTopics();
            });
            entry.setTextSize(14);
            entry.setMaxLines(2);
            entry.setTextColor(INK);
            entry.setBackground(shape(state==2?MINT:state==1?GOLD:CARD,15));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));
            lp.topMargin=dp(8);page.addView(entry,lp);
        }
        addButton(page,"Continuer avec "+topicSelectionLabel(),true,18,this::showSetup);
        addHomeFooter(page);
    }

    private void refreshTopics() {
        int y=currentScroll.getScrollY();
        showTopics();
        ScrollView refreshed=currentScroll;
        refreshed.post(()->refreshed.scrollTo(0,y));
    }

    private void addLockedButton(LinearLayout parent,String label,int top) {
        Button locked=button(label,false,()->{});
        locked.setEnabled(false);locked.setAlpha(0.65f);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(53));
        lp.topMargin=dp(top);parent.addView(locked,lp);
    }

    private String worldLabel() {return "all".equals(world)?"Tous les thèmes":content.universe(world).label;}
    private String worldTitle() {return "all".equals(world)?"La grande aventure":content.universe(world).title;}

    private void requestStart() {
        chooseSessionTopic(false);
        beginCourse();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==REQUEST_MIC) {
            listenOnly=results.length==0 || results[0]!=PackageManager.PERMISSION_GRANTED;
            if(listenOnly) Toast.makeText(this,"Mode écoute : réponses non évaluées.",Toast.LENGTH_LONG).show();
            if("lesson".equals(screen)) {paused=false;waitForAnswer();}
        }
    }

    private static final class LessonItem {
        final QuestContent.Universe mission;
        final QuestContent.Lesson chapter;
        final QuestContent.Exercise exercise;
        ChoiceQuestion choice;
        boolean multipleChoice;
        LessonItem(QuestContent.Universe mission,QuestContent.Lesson chapter,
                   QuestContent.Exercise exercise) {
            this.mission=mission;this.chapter=chapter;this.exercise=exercise;
        }
    }

    private static final class ChoiceQuestion {
        final String answer;
        final String spoken;
        final List<String> choices;
        final boolean wordOnly;
        ChoiceQuestion(String answer,String spoken,List<String> choices,boolean wordOnly) {
            this.answer=answer;this.spoken=spoken;this.choices=choices;
            this.wordOnly=wordOnly;
        }
    }

    private ChoiceQuestion createChoice(LessonItem item) {
        QuestContent.Exercise exercise=item.exercise;
        String model=normalize(exercise.model);
        QuestContent.TheoryCard match=null;
        for(QuestContent.TheoryCard card:item.chapter.theory)
            if(card.stage==exercise.stage&&normalize(card.example).equals(model)) {
                match=card;break;
            }
        if(match==null)for(QuestContent.TheoryCard card:item.chapter.theory)
            if(card.stage==exercise.stage&&normalize(card.word).equals(model)) {
                match=card;break;
            }
        if(match==null)return null;
        boolean wordOnly=normalize(match.word).equals(model);
        String answer=wordOnly?match.meaning:match.translation;
        LinkedHashSet<String> possible=new LinkedHashSet<>();
        for(QuestContent.TheoryCard card:item.chapter.theory)
            if(card!=match)possible.add(wordOnly?card.meaning:card.translation);
        possible.remove(answer);
        if(possible.size()<2)return null;
        List<String> distractors=new ArrayList<>(possible);
        Collections.shuffle(distractors);
        List<String> choices=new ArrayList<>();
        choices.add(answer);choices.add(distractors.get(0));choices.add(distractors.get(1));
        Collections.shuffle(choices);
        String spoken="en".equals(exercise.questionLocale)?exercise.question:exercise.model;
        return new ChoiceQuestion(answer,spoken,choices,wordOnly);
    }

    private QuestContent.TheoryCard visualFor(LessonItem item) {
        String model=normalize(item.exercise.model);
        for(QuestContent.TheoryCard card:item.chapter.theory)
            if(normalize(card.example).equals(model)||normalize(card.word).equals(model))return card;
        for(QuestContent.TheoryCard card:item.chapter.theory)
            if(card.stage==item.exercise.stage)return card;
        return item.chapter.theory.get(0);
    }

    private void addPhoto(LinearLayout page,QuestContent.TheoryCard card,int height,int top) {
        LessonVisualView photo=new LessonVisualView(this,card.word,card.meaning,card.visual);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(height));
        lp.topMargin=dp(top);
        page.addView(photo,lp);
    }

    private void assignExerciseTypes() {
        Map<String,List<LessonItem>> bySkill=new LinkedHashMap<>();
        for(LessonItem item:lesson) {
            if(!"repeat".equals(item.exercise.type))continue;
            item.choice=createChoice(item);
            if(item.choice!=null)
                bySkill.computeIfAbsent(item.chapter.topicId,k->new ArrayList<>()).add(item);
        }
        for(List<LessonItem> choices:bySkill.values()) {
            Collections.shuffle(choices);
            int count=Math.min(choices.size(),Math.max(1,(choices.size()+1)/2));
            for(int i=0;i<count;i++)choices.get(i).multipleChoice=true;
        }
    }

    private void beginCourse() {
        PlaybackCoordinator.startPhone();
        cancelCurrent();paused=false;theoryIndex=0;
        theoryMission="all".equals(world)?content.universes.get(
                new java.util.Random().nextInt(content.universes.size())):content.universe(world);
        theoryChapter=theoryMission.lesson(selectedTopicId);
        theoryCards.clear();theoryCards.addAll(theoryChapter.theory);
        screen="course-overview";spokenEnglish="";
        showCourseOverview();
        playSpoken(theoryChapter.intro,false,()->{});
    }

    private void showCourseOverview() {
        screen="course-overview";
        LinearLayout page=page();
        addText(page,"📚 LEÇON · "+theoryMission.label.toUpperCase(Locale.ROOT),12,primaryColor(),true,20);
        addText(page,content.topic(theoryChapter.topicId).label,29,INK,true,8);
        QuestContent.TheoryCard cover=theoryCards.get(0);
        LessonVisualView visual=new LessonVisualView(this,cover.word,cover.meaning,cover.visual);
        LinearLayout.LayoutParams visualLp=new LinearLayout.LayoutParams(-1,dp(145));
        visualLp.topMargin=dp(20);page.addView(visual,visualLp);
        LinearLayout overview=card(page,CARD,18);
        addText(overview,"Comprendre le sujet",21,INK,true,0);
        for(QuestContent.Segment segment:theoryChapter.intro)
            if("fr-FR".equals(segment.locale))addText(overview,segment.text,17,INK,false,10);
        addText(overview,"Mots et expressions à découvrir",16,primaryColor(),true,20);
        StringBuilder words=new StringBuilder();
        for(int i=0;i<Math.min(8,theoryCards.size());i++) {
            QuestContent.TheoryCard card=theoryCards.get(i);
            if(i>0)words.append("  ·  ");
            words.append(card.word).append(" = ").append(card.meaning);
        }
        addText(overview,words.toString(),15,INK,false,9);
        if(theoryCards.size()>8)addText(overview,"Et "+(theoryCards.size()-8)+
                " autres mots, découverts un par un.",13,MUTED,false,8);
        addButton(page,"Découvrir les "+theoryCards.size()+" cartes",true,23,()->{
            cancelCurrent();playTheoryCard();
        });
        addHomeFooter(page);
    }

    private void finishCourse() {
        progress.recordCourse(theoryMission.universeId,theoryChapter.topicId);
        screen="course-done";cancelCurrent();
        LinearLayout page=page();
        addText(page,"📚 LEÇON TERMINÉE",13,primaryColor(),true,24);
        addText(page,"Sujet découvert !",30,INK,true,10);
        addText(page,content.topic(theoryChapter.topicId).label+" passe à « Vu » dans "+
                theoryMission.label+".",17,INK,false,12);
        addText(page,"Tu connais maintenant "+theoryCards.size()+
                " mots et exemples. Entraîne-toi à les reconnaître et à les prononcer.",15,MUTED,false,12);
        addButton(page,"🎙️ Passer aux exercices",true,24,()->{
            if("all".equals(world)) {world=theoryMission.universeId;saveChoices();}
            beginExercises();
        });
        addButton(page,"📚 Découvrir une autre leçon",false,10,this::startAnotherCourse);
        addButton(page,"Revoir la leçon",false,10,this::beginCourse);
        addHomeFooter(page);
    }

    private void beginExercises() {
        if(!"all".equals(world)&&!progress.hasCompletedCourse(world,selectedTopicId)) {
            showNoQuestions();return;
        }
        PlaybackCoordinator.startPhone();
        cancelCurrent();
        lesson.clear();step=0;retries=0;paused=false;betweenQuestions=false;canSpeakNow=false;
        lessonSelectedChoice=-1;
        missionXp=0;missionCorrect=0;missionNew=0;questionRecorded=false;
        activeDialogue=null;dialogueMission=null;dialogueChallengeShown=false;
        dialogueAnswered=false;dialogueCorrect=false;dialogueSelectedChoice=-1;
        activePassage=null;passageMission=null;passageShown=false;
        passageAnswered=false;passageSelectedChoice=-1;readingSentence=-1;
        earnedBefore.clear();newBadges.clear();
        unlockedBefore.clear();newWorlds.clear();
        for(QuestContent.Universe mission:content.universes)
            if(progress.worldUnlocked(mission.universeId))unlockedBefore.add(mission.universeId);
        if(progress.worldUnlocked("all"))unlockedBefore.add("all");
        for(LearningProgress.Badge badge:progress.badges())
            if(badge.earned())earnedBefore.add(badge.title);
        listenOnly=false;
        spokenEnglish="";
        LinkedHashSet<String> usedConcepts=new LinkedHashSet<>();
        if("all".equals(world)) {
            int offset=(prefs.getInt("missions_all",0)*4)%content.universes.size();
            List<QuestContent.Topic> mixedTopics=new ArrayList<>(content.topics);
            Collections.shuffle(mixedTopics);
            int selected=0;
            for(QuestContent.Topic topic:mixedTopics) {
                if(selected>=4)break;
                for(int candidate=0;candidate<content.universes.size();candidate++) {
                    QuestContent.Universe mission=content.universes.get(
                            (offset+selected+candidate)%content.universes.size());
                    QuestContent.Lesson chapter=mission.lesson(topic.id);
                    if(progress.hasCompletedCourse(mission.universeId,topic.id)&&
                            !chapter.exercises.isEmpty()) {
                        addChapterQuestions(mission,chapter,usedConcepts);
                        selected++;
                        break;
                    }
                }
            }
        } else {
            QuestContent.Universe universe=content.universe(world);
            addChapterQuestions(universe,universe.lesson(selectedTopicId),usedConcepts);
        }
        interleaveLesson();
        readingAtStep=lesson.size()>2?2+new java.util.Random().nextInt(lesson.size()-2):lesson.size();
        assignExerciseTypes();
        if(lesson.isEmpty()) {showNoQuestions();return;}
        screen="intro";
        status="Bienvenue dans les exercices. À toi de jouer !";
        showMissionIntro();
        if(!"all".equals(world)) {
            for(QuestContent.Segment segment:content.universe(world).intro)
                if("en".equals(segment.locale)){spokenEnglish=segment.text;break;}
            if(captionView!=null)captionView.setText(spokenEnglish);
            int id=++sequenceId;
            playSpoken(content.universe(world).intro,false,()->{if(id==sequenceId) startStep();});
        }
    }

    private void addChapterQuestions(QuestContent.Universe mission,QuestContent.Lesson chapter,
                                     LinkedHashSet<String> usedConcepts) {
        List<QuestContent.Exercise> chosen=progress.select(chapter,questionsPerTheme,usedConcepts);
        for(QuestContent.Exercise exercise:chosen) {
            lesson.add(new LessonItem(mission,chapter,exercise));
        }
    }

    private void interleaveLesson() {
        Map<String,List<LessonItem>> groups=new LinkedHashMap<>();
        for(LessonItem item:lesson)
            groups.computeIfAbsent(item.chapter.topicId,k->new ArrayList<>()).add(item);
        List<String> order=new ArrayList<>(groups.keySet());
        Collections.shuffle(order);
        lesson.clear();
        for(int row=0;row<questionsPerTheme;row++)
            for(String skill:order)
                if(row<groups.get(skill).size())lesson.add(groups.get(skill).get(row));
    }

    private void showNoQuestions() {
        screen="empty";
        LinearLayout page=page();
        brand(page);
        addWorldArt(page,125,22,true);
        addText(page,"PAS D’EXERCICES DISPONIBLES",11,primaryColor(),true,25);
        addText(page,"Commence par une leçon",26,INK,true,10);
        addText(page,"Les exercices s’ouvrent après une leçon terminée dans cet univers et ce sujet.",15,MUTED,false,12);
        addButton(page,"Suivre la leçon",true,24,()->{chooseSessionTopic(false);beginCourse();});
        addButton(page,"Choisir un sujet",false,10,this::showSetup);
    }

    private void showMissionIntro() {
        screen="intro";
        LinearLayout page=page();
        brand(page);
        addWorldArt(page,150,22,true);
        addText(page,"BIENVENUE DANS "+worldLabel().toUpperCase(Locale.ROOT),11,primaryColor(),true,25);
        addText(page,"all".equals(world)?worldTitle():
                content.topic(selectedTopicId).label+" · "+content.universe(world).lesson(selectedTopicId).title,
                27,INK,true,8);
        addText(page,"À toi de parler, d’écouter et de choisir les bonnes réponses.",16,MUTED,false,12);
        addText(page,"🎙️ Oral  ·  🧩 QCM  ·  🔀 Mots à ranger  ·  💬 Réponse libre",
                14,primaryColor(),true,15);
        LinearLayout box=card(page,softColor(),23);
        addCaption(box,spokenEnglish);
        addButton(page,"Commencer les exercices",true,24,()->{cancelCurrent();startStep();});
        addHomeFooter(page);
    }

    private void startStep() {
        if(step==readingAtStep&&!passageShown){startPassageOrDialogue();return;}
        if(step>=lesson.size()){startPassageOrDialogue();return;}
        presentQuestion();
    }

    private void playTheoryCard() {
        if(theoryChapter==null||theoryIndex>=theoryCards.size()) {finishCourse();return;}
        cancelCurrent();
        screen="theory";
        QuestContent.TheoryCard card=theoryCards.get(theoryIndex);
        spokenEnglish=card.example;
        status=advancedMode?"Écoute le mot et sa phrase d’exemple.":
                "Écoute le mot, puis lis la phrase anglaise.";
        showTheory();
        int id=++sequenceId;
        playSpoken(card.audio,false,()->{
            if(id!=sequenceId||paused||!"theory".equals(screen))return;
            updateStatus("Prends ton temps. Touche « notion suivante » quand tu es prêt.");
        });
    }

    private void nextTheoryCard() {
        if(!"theory".equals(screen))return;
        cancelCurrent();
        theoryIndex++;
        if(theoryIndex>=theoryCards.size())finishCourse();
        else playTheoryCard();
    }

    private void replayTheoryWord() {
        if(!"theory".equals(screen)||theoryIndex>=theoryCards.size())return;
        cancelCurrent();paused=false;
        QuestContent.TheoryCard card=theoryCards.get(theoryIndex);
        QuestContent.Segment word=null;
        for(QuestContent.Segment segment:card.audio)
            if("en".equals(segment.locale)) {word=segment;break;}
        if(word==null)word=new QuestContent.Segment("en",card.word+".");
        status="Réécoute seulement le mot.";
        showTheory();
        int id=++sequenceId;
        List<QuestContent.Segment> single=new ArrayList<>();single.add(word);
        playSpoken(single,false,()->{if(id==sequenceId&&!paused)
            updateStatus("Touche « notion suivante » quand tu es prêt.");});
    }

    private void showTheory() {
        screen="theory";
        QuestContent.TheoryCard item=theoryCards.get(theoryIndex);
        LinearLayout page=page();
        addText(page,"📚 LEÇON · "+content.topic(theoryChapter.topicId).label.toUpperCase(Locale.ROOT),11,primaryColor(),true,20);
        addText(page,"Un mot à la fois",27,INK,true,7);
        addText(page,"Carte "+(theoryIndex+1)+" sur "+theoryCards.size(),13,MUTED,false,8);
        LessonVisualView visual=new LessonVisualView(this,item.word,item.meaning,item.visual);
        LinearLayout.LayoutParams visualLp=new LinearLayout.LayoutParams(-1,
                dp(item.visual.startsWith("photo:")?205:150));
        visualLp.topMargin=dp(17);page.addView(visual,visualLp);
        visual.setAlpha(0f);visual.animate().alpha(1f).setDuration(320).start();
        LinearLayout box=card(page,CARD,20);
        addText(box,advancedMode?"Mot à écouter":item.word,advancedMode?26:38,primaryColor(),true,0);
        addText(box,(advancedMode?"Sens : ":"= ")+item.meaning,21,INK,true,5);
        addText(box,advancedMode?"Écoute son explication et sa phrase d’exemple.":item.explanation,15,MUTED,false,15);
        addCaption(box,spokenEnglish.isEmpty()?item.example:spokenEnglish);
        addText(box,item.translation,15,MUTED,false,12);
        statusView=text(status,14,primaryColor(),true);
        LinearLayout.LayoutParams stateLp=new LinearLayout.LayoutParams(-1,-2);
        stateLp.topMargin=dp(19);page.addView(statusView,stateLp);
        addAudioControls(page,this::replayTheoryWord,()->{
            if(paused){paused=false;playTheoryCard();}
            else{cancelCurrent();paused=true;status="Cours en pause. Tu peux relire tranquillement.";showTheory();}
        });
        addButton(page,theoryIndex+1==theoryCards.size()?
                "Terminer la leçon":"J’ai compris · notion suivante",true,9,()->{
            paused=false;nextTheoryCard();
        });
        addHomeFooter(page);
    }

    private void presentQuestion() {
        if(paused||step>=lesson.size()) return;
        screen="lesson";
        cancelCurrent();
        LessonItem item=lesson.get(step);
        betweenQuestions=false;canSpeakNow=false;
        if(item.multipleChoice) {
            spokenEnglish=item.choice.spoken;
            status="Écoute, puis choisis la bonne réponse. Tu as tout ton temps.";
            showLesson();
            List<QuestContent.Segment> prompts=new ArrayList<>(content.shared("chooseTranslation"));
            prompts.add(new QuestContent.Segment("en",item.choice.spoken));
            playSpoken(prompts,false,()->{});
            return;
        }
        if("order".equals(item.exercise.type)) {
            prepareOrder(item.exercise.model);
            spokenEnglish="";
            status="Écoute la phrase, puis touche les mots dans le bon ordre.";
            showLesson();
            List<QuestContent.Segment> prompts=new ArrayList<>(item.exercise.questionAudio);
            playSpoken(prompts,false,()->{});
            return;
        }
        if("reply".equals(item.exercise.type)) {
            spokenEnglish="";
            status="Écoute la question. Cherche le mot anglais avant de parler.";
            recognizedText="";
            showLesson();
            int id=++sequenceId;
            playSpoken(item.exercise.questionAudio,false,
                    ()->{if(id==sequenceId)beginThinking();});
            return;
        }
        spokenEnglish="en".equals(item.exercise.questionLocale)?item.exercise.question:item.exercise.model;
        status="Écoute la consigne. Tu auras du temps pour réfléchir.";recognizedText="";
        showLesson();
        List<QuestContent.Segment> prompts=new ArrayList<>();
        prompts.addAll(item.exercise.contextAudio);
        prompts.addAll(item.exercise.questionAudio);
        int id=++sequenceId;
        playSpoken(prompts,false,()->{if(id==sequenceId) beginThinking();});
    }

    private void beginThinking() {
        if(!"lesson".equals(screen)||paused)return;
        canSpeakNow=true;
        status="Réfléchis. Je te dirai quand parler.";
        showLesson();
        int id=++sequenceId;
        playSpoken(content.shared("think"),false,()->{if(id==sequenceId)countdown(id,thinkSeconds);});
    }

    private void countdown(int id,int seconds) {
        if(id!=sequenceId||paused||!"lesson".equals(screen))return;
        if(seconds==0){readyToAnswer();return;}
        updateStatus("Réfléchis tranquillement : "+seconds+" secondes.");
        handler.postDelayed(()->countdown(id,seconds-1),1000);
    }

    private void readyToAnswer() {
        if(!"lesson".equals(screen)||paused)return;
        canSpeakNow=false;
        status="À toi de parler.";
        showLesson();
        int id=++sequenceId;
        playSpoken(content.shared("speakNow"),false,()->{if(id==sequenceId)waitForAnswer();});
    }

    private void waitForAnswer() {
        if(!"lesson".equals(screen)||paused) return;
        canSpeakNow=false;
        if(!listenOnly&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            updateStatus("Pour t’écouter, Hello Quest demande le microphone.");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);
            return;
        }
        if(listenOnly) {
            status="Réponds à voix haute. Le modèle arrive après une vraie pause.";
            showLesson();
            int id=++sequenceId;
            handler.postDelayed(()->{if(id==sequenceId) feedback("autoAnswer",true);},PASSIVE_ANSWER_MS);
            return;
        }
        if(!SpeechRecognizer.isRecognitionAvailable(this)) {
            listenOnly=true;waitForAnswer();return;
        }
        if(recognizer==null) {
            recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    micOpen=true;
                    updateStatus("Je t’écoute…");
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    micOpen=false;status="Je vérifie ta réponse…";showLesson();
                }
                @Override public void onError(int error) {
                    if(!listening) return;
                    listening=false;micOpen=false;
                    if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                            error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                            error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                            error==SpeechRecognizer.ERROR_NETWORK ||
                            error==SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                        listenOnly=true;
                        status="La reconnaissance est indisponible. On continue à écouter.";
                        showLesson();
                        waitForAnswer();
                    } else noAnswer();
                }
                @Override public void onResults(Bundle results) {
                    if(!listening) return;
                    listening=false;micOpen=false;
                    ArrayList<String> phrases=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(phrases==null||phrases.isEmpty()) {noAnswer();return;}
                    QuestContent.Exercise exercise=lesson.get(step).exercise;
                    recognizedText=phrases.get(0);
                    boolean correct=false;
                    for(String candidate:phrases) for(String accepted:exercise.answers)
                        if(normalize(candidate).equals(normalize(accepted)))correct=true;
                    if(correct) {
                        feedback("correct",true);
                    } else {
                        if(retries++==0) feedback("needsHelp",false);
                        else feedback("autoAnswer",true);
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType,Bundle params) {}
            });
        }
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,accent);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,accent);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,3200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,3000L);
        listening=true;micOpen=true;
        status="Je t’écoute…";showLesson();
        try {recognizer.startListening(intent);}
        catch(Exception e) {listening=false;micOpen=false;listenOnly=true;waitForAnswer();}
    }

    private void noAnswer() {
        if(retries++==0) feedback("unrecognized",false);
        else feedback("autoAnswer",true);
    }

    private static String normalize(String text) {
        String simple=Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('’','\'').replace('‘','\'');
        return simple.replaceAll("[.!?]+$", "").trim().replaceAll("\\s+", " ");
    }

    private void prepareOrder(String sentence) {
        orderTiles.clear();orderChosen.clear();orderWrong=false;
        String plain=sentence.trim().replaceAll("[.!?]+$", "");
        Collections.addAll(orderTiles,plain.split("\\s+"));
        List<String> original=new ArrayList<>(orderTiles);
        for(int attempt=0;attempt<8&&orderTiles.equals(original);attempt++)
            Collections.shuffle(orderTiles);
        if(orderTiles.equals(original))Collections.rotate(orderTiles,1);
    }

    private void chooseOrderTile(int index) {
        if(!"lesson".equals(screen)||paused||questionRecorded||orderChosen.contains(index))return;
        orderChosen.add(index);orderWrong=false;
        if(orderChosen.size()==orderTiles.size()) {
            List<String> answer=new ArrayList<>();
            for(int chosen:orderChosen)answer.add(orderTiles.get(chosen));
            String actual=normalize(String.join(" ",answer));
            String expected=normalize(lesson.get(step).exercise.model);
            if(actual.equals(expected)) {cancelCurrent();feedback("correct",true);return;}
            orderWrong=true;
            status="Ce n’est pas le bon ordre. Réessaie en écoutant la phrase.";
            renderOrderChoice();
            playSpoken("No, it's incorrect.","en",false,()->{});
            return;
        }
        renderOrderChoice();
    }

    private void renderOrderChoice() {
        if(orderAnswerView==null||orderErrorView==null||orderAnswerBox==null)return;
        List<String> placed=new ArrayList<>();
        for(int chosen:orderChosen)placed.add(orderTiles.get(chosen));
        orderAnswerView.setText(placed.isEmpty()?"Touche les mots ci-dessous…":
                String.join(" ",placed));
        orderAnswerBox.setBackground(shape(orderWrong?RED:LIGHT,22));
        orderErrorView.setText("❌ L’ordre n’est pas correct. Efface et essaie encore.");
        orderErrorView.setVisibility(orderWrong?View.VISIBLE:View.INVISIBLE);
        for(int i=0;i<orderTileButtons.size();i++)
            orderTileButtons.get(i).setEnabled(!orderChosen.contains(i)&&!orderWrong);
        updateStatus(status);
    }

    private void feedback(String type,boolean advance) {
        if(!"lesson".equals(screen)||paused||step>=lesson.size()) return;
        if(advance && questionRecorded)return;
        listening=false;micOpen=false;betweenQuestions=false;canSpeakNow=false;
        QuestContent.Exercise exercise=lesson.get(step).exercise;
        int earned=0;
        if(advance) {
            LearningProgress.Reward reward=progress.record(exercise,"correct".equals(type));
            questionRecorded=true;
            earned=reward.xp;
            missionXp+=reward.xp;
            if(reward.newQuestion)missionNew++;
            if("correct".equals(type))missionCorrect++;
        }
        spokenEnglish="reply".equals(exercise.type)&&type.equals("unrecognized")?"":exercise.model;
        if(type.equals("correct"))status="Bravo, tu as trouvé !";
        else if(type.equals("unrecognized"))status="Je n’ai pas bien entendu. On réessaie.";
        else if(type.equals("needsHelp"))status="Écoute l’indice, puis réessaie.";
        else if(type.equals("incorrectChoice"))status="Ce n’est pas la bonne réponse. La réponse verte est la bonne.";
        else status="Écoute la réponse modèle. Aucun point pour une réponse dévoilée.";
        if(advance&&earned>0)status+="  🌟 +"+earned+" points";
        showLesson();
        boolean choice=lesson.get(step).multipleChoice;
        List<QuestContent.Segment> reply=new ArrayList<>();
        if(choice&&(type.equals("correct")||type.equals("incorrectChoice")))
            reply.add(new QuestContent.Segment("en",type.equals("correct")?
                    "Good job!":"No, it's incorrect."));
        else reply.addAll(content.shared(type.equals("incorrectChoice")?"autoAnswer":type));
        if(type.equals("needsHelp"))reply.addAll(exercise.hint);
        if(!type.equals("unrecognized"))reply.add(new QuestContent.Segment("en",exercise.model));
        int id=++sequenceId;
        playSpoken(reply,false,()->{
            if(id!=sequenceId||paused) return;
            if(advance) {
                betweenQuestions=true;
                status="On respire un instant avant la suite.";
                showLesson();
                int pauseId=++sequenceId;
                playSpoken(content.shared("pause"),false,()->{
                    if(pauseId==sequenceId)
                        handler.postDelayed(()->{if(pauseId==sequenceId)nextQuestion();},QUESTION_BREAK_MS);
                });
            } else presentRetry();
        });
    }

    private void answerLessonChoice(int choice) {
        if(!"lesson".equals(screen)||questionRecorded||paused)return;
        cancelCurrent();
        lessonSelectedChoice=choice;
        LessonItem item=lesson.get(step);
        feedback(item.choice.choices.get(choice).equals(item.choice.answer)?"correct":"incorrectChoice",true);
    }

    private void presentRetry() {
        QuestContent.Exercise e=lesson.get(step).exercise;
        status="À toi d’essayer encore.";showLesson();
        int id=++sequenceId;
        playSpoken(e.questionAudio,false,()->{if(id==sequenceId)beginThinking();});
    }

    private void nextQuestion() {
        cancelCurrent();
        step++;retries=0;questionRecorded=false;canSpeakNow=false;lessonSelectedChoice=-1;
        if(step>=lesson.size())startPassageOrDialogue();
        else startStep();
    }

    private void startPassageOrDialogue() {
        if(passageShown){startDialogueOrFinish();return;}
        passageShown=true;
        List<QuestContent.ReadingPassage> candidates=new ArrayList<>();
        List<QuestContent.Universe> owners=new ArrayList<>();
        for(LessonItem item:lesson)
            for(QuestContent.ReadingPassage passage:item.mission.passages)
                if(passage.topicId.equals(item.chapter.topicId)&&!candidates.contains(passage)) {
                    candidates.add(passage);owners.add(item.mission);
                }
        if(candidates.isEmpty()){
            if(step<lesson.size())presentQuestion();else startDialogueOrFinish();
            return;
        }
        List<Integer> order=new ArrayList<>();
        for(int i=0;i<candidates.size();i++)order.add(i);
        Collections.shuffle(order);
        order.sort((a,b)->Long.compare(progress.readingLast(candidates.get(a).id),
                progress.readingLast(candidates.get(b).id)));
        int chosen=order.get(0);
        activePassage=candidates.get(chosen);passageMission=owners.get(chosen);
        passageAnswered=false;passageSelectedChoice=-1;readingSentence=-1;
        showReading();playReading();
    }

    private void playReading() {
        if(!"reading".equals(screen)||activePassage==null||PlaybackCoordinator.carActive())return;
        cancelCurrent();
        List<QuestContent.Segment> sentences=new ArrayList<>();
        for(String sentence:activePassage.sentences)
            sentences.add(new QuestContent.Segment("en",sentence));
        final int[] current={0};
        audio.play(sentences,accent,false,segment->{
            readingSentence=current[0]++;
            updateReadingHighlight();
        },()->{readingSentence=-1;updateReadingHighlight();});
    }

    private void updateReadingHighlight() {
        for(int i=0;i<readingLines.size();i++) {
            TextView line=readingLines.get(i);
            line.setBackground(shape(i==readingSentence?GOLD:CARD,12));
            line.setTextColor(i==readingSentence?primaryColor():INK);
        }
    }

    private void answerReading(int choice) {
        if(!"reading".equals(screen)||passageAnswered)return;
        cancelCurrent();passageAnswered=true;passageSelectedChoice=choice;
        boolean correct=choice==activePassage.correctIndex;
        missionXp+=progress.recordReading(activePassage.id,correct);
        showReading();
        playSpoken(correct?"Good job!":"No, it's incorrect.","en",false,()->{});
    }

    private void showReading() {
        screen="reading";
        LinearLayout page=page();
        readingLines.clear();
        addText(page,"📖 LECTURE GUIDÉE · "+passageMission.label.toUpperCase(Locale.ROOT),
                11,primaryColor(),true,17);
        addText(page,activePassage.title,25,INK,true,7);
        addText(page,"Suis les phrases des yeux pendant que la voix anglaise les lit.",
                14,MUTED,false,8);
        addPhoto(page,passageMission.lesson(activePassage.topicId).theory.get(0),130,12);
        LinearLayout box=card(page,softColor(),18);
        for(String sentence:activePassage.sentences) {
            TextView line=text(sentence,19,INK,false);
            line.setPadding(dp(10),dp(7),dp(10),dp(7));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.topMargin=dp(5);box.addView(line,lp);readingLines.add(line);
        }
        updateReadingHighlight();
        addAudioControls(page,this::playReading,null);
        addText(page,activePassage.question,18,INK,true,13);
        for(int i=0;i<activePassage.choices.size();i++) {
            final int selected=i;
            Button option=button(activePassage.choices.get(i),false,()->answerReading(selected));
            option.setEnabled(!passageAnswered);
            if(passageAnswered&&i==activePassage.correctIndex)option.setBackground(shape(MINT,15));
            else if(passageAnswered&&i==passageSelectedChoice)option.setBackground(shape(RED,15));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));
            lp.topMargin=dp(9);page.addView(option,lp);
        }
        if(passageAnswered) {
            addText(page,passageSelectedChoice==activePassage.correctIndex?
                    "Bravo ! 🌟 +8 points":"❌ Ce n’est pas la bonne réponse. La réponse verte est : « "+
                    activePassage.choices.get(activePassage.correctIndex)+" ».",
                    16,primaryColor(),true,16);
            addButton(page,"Continuer l’aventure",true,16,()->{
                cancelCurrent();
                if(step<lesson.size())startStep();else startDialogueOrFinish();
            });
        }
        addHomeFooter(page);
    }

    private void startDialogueOrFinish() {
        if(dialogueChallengeShown){finishLesson();return;}
        List<QuestContent.MiniDialogue> candidates=new ArrayList<>();
        List<QuestContent.Universe> owners=new ArrayList<>();
        for(LessonItem item:lesson)
            for(QuestContent.MiniDialogue dialogue:item.mission.dialogues)
                if(dialogue.topicId.equals(item.chapter.topicId)&&dialogue.stage==item.exercise.stage&&
                        !candidates.contains(dialogue)) {
                    candidates.add(dialogue);owners.add(item.mission);
                }
        if(candidates.isEmpty()){finishLesson();return;}
        List<Integer> order=new ArrayList<>();
        for(int i=0;i<candidates.size();i++)order.add(i);
        Collections.shuffle(order);
        order.sort((a,b)->Long.compare(progress.dialogueLast(candidates.get(a).id),
                progress.dialogueLast(candidates.get(b).id)));
        int chosen=order.get(0);
        activeDialogue=candidates.get(chosen);dialogueMission=owners.get(chosen);
        dialogueChallengeShown=true;dialogueAnswered=false;
        spokenEnglish=String.join("\n",activeDialogue.lines.stream().map(line->line.text)
                .toArray(String[]::new));
        showDialogue();playDialogue();
    }

    private void playDialogue() {
        if(!"dialogue".equals(screen)||activeDialogue==null)return;
        cancelCurrent();
        spokenEnglish=String.join("\n",activeDialogue.lines.stream().map(line->line.text)
                .toArray(String[]::new));
        if(captionView!=null)captionView.setText(spokenEnglish);
        List<QuestContent.Segment> lines=new ArrayList<>(activeDialogue.lines);
        lines.add(new QuestContent.Segment("fr-FR",activeDialogue.question));
        playSpoken(lines,false,()->{});
    }

    private void answerDialogue(int choice) {
        if(!"dialogue".equals(screen)||dialogueAnswered)return;
        cancelCurrent();dialogueAnswered=true;dialogueSelectedChoice=choice;
        dialogueCorrect=choice==activeDialogue.correctIndex;
        missionXp+=progress.recordDialogue(activeDialogue.id,dialogueCorrect);
        showDialogue();
        playSpoken(dialogueCorrect?"Good job!":"No, it's incorrect.","en",false,()->{});
    }

    private void showDialogue() {
        screen="dialogue";
        LinearLayout page=page();
        addText(page,"💬 DÉFI D’ÉCOUTE · "+dialogueMission.label.toUpperCase(Locale.ROOT),11,primaryColor(),true,19);
        addText(page,"Une petite discussion",26,INK,true,8);
        addText(page,"Écoute deux ou trois phrases, repère un mot ou une idée, puis choisis ta réponse.",14,MUTED,false,8);
        addPhoto(page,dialogueMission.lesson(activeDialogue.topicId).theory.get(0),130,12);
        LinearLayout box=card(page,softColor(),18);
        addCaption(box,spokenEnglish);
        addText(box,activeDialogue.question,18,INK,true,15);
        addAudioControls(page,this::playDialogue,null);
        for(int i=0;i<activeDialogue.choices.size();i++) {
            final int selected=i;
            Button choice=button(activeDialogue.choices.get(i),false,()->answerDialogue(selected));
            choice.setEnabled(!dialogueAnswered);
            if(dialogueAnswered&&i==activeDialogue.correctIndex)
                choice.setBackground(shape(MINT,15));
            else if(dialogueAnswered&&i==dialogueSelectedChoice)
                choice.setBackground(shape(RED,15));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));
            lp.topMargin=dp(9);page.addView(choice,lp);
        }
        if(dialogueAnswered) {
            addText(page,dialogueCorrect?"Bravo ! 🌟 +8 points":
                    "❌ Ce n’est pas la bonne réponse. La réponse verte est : « "+
                            activeDialogue.choices.get(activeDialogue.correctIndex)+" ».",
                    16,primaryColor(),true,17);
            addButton(page,"Voir mon bilan",true,18,this::finishLesson);
        }
        addHomeFooter(page);
    }

    private void finishLesson() {
        screen="done";status="";
        progress.recordExercises(world,"all".equals(world)?"all":selectedTopicId);
        newWorlds.clear();
        for(QuestContent.Universe mission:content.universes)
            if(progress.worldUnlocked(mission.universeId)&&
                    !unlockedBefore.contains(mission.universeId))newWorlds.add(mission.label);
        if(progress.worldUnlocked("all")&&!unlockedBefore.contains("all"))
            newWorlds.add("Tous les thèmes");
        newBadges.clear();
        for(LearningProgress.Badge badge:progress.badges())
            if(badge.earned()&&!earnedBefore.contains(badge.title))newBadges.add(badge);
        spokenEnglish="";
        if(!"all".equals(world))for(QuestContent.Segment segment:content.universe(world).outro)
            if("en".equals(segment.locale)) {spokenEnglish=segment.text;break;}
        showDone();
        Runnable unlockSound=()->{
            if(!newWorlds.isEmpty()&&"done".equals(screen))playUnlockJingle();
        };
        if(!"all".equals(world))playSpoken(content.universe(world).outro,false,unlockSound);
        else unlockSound.run();
    }

    private void playUnlockJingle() {
        Toast.makeText(this,"🎉 Nouvel univers débloqué : "+String.join(", ",newWorlds),
                Toast.LENGTH_LONG).show();
        ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_MUSIC,65);
        tone.startTone(ToneGenerator.TONE_DTMF_3,130);
        handler.postDelayed(()->tone.startTone(ToneGenerator.TONE_DTMF_6,130),180);
        handler.postDelayed(()->tone.startTone(ToneGenerator.TONE_DTMF_9,250),360);
        handler.postDelayed(tone::release,750);
    }

    private void showLesson() {
        if(step>=lesson.size()){showDone();return;}
        screen="lesson";
        LinearLayout page=page();
        LessonItem item=lesson.get(step);
        if(micOpen&&!item.multipleChoice) {
            LinearLayout listening=card(page,0xff4d3e9d,0);
            listening.setGravity(Gravity.CENTER);
            listening.setElevation(dp(12));
            TextView mic=text("🎙️",82,Color.WHITE,true);
            mic.setGravity(Gravity.CENTER);
            mic.setContentDescription("Microphone ouvert. Parle maintenant.");
            mic.setBackground(shape(0xffffe2e6,70));
            LinearLayout.LayoutParams micLp=new LinearLayout.LayoutParams(dp(136),dp(136));
            micLp.gravity=Gravity.CENTER_HORIZONTAL;
            listening.addView(mic,micLp);
            TextView label=text("MICRO OUVERT",23,Color.WHITE,true);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelLp=new LinearLayout.LayoutParams(-1,-2);
            labelLp.topMargin=dp(14);
            listening.addView(label,labelLp);
            TextView instruction=text("Parle maintenant",18,Color.WHITE,false);
            instruction.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams instructionLp=new LinearLayout.LayoutParams(-1,-2);
            instructionLp.topMargin=dp(5);
            listening.addView(instruction,instructionLp);
        }
        addText(page,item.mission.label.toUpperCase(Locale.ROOT)+" · "+item.chapter.title.toUpperCase(Locale.ROOT),11,primaryColor(),true,21);
        addText(page,"all".equals(world)?"La grande aventure":item.mission.title,24,INK,true,6);
        String format=item.multipleChoice?"🧩 QCM":
                "order".equals(item.exercise.type)?"🔀 REMETS EN ORDRE":
                "reply".equals(item.exercise.type)?"💬 RÉPONDS À L’AVENTURE":"🎙️ ORAL";
        addText(page,format+" · ÉTAPE "+item.exercise.stage+"/3",11,primaryColor(),true,8);
        addText(page,"Question "+(step+1)+" sur "+lesson.size()+"  ·  🌟 "+missionXp+" points gagnés",13,MUTED,false,8);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(lesson.size());progress.setProgress(step+1);
        progress.setProgressTintList(ColorStateList.valueOf(primaryColor()));
        LinearLayout.LayoutParams progressLp=new LinearLayout.LayoutParams(-1,dp(6));
        progressLp.topMargin=dp(10);page.addView(progress,progressLp);
        QuestContent.TheoryCard imageCard=visualFor(item);
        addPhoto(page,imageCard,145,14);
        LinearLayout hero=card(page,softColor(),20);
        String promptText=item.multipleChoice?(item.choice.wordOnly?
                "Écoute l’anglais et choisis le sens du mot entendu.":
                "Écoute l’anglais et choisis la bonne traduction."):
                "en".equals(item.exercise.questionLocale)?"Écoute cette phrase en anglais.":item.exercise.question;
        TextView prompt=text(promptText,21,INK,true);
        prompt.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams promptLp=new LinearLayout.LayoutParams(-1,-2);
        promptLp.topMargin=dp(4);
        hero.addView(prompt,promptLp);
        if(!item.multipleChoice&&!item.exercise.context.isEmpty()) {
            TextView context=text(item.exercise.context,13,MUTED,false);
            context.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams contextLp=new LinearLayout.LayoutParams(-1,-2);
            contextLp.topMargin=dp(11);hero.addView(context,contextLp);
        }
        if(("order".equals(item.exercise.type)||"reply".equals(item.exercise.type))&&
                !questionRecorded) {
            addText(hero,"👂 Écoute bien avant de répondre",16,primaryColor(),true,14);
            captionView=null;
        } else if("order".equals(item.exercise.type)||"reply".equals(item.exercise.type)) {
            addText(hero,"Réponse : "+item.exercise.model,21,INK,true,14);
            captionView=null;
        } else addCaption(hero,spokenEnglish);
        if(!item.multipleChoice)
            addText(hero,"En français : "+item.exercise.translation,17,INK,true,14);
        statusView=text(status,14,primaryColor(),true);
        statusView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateLp=new LinearLayout.LayoutParams(-1,-2);
        stateLp.topMargin=dp(17);hero.addView(statusView,stateLp);
        if(!advancedMode&&!recognizedText.isEmpty())addText(hero,"J’ai entendu : « "+recognizedText+" »",12,MUTED,false,12);
        if(item.multipleChoice) {
            if(paused) {
                addAudioControls(page,()->{paused=false;presentQuestion();},
                        ()->{paused=false;presentQuestion();});
                addHomeFooter(page);
                return;
            }
            addAudioControls(page,()->{
                cancelCurrent();
                playSpoken(item.choice.spoken,"en",false,()->{});
            },()->{cancelCurrent();paused=true;status="Séance en pause.";showLesson();});
            for(int i=0;i<item.choice.choices.size();i++) {
                final int selected=i;
                String answer=item.choice.choices.get(i);
                Button option=button(answer,false,()->answerLessonChoice(selected));
                option.setEnabled(!questionRecorded);
                if(questionRecorded&&answer.equals(item.choice.answer))option.setBackground(shape(MINT,15));
                else if(questionRecorded&&i==lessonSelectedChoice)option.setBackground(shape(RED,15));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(61));
                lp.topMargin=dp(9);page.addView(option,lp);
            }
            if(questionRecorded)addButton(page,"Question suivante",true,18,this::nextQuestion);
            addHomeFooter(page);
            return;
        }
        if("order".equals(item.exercise.type)) {
            if(paused) {
                addAudioControls(page,()->{paused=false;presentQuestion();},
                        ()->{paused=false;presentQuestion();});
                addHomeFooter(page);
                return;
            }
            addAudioControls(page,()->{
                cancelCurrent();playSpoken(item.exercise.questionAudio,false,()->{});
            },()->{cancelCurrent();paused=true;status="Séance en pause.";showLesson();});
            orderAnswerBox=card(page,LIGHT,14);
            orderAnswerView=text("",20,INK,true);
            orderAnswerView.setMinHeight(dp(96));
            orderAnswerBox.addView(orderAnswerView,new LinearLayout.LayoutParams(-1,-2));
            orderErrorView=text("",15,0xffb52b47,true);
            LinearLayout.LayoutParams errorLp=new LinearLayout.LayoutParams(-1,dp(43));
            errorLp.topMargin=dp(10);page.addView(orderErrorView,errorLp);
            orderTileButtons.clear();
            if(!questionRecorded) {
                for(int first=0;first<orderTiles.size();first+=2) {
                    LinearLayout row=new LinearLayout(this);
                    LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,dp(55));
                    rowLp.topMargin=dp(7);page.addView(row,rowLp);
                    for(int index=first;index<Math.min(first+2,orderTiles.size());index++) {
                        final int tile=index;
                        Button word=button(orderTiles.get(index),false,()->chooseOrderTile(tile));
                        orderTileButtons.add(word);
                        LinearLayout.LayoutParams tileLp=new LinearLayout.LayoutParams(0,-1,1);
                        if(index>first)tileLp.leftMargin=dp(7);
                        row.addView(word,tileLp);
                    }
                }
                addButton(page,"↶ Effacer le dernier mot",false,10,()->{
                    if(!orderChosen.isEmpty())orderChosen.remove(orderChosen.size()-1);
                    orderWrong=false;status="Essaie un autre ordre.";renderOrderChoice();
                });
                addButton(page,"Voir la réponse et continuer",false,9,()->{
                    cancelCurrent();feedback("autoAnswer",true);
                });
            } else addButton(page,"Question suivante",true,18,this::nextQuestion);
            renderOrderChoice();
            addHomeFooter(page);
            return;
        }
        if(paused) {
            addAudioControls(page,()->{paused=false;presentQuestion();},()->{
                paused=false;
                if(betweenQuestions||questionRecorded)nextQuestion();else presentQuestion();
            });
            addHomeFooter(page);
            return;
        }
        addAudioControls(page,()->{cancelCurrent();presentQuestion();},()->{
            cancelCurrent();paused=true;status="Séance en pause.";showLesson();
        });
        if(betweenQuestions||questionRecorded)
            addButton(page,"Question suivante",true,20,this::nextQuestion);
        else if(canSpeakNow)
            addButton(page,"🎙️ Parler maintenant",true,20,()->{
                cancelCurrent();canSpeakNow=false;waitForAnswer();
            });
        if(!questionRecorded&&advancedMode)addButton(page,"Un indice",false,9,()->{
            cancelCurrent();status="Écoute l’indice.";showLesson();
            List<QuestContent.Segment> hint=new ArrayList<>(item.exercise.hint);
            hint.add(new QuestContent.Segment("en",item.exercise.model));
            int id=++sequenceId;
            playSpoken(hint,true,()->{if(id==sequenceId)presentRetry();});
        });
        if(!questionRecorded)addButton(page,"Voir la réponse et continuer",false,9,()->{
            cancelCurrent();feedback("autoAnswer",true);
        });
        addText(page,"Tu peux réécouter autant que tu veux. Un son mal entendu n’est pas compté comme une erreur.",12,MUTED,false,22);
        addHomeFooter(page);
    }

    private void showDone() {
        screen="done";
        LinearLayout page=page();
        brand(page);
        addWorldArt(page,125,22,true);
        addText(page,"🎉  EXERCICES TERMINÉS",11,primaryColor(),true,25);
        addText(page,"Bravo, explorateur !",31,INK,true,12);
        addText(page,"Chaque essai fait partie de l’aventure.",15,MUTED,false,11);
        LinearLayout reward=card(page,GOLD,20);
        addText(reward,"🌟  +"+missionXp+" points",24,INK,true,0);
        addText(reward,missionNew+" nouvelle(s) question(s) découverte(s) · "+missionCorrect+
                " réponse(s) réussie(s)",13,MUTED,false,9);
        if(activeDialogue!=null)addText(reward,"💬 Défi d’écoute : "+
                (dialogueCorrect?"réussi":"à retravailler"),13,MUTED,false,8);
        if(activePassage!=null)addText(reward,"📖 Lecture guidée : "+
                (passageSelectedChoice==activePassage.correctIndex?"réussie":"à retravailler"),
                13,MUTED,false,8);
        addText(reward,"Niveau "+progress.level()+" · "+progress.xp()+" points au total",13,primaryColor(),true,9);
        for(String unlocked:newWorlds) {
            LinearLayout unlock=card(page,MINT,18);
            addText(unlock,"🎉  NOUVEL UNIVERS DÉBLOQUÉ !",14,primaryColor(),true,0);
            addText(unlock,"🔓 "+unlocked,24,INK,true,8);
            addText(unlock,"Une nouvelle aventure t’attend dans le choix des univers.",14,MUTED,false,7);
            unlock.setAlpha(0f);unlock.setScaleX(0.88f);unlock.setScaleY(0.88f);
            unlock.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).start();
        }
        for(LearningProgress.Badge badge:newBadges) {
            LinearLayout badgeCard=card(page,MINT,14);
            addText(badgeCard,badge.icon+"  Nouveau badge : "+badge.title,18,INK,true,0);
            addText(badgeCard,badge.description,13,MUTED,false,7);
        }
        LinearLayout card=card(page,MINT,25);
        addText(card,"Les phrases de la mission",16,INK,true,0);
        LinkedHashSet<String> models=new LinkedHashSet<>();
        for(LessonItem item:lesson)models.add(item.exercise.model);
        if(advancedMode)addText(card,"Tu as travaillé "+models.size()+" phrase(s). Réécoute-les dans une nouvelle mission.",14,INK,false,10);
        else for(String model:models)addText(card,"• "+model,15,INK,false,10);
        LinearLayout ending=card(page,softColor(),18);
        addCaption(ending,spokenEnglish);
        addButton(page,"🎧 Passer au quiz",true,28,this::startQuiz);
        addButton(page,"Refaire les exercices",false,10,this::beginExercises);
        addButton(page,"Revoir la leçon",false,10,this::beginCourse);
        addButton(page,"Choisir une autre aventure",false,10,()->{cancelCurrent();showSetup();});
        addButton(page,"Accueil",false,10,()->{cancelCurrent();showHome();});
    }

    private void showProgress() {
        screen="progress";
        LinearLayout page=page();
        addText(page,"Mes progrès",29,INK,true,27);
        addText(page,"Tes découvertes, tes points et tes badges restent sur ce téléphone.",14,MUTED,false,8);
        addScoreBoard(page,18);
        addText(page,"🏅 Mes badges",20,INK,true,24);
        for(LearningProgress.Badge badge:progress.badges()) {
            LinearLayout tile=card(page,badge.earned()?MINT:CARD,10);
            addText(tile,badge.icon+"  "+badge.title+(badge.earned()?"  ✓":""),18,INK,true,0);
            addText(tile,badge.description+" · "+Math.min(badge.progress,badge.goal)+"/"+badge.goal,
                    13,MUTED,false,7);
        }
        addText(page,"🗺️ Aventures explorées",20,INK,true,24);
        addText(page,"Univers débloqués : "+unlockedWorldCount()+"/7 · Quiz : "+
                progress.quizCorrect()+"/"+progress.quizPlayed()+" bonnes réponses",13,MUTED,false,8);
        for(QuestContent.Universe mission:content.universes) {
            if(progress.worldUnlocked(mission.universeId))continue;
            addText(page,"🔒 "+mission.label+" · "+progress.missions()+"/"+
                    progress.worldUnlockMissions(mission.universeId)+" séances · "+
                    progress.masteredQuestions()+"/"+progress.worldUnlockGoal(mission.universeId)+
                    " réponses réussies",13,MUTED,false,8);
        }
        if(!progress.worldUnlocked("all"))addText(page,"🔒 Tous les thèmes · "+
                progress.missions()+"/"+progress.worldUnlockMissions("all")+" séances · "+
                progress.masteredQuestions()+"/"+progress.worldUnlockGoal("all")+
                " réponses réussies",13,MUTED,false,8);
        addButton(page,"Voir l’état des "+content.topics.size()+" sujets",false,18,this::showTopics);
        for(QuestContent.Universe mission:content.universes) {
            LinearLayout section=card(page,CARD,20);
            addText(section,mission.label,18,INK,true,0);
            int seen=0,validated=0;
            for(QuestContent.Topic topic:content.topics) {
                int state=progress.topicState(mission.universeId,topic.id);
                if(state>=1)seen++;
                if(state==2)validated++;
            }
            addText(section,seen+" sujet(s) vus · "+validated+" validés · "+
                    prefs.getInt("missions_"+mission.universeId,0)+" séance(s) d’exercices terminées",13,MUTED,false,7);
        }
        LinearLayout section=card(page,LIGHT,20);
        addText(section,"Réponses réussies",18,INK,true,0);
        addText(section,progress.correctAnswers()+" réponses reconnues correctement",13,INK,false,10);
        addText(page,"Les réponses prononcées en mode écoute ne sont pas évaluées.",12,MUTED,false,17);
        addHomeFooter(page);
    }

    private int unlockedWorldCount() {
        int count=progress.worldUnlocked("all")?1:0;
        for(QuestContent.Universe mission:content.universes)
            if(progress.worldUnlocked(mission.universeId))count++;
        return count;
    }

    private void startQuiz() {
        if(!"all".equals(world)&&!progress.hasCompletedCourse(world,selectedTopicId)) {
            showNoQuiz();return;
        }
        PlaybackCoordinator.startPhone();
        cancelCurrent();
        quiz.clear();
        quizWorld=world;quizTopicId=selectedTopicId;quizPassRecorded=false;
        quiz.addAll(QuizEngine.create(content,progress,world,selectedTopicId,10));
        quizStep=0;quizCorrect=0;quizXp=0;quizAnswered=false;quizMessage="";
        quizSelectedChoice="";
        if(quiz.size()<5) {showNoQuiz();return;}
        showQuizQuestion();
        playQuizExample();
    }

    private void showNoQuiz() {
        screen="quiz-empty";
        LinearLayout page=page();
        brand(page);addWorldArt(page,120,22,true);
        addText(page,"Pas de quiz disponible",25,INK,true,24);
        addText(page,"Termine d’abord une leçon dans ce sujet et cet univers. Il faut au moins cinq questions pour lancer un quiz.",15,MUTED,false,10);
        addButton(page,"Suivre une leçon",true,24,()->{chooseSessionTopic(false);beginCourse();});
        addButton(page,"Choisir un sujet",false,10,this::showSetup);
        addButton(page,"Accueil",false,10,this::showHome);
    }

    private void playQuizExample() {
        if(!"quiz".equals(screen)||quizStep>=quiz.size())return;
        cancelCurrent();
        QuizEngine.Question q=quiz.get(quizStep);
        spokenEnglish=q.spoken();
        playSpoken(q.spoken(),"en",false,()->{});
    }

    private void answerQuiz(String choice) {
        if(!"quiz".equals(screen)||quizAnswered)return;
        cancelCurrent();
        QuizEngine.Question q=quiz.get(quizStep);
        quizAnswered=true;quizSelectedChoice=choice;
        boolean correct=choice.equals(q.answer());
        int earned=progress.recordQuiz(q.key,correct);
        if(correct)quizCorrect++;
        quizXp+=earned;
        quizMessage=correct?"Bravo ! 🌟 +"+earned+" points":
                "❌ Ta réponse est fausse. La réponse verte est : « "+q.answer()+
                        " ». Écoute encore si tu veux.";
        showQuizQuestion();
        playSpoken(correct?"Good job!":"No, it's incorrect.","en",false,()->{});
    }

    private void showQuizQuestion() {
        if(quizStep>=quiz.size()){showQuizDone();return;}
        screen="quiz";
        QuizEngine.Question q=quiz.get(quizStep);
        LinearLayout page=page();
        addText(page,"🎧 QUIZ · "+q.mission.label.toUpperCase(Locale.ROOT)+" · "+
                content.topic(q.lesson.topicId).label.toUpperCase(Locale.ROOT),
                11,primaryColor(),true,19);
        addText(page,"Question "+(quizStep+1)+" sur "+quiz.size(),23,INK,true,8);
        addText(page,"✅ "+quizCorrect+" bonne"+(quizCorrect>1?"s":"")+
                " réponse"+(quizCorrect>1?"s":"")+" pour le moment",15,primaryColor(),true,7);
        addText(page,q.wordOnly?"Que signifie ce mot anglais ?":"Écoute la phrase anglaise. Quelle phrase française signifie la même chose ?",15,MUTED,false,8);
        addPhoto(page,q.card,145,16);
        LinearLayout box=card(page,softColor(),18);
        if(q.wordOnly)addText(box,q.card.word,32,INK,true,0);
        else addCaption(box,q.card.example);
        addAudioControls(page,this::playQuizExample,null);
        for(String choice:q.choices) {
            Button answer=button(choice,false,()->answerQuiz(choice));
            answer.setEnabled(!quizAnswered);
            if(quizAnswered&&choice.equals(q.answer()))answer.setBackground(shape(MINT,15));
            else if(quizAnswered&&choice.equals(quizSelectedChoice))answer.setBackground(shape(RED,15));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(63));
            lp.topMargin=dp(10);page.addView(answer,lp);
        }
        if(quizAnswered) {
            addText(page,quizMessage,17,primaryColor(),true,19);
            addButton(page,quizStep+1==quiz.size()?"Voir mon score":"Question suivante",true,17,()->{
                cancelCurrent();quizStep++;quizAnswered=false;quizMessage="";quizSelectedChoice="";
                if(quizStep>=quiz.size())showQuizDone();
                else {showQuizQuestion();playQuizExample();}
            });
        }
        addText(page,"Tu peux réécouter sans limite de temps.",12,MUTED,false,20);
        addHomeFooter(page);
    }

    private void showQuizDone() {
        if(!quizPassRecorded) {
            progress.recordQuizOutcome(quizWorld,quizTopicId,quizCorrect,quiz.size());
            quizPassRecorded=true;
        }
        screen="quiz-done";
        LinearLayout page=page();
        brand(page);addWorldArt(page,130,20,true);
        addText(page,"🏆 QUIZ TERMINÉ",12,primaryColor(),true,25);
        addText(page,quizCorrect+" / "+quiz.size()+" bonnes réponses",29,INK,true,9);
        LinearLayout reward=card(page,GOLD,20);
        addText(reward,"🌟 +"+quizXp+" points",23,INK,true,0);
        addText(reward,"Niveau "+progress.level()+" · "+progress.xp()+" points au total",13,MUTED,false,8);
        boolean passed=quiz.size()>=5&&quiz.size()<=10&&quizCorrect*4>quiz.size()*3;
        if(!"all".equals(quizWorld)) {
            String result=passed?
                    "✅ Sujet validé !":progress.topicState(quizWorld,quizTopicId)==2?
                    "✅ Sujet déjà validé. Essaie encore pour améliorer ton score.":
                    "Encore un essai : il faut plus de 75 % de bonnes réponses.";
            addText(reward,result,14,INK,true,10);
            if(passed) {
                LinearLayout diploma=card(page,MINT,20);
                addText(diploma,"🏅  DIPLÔME DE RÉUSSITE",22,primaryColor(),true,0);
                addText(diploma,content.topic(quizTopicId).label+" · "+
                        content.universe(quizWorld).label,17,INK,true,10);
                addText(diploma,quizCorrect+" bonnes réponses sur "+quiz.size(),15,INK,false,6);
            }
        } else {
            addText(reward,"Le quiz mélange ne valide pas un sujet précis.",14,INK,false,10);
            if(passed) {
                LinearLayout diploma=card(page,MINT,20);
                addText(diploma,"🏅  DÉFI RÉUSSI",22,primaryColor(),true,0);
                addText(diploma,"Grand mélange · "+quizCorrect+" sur "+quiz.size(),16,INK,true,9);
            }
        }
        addButton(page,"Rejouer un quiz",true,23,this::startQuiz);
        addButton(page,"Accueil",false,10,this::showHome);
    }

    private void playCarMission() {
        String id=("all".equals(world)?content.universes.get(0).universeId:world)+":"+
                selectedTopicId+":"+accent;
        Intent intent=new Intent(this,QuestMediaService.class);
        intent.setAction(QuestMediaService.ACTION_PLAY_TRACK);
        intent.putExtra(QuestMediaService.EXTRA_TRACK_ID,id);
        startForegroundService(intent);
        Toast.makeText(this,"Mission audio lancée. Retrouve Hello Quest dans Android Auto.",Toast.LENGTH_LONG).show();
    }

    private void showAutoHelp() {
        screen="auto-help";
        LinearLayout page=page();
        addText(page,"🚗 Android Auto",28,INK,true,26);
        addText(page,"Hello Quest apparaît dans les applications audio de la voiture, avec six univers et une piste par sujet.",15,MUTED,false,10);
        LinearLayout box=card(page,softColor(),19);
        addText(box,"Pour afficher cet APK de test",18,INK,true,0);
        addText(box,"1. Sur le téléphone, ouvre les réglages Android Auto et touche « Version » dix fois pour activer les options développeur.",14,INK,false,10);
        addText(box,"2. Dans le menu ⋮ des options développeur, active « Sources inconnues ».",14,INK,false,9);
        addText(box,"3. Vérifie que Hello Quest est visible dans « Personnaliser le lanceur », puis reconnecte le téléphone à la voiture.",14,INK,false,9);
        addText(page,"Ce réglage est nécessaire pour tester une application audio installée hors du Play Store.",13,MUTED,false,15);
        addButton(page,"Tester le catalogue audio",true,20,this::checkCarCatalogue);
        if(!carCheckMessage.isEmpty())addText(page,carCheckMessage,15,primaryColor(),true,14);
        if(!"all".equals(world))addButton(page,"Lancer la mission audio de "+worldLabel(),false,13,this::playCarMission);
        addButton(page,"Retour aux réglages",false,15,this::showSettings);
        addHomeFooter(page);
    }

    private void checkCarCatalogue() {
        if(carBrowser!=null){carBrowser.disconnect();carBrowser=null;}
        carCheckMessage="Connexion au catalogue audio…";
        showAutoHelp();
        carBrowser=new MediaBrowserCompat(this,new ComponentName(this,QuestMediaService.class),
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override public void onConnected() {
                        if(carBrowser==null)return;
                        carBrowser.subscribe(carBrowser.getRoot(),new MediaBrowserCompat.SubscriptionCallback() {
                            @Override public void onChildrenLoaded(String parentId,
                                    List<MediaBrowserCompat.MediaItem> children) {
                                carCheckMessage=children.size()==content.universes.size()?"✓ Catalogue accessible : "+
                                        children.size()+" univers audio.":"Catalogue incomplet : "+children.size()+" univers.";
                                if("auto-help".equals(screen))showAutoHelp();
                                if(carBrowser!=null){carBrowser.disconnect();carBrowser=null;}
                            }
                            @Override public void onError(String parentId) {
                                carCheckMessage="Impossible de lire le catalogue audio.";
                                if("auto-help".equals(screen))showAutoHelp();
                            }
                        });
                    }
                    @Override public void onConnectionFailed() {
                        carCheckMessage="Le service audio ne répond pas. Réinstalle l’APK.";
                        if("auto-help".equals(screen))showAutoHelp();
                    }
                    @Override public void onConnectionSuspended() {
                        carCheckMessage="Connexion audio interrompue. Relance le test.";
                        if("auto-help".equals(screen))showAutoHelp();
                    }
                },null);
        carBrowser.connect();
    }

    private void cancelCurrent() {
        sequenceId++;
        if(audio!=null)audio.stop();
        micOpen=false;
        if(recognizer!=null && listening) {listening=false;try{recognizer.cancel();}catch(Exception ignored){}}
    }

    @Override protected void onPause() {
        cancelCurrent();
        paused=true;
        if("theory".equals(screen))status="Cours en pause. Reprends quand tu es prêt.";
        else if("lesson".equals(screen))status="Séance en pause. Reprends quand tu es prêt.";
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if(paused&&"theory".equals(screen))showTheory();
        else if(paused&&"lesson".equals(screen))showLesson();
    }

    @Override public void onBackPressed() {
        if("home".equals(screen))super.onBackPressed();
        else {cancelCurrent();paused=false;showHome();}
    }

    @Override protected void onDestroy() {
        PlaybackCoordinator.unregisterPhone(stopPhoneForCar);
        cancelCurrent();
        if(carBrowser!=null){carBrowser.disconnect();carBrowser=null;}
        if(recognizer!=null) {recognizer.destroy();recognizer=null;}
        super.onDestroy();
    }

    private int dp(int value) {return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
}
