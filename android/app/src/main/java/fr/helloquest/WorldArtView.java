package fr.helloquest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Small original illustrations; no licensed artwork is packaged. */
final class WorldArtView extends View {
    private final String world;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    WorldArtView(Context context, String world) {
        super(context);
        this.world = world;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void fill(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void circle(Canvas c, float x, float y, float radius, int color) {
        fill(color); c.drawCircle(x, y, radius, paint);
    }
    private void rect(Canvas c, float l, float t, float r, float b, float radius, int color) {
        fill(color); c.drawRoundRect(new RectF(l,t,r,b),radius,radius,paint);
    }
    private void path(Canvas c, int color, float... points) {
        Path shape = new Path(); shape.moveTo(points[0],points[1]);
        for (int i=2;i<points.length;i+=2) shape.lineTo(points[i],points[i+1]);
        shape.close(); fill(color); c.drawPath(shape,paint);
    }
    private void sparkle(Canvas c, float x, float y, float size, int color) {
        path(c,color,x,y-size,x+size*.23f,y-size*.23f,x+size,y,
                x+size*.23f,y+size*.23f,x,y+size,x-size*.23f,y+size*.23f,
                x-size,y,x-size*.23f,y-size*.23f);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(getWidth()/360f,getHeight()/160f);
        if ("all".equals(world)) drawAll(canvas);
        else if ("harry-potter".equals(world)) drawMagic(canvas);
        else if ("chess".equals(world)) drawChess(canvas);
        else if ("video-games".equals(world)) drawGames(canvas);
        else if ("travel".equals(world)) drawTravel(canvas);
        else if ("animals".equals(world)) drawAnimals(canvas);
        else drawSpace(canvas);
        canvas.restore();
    }

    private void drawSpace(Canvas c) {
        int navy=Color.rgb(37,48,106), gold=Color.rgb(255,198,92);
        rect(c,0,0,360,160,26,navy);
        circle(c,287,78,53,Color.rgb(243,139,94));
        circle(c,272,63,32,Color.rgb(255,190,111));
        fill(Color.rgb(255,223,164));
        c.save();c.rotate(-17,281,79);c.drawOval(new RectF(214,70,347,91),paint);c.restore();
        circle(c,287,78,45,Color.rgb(243,139,94));
        circle(c,272,63,28,Color.rgb(255,190,111));
        sparkle(c,57,43,12,gold);sparkle(c,169,33,7,Color.WHITE);
        sparkle(c,197,105,10,Color.WHITE);sparkle(c,40,115,6,gold);
        path(c,Color.rgb(220,239,255),95,87,158,73,184,84,158,94,95,87);
        path(c,Color.rgb(116,199,230),123,83,154,49,155,80,123,83);
        path(c,Color.rgb(116,199,230),124,89,157,118,158,93,124,89);
        circle(c,172,84,5,Color.rgb(255,207,98));
    }

    private void drawAll(Canvas c) {
        rect(c,0,0,360,160,26,Color.rgb(97,57,143));
        int[] colors={Color.rgb(255,192,92),Color.rgb(121,218,202),
                Color.rgb(255,137,120),Color.rgb(150,183,255),
                Color.rgb(233,152,225),Color.rgb(174,218,133)};
        for(int i=0;i<6;i++) {
            float x=43+(i%3)*132;
            float y=46+(i/3)*73;
            circle(c,x,y,24,colors[i]);
            sparkle(c,x,y,12,Color.WHITE);
        }
        sparkle(c,177,80,15,Color.rgb(255,224,127));
    }

    private void drawMagic(Canvas c) {
        int plum=Color.rgb(93,52,116), gold=Color.rgb(255,209,114);
        rect(c,0,0,360,160,26,plum);
        circle(c,292,42,48,Color.rgb(121,75,144));
        sparkle(c,57,42,13,gold);sparkle(c,301,40,11,gold);
        sparkle(c,242,75,7,Color.WHITE);sparkle(c,96,91,6,Color.WHITE);
        path(c,Color.rgb(248,230,191),91,78,172,91,172,139,91,125);
        path(c,Color.rgb(255,246,222),174,91,254,78,254,125,174,139);
        path(c,Color.rgb(242,179,108),83,75,91,78,91,125,172,139,174,146,82,130);
        path(c,Color.rgb(242,179,108),254,78,262,75,263,130,174,146,174,139,254,125);
        fill(Color.rgb(177,123,97));paint.setStrokeWidth(3);paint.setStyle(Paint.Style.STROKE);
        c.drawLine(174,92,174,141,paint);
        c.drawLine(109,91,153,98,paint);c.drawLine(109,104,153,111,paint);
        c.drawLine(195,98,239,91,paint);c.drawLine(195,111,239,104,paint);
        fill(gold);c.drawCircle(174,145,4,paint);
        fill(gold);paint.setStrokeWidth(5);paint.setStyle(Paint.Style.STROKE);
        c.drawLine(269,119,317,69,paint);
        sparkle(c,318,67,10,gold);
    }

    private void drawChess(Canvas c) {
        int teal=Color.rgb(27,105,111), cream=Color.rgb(255,244,214);
        rect(c,0,0,360,160,26,teal);
        for(int row=0;row<2;row++)for(int col=0;col<8;col++)
            if((row+col)%2==0)rect(c,0+col*45,120+row*20,45+col*45,140+row*20,0,Color.rgb(65,139,139));
        rect(c,95,112,169,122,4,cream);
        rect(c,106,96,158,113,5,cream);
        rect(c,113,43,151,99,5,cream);
        path(c,cream,108,45,108,36,119,36,119,45,129,45,129,36,140,36,140,45,151,45,151,36,161,36,157,54,113,54);
        rect(c,190,113,275,122,4,Color.rgb(255,207,119));
        path(c,Color.rgb(255,207,119),201,108,210,57,224,77,231,45,242,75,257,56,265,108);
        circle(c,210,51,5,cream);circle(c,231,39,6,cream);circle(c,257,50,5,cream);
        sparkle(c,57,47,10,Color.rgb(255,207,119));
        sparkle(c,305,45,10,cream);
    }

    private void drawGames(Canvas c) {
        int coral=Color.rgb(188,81,64), cream=Color.rgb(255,246,218);
        rect(c,0,0,360,160,26,coral);
        for(int i=0;i<5;i++)rect(c,33+i*65,23+(i%2)*16,42+i*65,32+(i%2)*16,2,Color.rgb(255,183,112));
        sparkle(c,47,110,10,cream);sparkle(c,315,55,10,cream);
        rect(c,66,63,294,135,35,Color.rgb(255,204,142));
        rect(c,95,51,265,128,32,cream);
        rect(c,121,75,132,108,2,Color.rgb(75,91,151));
        rect(c,110,86,143,97,2,Color.rgb(75,91,151));
        circle(c,232,83,9,Color.rgb(86,163,143));
        circle(c,254,99,9,Color.rgb(93,117,198));
        circle(c,210,102,5,Color.rgb(225,146,99));
    }

    private void drawTravel(Canvas c) {
        int sky=Color.rgb(47,131,182), cloud=Color.rgb(245,252,255);
        rect(c,0,0,360,160,26,sky);
        circle(c,132,81,59,Color.rgb(130,211,226));
        path(c,Color.rgb(91,169,119),105,31,125,40,132,58,115,66,104,84,80,79,78,62);
        path(c,Color.rgb(91,169,119),154,43,179,56,186,79,163,78,157,95,135,109,123,91,139,75);
        path(c,Color.rgb(91,169,119),102,113,124,116,137,136,123,139,109,130);
        fill(Color.rgb(233,250,255));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
        c.drawOval(new RectF(93,22,171,140),paint);
        c.drawLine(73,80,191,80,paint);
        path(c,cloud,222,62,308,39,291,70,334,82,285,86,274,106,260,83);
        rect(c,250,115,318,121,4,Color.rgb(255,219,133));
        sparkle(c,319,34,8,Color.rgb(255,223,145));
    }

    private void drawAnimals(Canvas c) {
        int green=Color.rgb(58,130,85), cream=Color.rgb(255,245,213);
        rect(c,0,0,360,160,26,green);
        circle(c,95,46,42,Color.rgb(89,154,98));
        circle(c,285,50,36,Color.rgb(89,154,98));
        path(c,Color.rgb(246,155,80),77,80,92,30,122,58,144,30,159,80);
        circle(c,118,91,45,Color.rgb(246,155,80));
        path(c,cream,76,102,118,125,160,102,143,118,93,118);
        circle(c,102,86,4,Color.rgb(48,54,62));
        circle(c,135,86,4,Color.rgb(48,54,62));
        path(c,Color.rgb(48,54,62),113,105,123,105,118,111);
        path(c,Color.rgb(204,219,219),209,81,224,41,242,67,264,41,279,81);
        circle(c,244,96,39,Color.rgb(204,219,219));
        circle(c,231,89,4,Color.rgb(48,54,62));
        circle(c,258,89,4,Color.rgb(48,54,62));
        path(c,Color.rgb(232,141,135),239,106,249,106,244,113);
        sparkle(c,47,43,8,cream);sparkle(c,317,113,8,cream);
    }
}
