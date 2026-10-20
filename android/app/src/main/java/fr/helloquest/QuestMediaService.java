package fr.helloquest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONObject;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media.MediaBrowserServiceCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Standard media browser and playback session used by Android Auto. */
public final class QuestMediaService extends MediaBrowserServiceCompat {
    static final String ACTION_PLAY_TRACK="fr.helloquest.PLAY_TRACK";
    static final String ACTION_TOGGLE="fr.helloquest.TOGGLE";
    static final String EXTRA_TRACK_ID="trackId";
    private static final String CHANNEL="hello_quest_audio";
    private static final int NOTIFICATION_ID=112;
    private static final String ROOT="helloquest-root";

    private QuestContent content;
    private JSONObject tracks;
    private MediaSessionCompat session;
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private String currentId="";
    private boolean interrupted;
    private int playerGeneration;
    private final Runnable stopForPhone=this::stop;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        PlaybackCoordinator.registerCar(stopForPhone);
        try {
            content=QuestContent.load(this);
            tracks=new JSONObject(QuestContent.read(this,"audio/tracks/index.json"));
        } catch(Exception e) {stopSelf();return;}
        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Missions audio",NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(manager!=null)manager.createNotificationChannel(channel);
        session=new MediaSessionCompat(this,"Hello Quest");
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS|MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlayFromMediaId(String mediaId,Bundle extras) {playTrack(mediaId);}
            @Override public void onPlayFromSearch(String query,Bundle extras) {playFromSearch(query);}
            @Override public void onPlay() {resume();}
            @Override public void onPause() {pause();}
            @Override public void onStop() {stop();}
            @Override public void onSeekTo(long pos) {
                if(player!=null){player.seekTo((int)pos);publish(PlaybackStateCompat.STATE_PLAYING);}
            }
            @Override public void onSkipToNext() {skip(1);}
            @Override public void onSkipToPrevious() {skip(-1);}
        });
        session.setActive(true);
        setSessionToken(session.getSessionToken());
        publish(PlaybackStateCompat.STATE_NONE);
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName,int clientUid,Bundle rootHints) {
        return content==null?null:new BrowserRoot(ROOT,null);
    }

    @Override public void onLoadChildren(String parentId,Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> children=new ArrayList<>();
        if(content==null){result.sendResult(children);return;}
        if(ROOT.equals(parentId)) {
            for(QuestContent.Universe mission:content.universes) {
                MediaDescriptionCompat desc=new MediaDescriptionCompat.Builder()
                        .setMediaId("world:"+mission.universeId)
                        .setTitle(mission.label)
                        .setSubtitle("Missions audio en anglais")
                        .setIconUri(iconUri()).build();
                children.add(new MediaBrowserCompat.MediaItem(desc,MediaBrowserCompat.MediaItem.FLAG_BROWSABLE));
            }
        } else if(parentId.startsWith("world:")) {
            QuestContent.Universe mission=content.universe(parentId.substring(6));
            if(mission==null){result.sendResult(children);return;}
            String accent=getSharedPreferences("learning",MODE_PRIVATE).getString("accent","en-GB");
            for(QuestContent.Lesson chapter:mission.lessons)
                children.add(item(mission,chapter.topicId,accent,
                        lessonTitle(chapter),"Leçon audio"));
        }
        result.sendResult(children);
    }

    private MediaBrowserCompat.MediaItem item(QuestContent.Universe mission,String chapter,String accent,String title,String subtitle) {
        String id=mission.universeId+":"+chapter+":"+accent;
        MediaDescriptionCompat desc=new MediaDescriptionCompat.Builder().setMediaId(id).setTitle(title)
                .setSubtitle(subtitle+" · "+mission.label)
                .setIconUri(iconUri()).build();
        return new MediaBrowserCompat.MediaItem(desc,MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    private Uri iconUri() {return Uri.parse("android.resource://"+getPackageName()+"/"+R.drawable.ic_app);}

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && ACTION_PLAY_TRACK.equals(intent.getAction()))
            playTrack(intent.getStringExtra(EXTRA_TRACK_ID));
        else if(intent!=null && ACTION_TOGGLE.equals(intent.getAction())) {
            if(player!=null && player.isPlaying())pause();else resume();
        }
        return START_NOT_STICKY;
    }

    private void playTrack(String id) {
        if(id==null || tracks==null) return;
        String path=tracks.optString(id,"");
        if(path.isEmpty())return;
        PlaybackCoordinator.startCar();
        releasePlayer();
        currentId=id;
        QuestContent.Universe mission=missionForTrack(id);
        String title=trackTitle(id);
        session.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,mission.label+" · Hello Quest")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,"Missions d’anglais")
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,cover())
                .build());
        publish(PlaybackStateCompat.STATE_BUFFERING);
        // The activity starts this as a foreground service; an Android Auto
        // controller may instead start it while bound to the media browser.
        startForeground(NOTIFICATION_ID,notification(false));
        requestFocus();
        int generation=++playerGeneration;
        try(AssetFileDescriptor file=getAssets().openFd(path)) {
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            player.setOnPreparedListener(mp->{
                if(generation!=playerGeneration)return;
                mp.start();publish(PlaybackStateCompat.STATE_PLAYING);
                updateNotification(true);
            });
            player.setOnCompletionListener(mp->{
                if(generation!=playerGeneration)return;
                mp.seekTo(0);publish(PlaybackStateCompat.STATE_STOPPED);updateNotification(false);
                abandonFocus();stopForeground(STOP_FOREGROUND_DETACH);
            });
            player.setOnErrorListener((mp,what,extra)->{
                publish(PlaybackStateCompat.STATE_ERROR);updateNotification(false);return true;
            });
            player.prepareAsync();
        }catch(IOException e){publish(PlaybackStateCompat.STATE_ERROR);stopForeground(STOP_FOREGROUND_REMOVE);}
    }

    private void playFromSearch(String query) {
        if(content==null){return;}
        String words=query==null?"":query.toLowerCase(Locale.ROOT);
        if(words.isEmpty()){resume();return;}
        QuestContent.Universe chosen=null;
        for(QuestContent.Universe mission:content.universes) {
            if(words.contains(mission.label.toLowerCase(Locale.ROOT)) ||
                    words.contains(mission.universeId.replace('-', ' '))) {
                chosen=mission;break;
            }
        }
        if(chosen==null) {
            if(words.contains("potter") || words.contains("poudlard"))chosen=content.universe("harry-potter");
            else if(words.contains("jedi") || words.contains("wars"))chosen=content.universe("star-wars");
            else if(words.contains("chess") || words.contains("echecs") || words.contains("échecs"))chosen=content.universe("chess");
            else if(words.contains("jeu") || words.contains("vidéo") || words.contains("video"))chosen=content.universe("video-games");
            else if(words.contains("voyage") || words.contains("pays"))chosen=content.universe("travel");
            else if(words.contains("animal") || words.contains("animaux"))chosen=content.universe("animals");
        }
        if(chosen==null){resume();return;}
        String chapter=chosen.lessons.get(0).topicId;
        if(words.contains("pronom"))chapter="pronouns";
        else if(words.contains("verbe") || words.contains("action"))chapter="verbs";
        else if(words.contains("vocabulaire") || words.contains("mot"))chapter="words";
        else if(words.contains("comprehension") || words.contains("compréhension"))chapter="listening";
        if(chosen.lesson(chapter)==null)chapter=chosen.lessons.get(0).topicId;
        String accent=getSharedPreferences("learning",MODE_PRIVATE).getString("accent","en-GB");
        playTrack(chosen.universeId+":"+chapter+":"+accent);
    }

    private QuestContent.Universe missionForTrack(String id) {
        return content.universe(id.split(":",3)[0]);
    }

    private String trackTitle(String id) {
        String[] fields=id.split(":",3);
        QuestContent.Universe mission=content.universe(fields[0]);
        if(fields.length<2)return mission.title;
        return lessonTitle(mission.lesson(fields[1]));
    }

    private String lessonTitle(QuestContent.Lesson lesson) {
        String label=content.topic(lesson.topicId).label;
        return lesson.title.startsWith(label)?lesson.title:label+" · "+lesson.title;
    }

    private void skip(int direction) {
        if(currentId.isEmpty())return;
        String[] pieces=currentId.split(":",3);
        if(pieces.length!=3)return;
        QuestContent.Universe mission=content.universe(pieces[0]);
        List<String> ids=new ArrayList<>();
        for(QuestContent.Lesson chapter:mission.lessons)ids.add(chapter.topicId);
        int pos=ids.indexOf(pieces[1]);
        if(pos<0)return;
        int next=(pos+direction+ids.size())%ids.size();
        playTrack(mission.universeId+":"+ids.get(next)+":"+pieces[2]);
    }

    private void resume() {
        if(player==null){
            if(currentId.isEmpty() && content!=null) {
                QuestContent.Universe mission=content.universes.get(0);
                String accent=getSharedPreferences("learning",MODE_PRIVATE).getString("accent","en-GB");
                playTrack(mission.universeId+":"+mission.lessons.get(0).topicId+":"+accent);
            }else if(!currentId.isEmpty())playTrack(currentId);
            return;
        }
        PlaybackCoordinator.startCar();
        requestFocus();
        try{player.start();publish(PlaybackStateCompat.STATE_PLAYING);startForeground(NOTIFICATION_ID,notification(true));}
        catch(IllegalStateException ignored){}
    }

    private void pause() {
        if(player!=null){
            try{if(player.isPlaying())player.pause();}catch(IllegalStateException ignored){}
            publish(PlaybackStateCompat.STATE_PAUSED);updateNotification(false);
        }
    }

    private void stop() {
        releasePlayer();abandonFocus();publish(PlaybackStateCompat.STATE_STOPPED);
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();
    }

    private void requestFocus() {
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        if(focusRequest==null)focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(change->{
                    if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        interrupted=player!=null && player.isPlaying();pause();
                    }else if(change==AudioManager.AUDIOFOCUS_GAIN && interrupted){interrupted=false;resume();}
                },handler).build();
        audioManager.requestAudioFocus(focusRequest);
    }

    private void abandonFocus() {
        if(focusRequest!=null)audioManager.abandonAudioFocusRequest(focusRequest);
    }

    private void publish(int state) {
        if(session==null)return;
        long position=0;
        if(player!=null)try{position=player.getCurrentPosition();}catch(IllegalStateException ignored){}
        long actions=PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE|PlaybackStateCompat.ACTION_STOP|
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID|PlaybackStateCompat.ACTION_SKIP_TO_NEXT|
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS|PlaybackStateCompat.ACTION_SEEK_TO;
        session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions)
                .setState(state,position,state==PlaybackStateCompat.STATE_PLAYING?1f:0f,SystemClock.elapsedRealtime()).build());
    }

    private Notification notification(boolean playing) {
        Intent launch=new Intent(this,MainActivity.class);
        PendingIntent open=PendingIntent.getActivity(this,0,launch,PendingIntent.FLAG_IMMUTABLE);
        Intent toggle=new Intent(this,QuestMediaService.class).setAction(ACTION_TOGGLE);
        PendingIntent play=PendingIntent.getService(this,1,toggle,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        String title=currentId.isEmpty()?"Hello Quest":trackTitle(currentId);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_status)
                .setContentTitle(title).setContentText("Mission d’anglais à écouter")
                .setContentIntent(open).setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,
                        playing?"Pause":"Lecture",play)
                .setStyle(new Notification.MediaStyle().setMediaSession((android.media.session.MediaSession.Token)session.getSessionToken().getToken())
                        .setShowActionsInCompactView(0))
                .setOngoing(playing).build();
    }

    private void updateNotification(boolean playing) {
        startForeground(NOTIFICATION_ID,notification(playing));
    }

    private Bitmap cover() {
        Bitmap bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.rgb(89,67,204));
        Drawable icon=getDrawable(R.drawable.ic_status);
        if(icon!=null){icon.setBounds(112,112,400,400);icon.draw(canvas);}
        return bitmap;
    }

    private void releasePlayer() {
        playerGeneration++;
        if(player!=null){
            player.setOnPreparedListener(null);player.setOnCompletionListener(null);
            player.setOnErrorListener(null);player.release();player=null;
        }
    }

    @Override public void onDestroy() {
        PlaybackCoordinator.unregisterCar(stopForPhone);
        releasePlayer();abandonFocus();
        if(session!=null){session.setActive(false);session.release();session=null;}
        super.onDestroy();
    }
}
