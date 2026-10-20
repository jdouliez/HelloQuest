package fr.helloquest;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class AudioClips {
    private final Context context;
    private final JSONObject index;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private List<QuestContent.Segment> queue = new ArrayList<>();
    private int cursor;
    private Runnable completion;
    private Consumer<QuestContent.Segment> segmentListener;
    private long runId;

    AudioClips(Context context) throws IOException, JSONException {
        this.context = context.getApplicationContext();
        this.index = new JSONObject(QuestContent.read(context, "audio/index.json"));
    }

    void play(List<QuestContent.Segment> segments, String accent, boolean slow, Runnable onComplete) {
        play(segments, accent, slow, null, onComplete);
    }

    void play(List<QuestContent.Segment> segments, String accent, boolean slow,
              Consumer<QuestContent.Segment> onSegment, Runnable onComplete) {
        stop();
        queue = new ArrayList<>(segments);
        cursor = 0;
        segmentListener = onSegment;
        completion = onComplete;
        next(accent, slow, runId);
    }

    void play(String text, String locale, String accent, boolean slow, Runnable onComplete) {
        List<QuestContent.Segment> single = new ArrayList<>();
        single.add(new QuestContent.Segment(locale, text));
        play(single, accent, slow, onComplete);
    }

    private void next(String accent, boolean slow, long id) {
        if (id != runId) return;
        releasePlayer();
        if (cursor >= queue.size()) {
            Runnable done = completion;
            completion = null;
            segmentListener = null;
            if (done != null) handler.post(done);
            return;
        }
        QuestContent.Segment segment = queue.get(cursor++);
        if (segmentListener != null) segmentListener.accept(segment);
        String locale = "en".equals(segment.locale) ? accent : segment.locale;
        String key = locale + "|" + (slow ? "slow" : "natural") + "|" + segment.text;
        String path = index.optString(key, "");
        if (path.isEmpty()) {
            // A missing slow variant may use its recorded natural version.
            path = index.optString(locale + "|natural|" + segment.text, "");
        }
        if (path.isEmpty()) { next(accent, slow, id); return; }
        try (AssetFileDescriptor file = context.getAssets().openFd(path)) {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            player.setOnCompletionListener(p -> next(accent, slow, id));
            player.setOnErrorListener((p, what, extra) -> { next(accent, slow, id); return true; });
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
        } catch (IOException e) {
            next(accent, slow, id);
        }
    }

    void stop() {
        runId++;
        completion = null;
        segmentListener = null;
        queue.clear();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.setOnPreparedListener(null);
            player.release();
            player = null;
        }
    }
}
