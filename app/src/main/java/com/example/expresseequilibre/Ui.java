package com.example.expresseequilibre;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;

/** Couleurs, unités et petits utilitaires de dessin partagés par tous les écrans. */
final class Ui {
    private Ui() {}

    static final int BG = Color.rgb(220, 240, 255);
    static final int PRIMARY = Color.rgb(30, 70, 120);
    static final int BUTTON = Color.rgb(21, 101, 192);
    static final int TEAL = Color.rgb(0, 121, 107);
    static final int SLATE = Color.rgb(55, 71, 79);
    static final int MUTED = Color.rgb(84, 110, 122);
    static final int BALL = Color.rgb(33, 150, 243);
    static final int STAR = Color.rgb(255, 193, 7);
    static final int STAR_EMPTY = Color.rgb(176, 190, 205);
    static final int OK = Color.rgb(46, 125, 50);
    static final int DANGER = Color.rgb(198, 40, 40);
    static final int PURPLE = Color.rgb(123, 31, 162);
    static final int ORANGE = Color.rgb(191, 87, 0);
    static final int BOARD = Color.rgb(250, 252, 255);
    static final int WALL = Color.rgb(69, 90, 120);
    static final int HOLE = Color.rgb(28, 33, 48);
    static final int TEXT = Color.rgb(25, 40, 60);
    static final int GOLD_TEXT = Color.rgb(150, 85, 0);

    static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final RectF R = new RectF();
    static final Path PATH = new Path();
    private static final Typeface TITLE = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private static float density = 1f;
    private static float spScale = 1f;

    static void init(Context c) {
        android.util.DisplayMetrics m = c.getResources().getDisplayMetrics();
        density = m.density;
        spScale = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, m);
    }

    static float dp(float v) { return v * density; }

    static float sp(float v) { return v * spScale; }

    static int alpha(int color, int a) {
        return Color.argb(Math.max(0, Math.min(255, a)), Color.red(color), Color.green(color), Color.blue(color));
    }

    static int shade(int color, float f) {
        return Color.rgb((int) (Color.red(color) * f), (int) (Color.green(color) * f), (int) (Color.blue(color) * f));
    }

    static Paint fill(int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        return P;
    }

    static Paint stroke(int color, float width) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(width);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        P.setColor(color);
        return P;
    }

    static float measure(String s, float size, boolean bold) {
        P.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        P.setTextSize(size);
        return P.measureText(s);
    }

    static void text(Canvas c, String s, float x, float y, float size, int color,
                     boolean bold, Paint.Align align, float maxW) {
        draw(c, s, x, y, size, color, bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT, align, maxW);
    }

    static void text(Canvas c, String s, float cx, float y, float size, int color, boolean bold) {
        text(c, s, cx, y, size, color, bold, Paint.Align.CENTER, 0f);
    }

    /** Titres : police "black" plus punchy. */
    static void title(Canvas c, String s, float x, float y, float size, int color, Paint.Align align, float maxW) {
        draw(c, s, x, y, size, color, TITLE, align, maxW);
    }

    private static void draw(Canvas c, String s, float x, float y, float size, int color,
                             Typeface tf, Paint.Align align, float maxW) {
        P.setStyle(Paint.Style.FILL);
        P.setTypeface(tf);
        P.setTextAlign(align);
        P.setTextSize(size);
        if (maxW > 0f) {
            float w = P.measureText(s);
            if (w > maxW) P.setTextSize(size * maxW / w);
        }
        P.setColor(color);
        c.drawText(s, x, y, P);
    }

    static void roundRect(Canvas c, float l, float t, float r, float b, float rad, int color) {
        R.set(l, t, r, b);
        c.drawRoundRect(R, rad, rad, fill(color));
    }

    static void dim(Canvas c, int w, int h, int color) {
        c.drawRect(0, 0, w, h, fill(color));
    }

    static void ball(Canvas c, float x, float y, float r) {
        c.drawCircle(x, y, r, fill(BALL));
        c.drawCircle(x, y, r - dp(1), stroke(shade(BALL, 0.7f), dp(2)));
        c.drawCircle(x - r * 0.35f, y - r * 0.35f, r * 0.26f, fill(Color.WHITE));
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static int lerp(int a, int b, float t) {
        t = clamp(t, 0f, 1f);
        return Color.rgb(
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    static int medalColor(int m) {
        return m >= 3 ? Color.rgb(255, 193, 7) : m == 2 ? Color.rgb(176, 190, 197) : Color.rgb(205, 127, 50);
    }
}