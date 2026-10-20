package fr.helloquest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LruCache;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;

/** Displays the real photograph assigned to a lesson concept. */
final class LessonVisualView extends View {
    private static final LruCache<String,Bitmap> IMAGES = new LruCache<String,Bitmap>(16*1024) {
        @Override protected int sizeOf(String key,Bitmap bitmap) {
            return Math.max(1,bitmap.getByteCount()/1024);
        }
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final String photo;
    private final float density;

    LessonVisualView(Context context,String word,String meaning,String visual) {
        super(context);
        if(!visual.startsWith("photo:"))throw new IllegalArgumentException("Missing photo for "+word);
        photo=visual.substring(6);
        density=getResources().getDisplayMetrics().density;
        setContentDescription("Photographie pour "+word+" : "+meaning);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth(),height=getHeight();
        Bitmap image=bitmap(photo);
        if(image==null) {
            canvas.drawColor(Color.rgb(230,236,255));
            return;
        }
        float scale=Math.max(width/image.getWidth(),height/image.getHeight());
        int sourceWidth=Math.min(image.getWidth(),Math.round(width/scale));
        int sourceHeight=Math.min(image.getHeight(),Math.round(height/scale));
        int left=(image.getWidth()-sourceWidth)/2;
        int top=(image.getHeight()-sourceHeight)/2;
        Rect source=new Rect(left,top,left+sourceWidth,top+sourceHeight);
        RectF target=new RectF(0,0,width,height);
        Path clip=new Path();
        clip.addRoundRect(target,16*density,16*density,Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawBitmap(image,source,target,paint);
        canvas.restore();
    }

    private Bitmap bitmap(String id) {
        synchronized(IMAGES) {
            Bitmap cached=IMAGES.get(id);
            if(cached!=null)return cached;
            try(InputStream input=getContext().getAssets().open("photos/"+id+".jpg")) {
                cached=BitmapFactory.decodeStream(input);
                if(cached!=null)IMAGES.put(id,cached);
                return cached;
            } catch(IOException ignored) { return null; }
        }
    }
}
