package com.example.expresseequilibre.ui;

import android.graphics.Canvas;
import android.graphics.Path;

/** Pictogrammes dessinés au Canvas (aucun asset à importer). Chaque icône tient dans un carré de côté s centré en (cx, cy). */
public final class Icons {
    private Icons() {}

    // ---------- Outils de tracé ----------

    /** Chemin passant par les points x0,y0, x1,y1, ... (réutilise Ui.PATH). */
    private static Path path(float... xy) {
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        return p;
    }

    private static void polyline(Canvas c, int color, float width, float... xy) {
        c.drawPath(path(xy), Ui.stroke(color, width));
    }

    private static void polygon(Canvas c, int color, float... xy) {
        Path p = path(xy);
        p.close();
        c.drawPath(p, Ui.fill(color));
    }

    private static void line(Canvas c, int color, float width, float x0, float y0, float x1, float y1) {
        c.drawLine(x0, y0, x1, y1, Ui.stroke(color, width));
    }

    /** Contour d'un téléphone (partagé par les animations Incliner et Secouer). */
    private static void phone(Canvas c, float cx, float cy, float s, int color) {
        Ui.R.set(cx - s * 0.2f, cy - s * 0.36f, cx + s * 0.2f, cy + s * 0.36f);
        c.drawRoundRect(Ui.R, s * 0.06f, s * 0.06f, Ui.stroke(color, s * 0.08f));
        line(c, color, s * 0.06f, cx - s * 0.06f, cy - s * 0.26f, cx + s * 0.06f, cy - s * 0.26f);
    }

    // ---------- Commandes ----------

    public static void play(Canvas c, float cx, float cy, float s, int color) {
        polygon(c, color, cx - s * 0.28f, cy - s * 0.40f, cx - s * 0.28f, cy + s * 0.40f, cx + s * 0.42f, cy);
    }

    public static void pause(Canvas c, float cx, float cy, float s, int color) {
        float bw = s * 0.22f, bh = s * 0.72f;
        Ui.roundRect(c, cx - s * 0.30f, cy - bh / 2, cx - s * 0.30f + bw, cy + bh / 2, bw * 0.3f, color);
        Ui.roundRect(c, cx + s * 0.08f, cy - bh / 2, cx + s * 0.08f + bw, cy + bh / 2, bw * 0.3f, color);
    }

    public static void jump(Canvas c, float cx, float cy, float s, int color) {
        float w = s * 0.14f;
        line(c, color, w, cx, cy + s * 0.42f, cx, cy - s * 0.34f);
        polyline(c, color, w, cx - s * 0.30f, cy - s * 0.04f, cx, cy - s * 0.38f, cx + s * 0.30f, cy - s * 0.04f);
    }

    public static void wind(Canvas c, float cx, float cy, float s, int color) {
        float x0 = cx - s * 0.45f;
        for (int i = -1; i <= 1; i++) {
            float y = cy + i * s * 0.27f;
            float len = (i == 0) ? 0.95f : 0.65f;
            Path p = Ui.PATH;
            p.reset();
            p.moveTo(x0, y);
            p.cubicTo(x0 + s * 0.30f, y - s * 0.20f, x0 + s * 0.50f, y + s * 0.20f, x0 + s * 0.9f * len, y);
            c.drawPath(p, Ui.stroke(color, s * 0.09f));
        }
    }

    public static void tiltAnim(Canvas c, float cx, float cy, float s, int color, float angle) {
        c.save();
        c.rotate(angle, cx, cy);
        phone(c, cx, cy, s, color);
        c.restore();
        Ui.R.set(cx - s * 0.52f, cy - s * 0.52f, cx + s * 0.52f, cy + s * 0.52f);
        c.drawArc(Ui.R, 20, 35, false, Ui.stroke(color, s * 0.07f));
        c.drawArc(Ui.R, 125, 35, false, Ui.stroke(color, s * 0.07f));
    }

    public static void shakeAnim(Canvas c, float cx, float cy, float s, int color, float dx) {
        c.save();
        c.translate(dx, 0f);
        phone(c, cx, cy, s, color);
        c.restore();
        float m = s * 0.38f;
        for (int side = -1; side <= 1; side += 2) {
            for (int row = -1; row <= 1; row += 2) {
                line(c, color, s * 0.07f, cx + side * m, cy + row * s * 0.2f, cx + side * (m + s * 0.12f), cy + row * s * 0.2f);
            }
        }
    }

    /** Joystick : dx, dy (-1..1) décalent la poignée (animation du tutoriel). */
    public static void joystick(Canvas c, float cx, float cy, float s, int color, float dx, float dy) {
        c.drawCircle(cx, cy, s * 0.42f, Ui.stroke(color, s * 0.07f));
        c.drawCircle(cx + dx * s * 0.17f, cy + dy * s * 0.17f, s * 0.20f, Ui.fill(color));
    }

    // ---------- Étoiles ----------

    public static void star(Canvas c, float cx, float cy, float r, int color, boolean filled) {
        Path p = Ui.PATH;
        p.reset();
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + i * 36);
            float rad = (i % 2 == 0) ? r : r * 0.42f;
            float px = cx + (float) Math.cos(a) * rad;
            float py = cy + (float) Math.sin(a) * rad;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.close();
        c.drawPath(p, filled ? Ui.fill(color) : Ui.stroke(color, r * 0.14f));
    }

    /** Étoile de notation : dorée avec contour si gagnée, grise sinon. */
    public static void rating(Canvas c, float cx, float cy, float r, boolean earned) {
        star(c, cx, cy, r, earned ? Ui.STAR : Ui.STAR_EMPTY, true);
        if (earned) star(c, cx, cy, r, Ui.STAR_EDGE, false);
    }

    // ---------- Bonus / malus ----------

    public static void clock(Canvas c, float cx, float cy, float s, int color) {
        float w = s * 0.09f;
        c.drawCircle(cx, cy, s * 0.38f, Ui.stroke(color, w));
        line(c, color, w, cx, cy, cx, cy - s * 0.24f);
        line(c, color, w, cx, cy, cx + s * 0.17f, cy + s * 0.08f);
    }

    public static void fastClock(Canvas c, float cx, float cy, float s, int color) {
        clock(c, cx - s * 0.1f, cy, s * 0.85f, color);
        for (int i = 0; i < 2; i++) {
            float x = cx + s * 0.28f + i * s * 0.12f;
            polyline(c, color, s * 0.07f, x, cy - s * 0.14f, x + s * 0.1f, cy, x, cy + s * 0.14f);
        }
    }

    public static void spring(Canvas c, float cx, float cy, float s, int color) {
        Path p = Ui.PATH;
        p.reset();
        float y = cy + s * 0.4f, step = s * 0.16f;
        p.moveTo(cx, y);
        for (int i = 0; i < 5; i++) {
            y -= step;
            p.lineTo(i % 2 == 0 ? cx - s * 0.25f : cx + s * 0.25f, y);
        }
        p.lineTo(cx, y - step * 0.5f);
        c.drawPath(p, Ui.stroke(color, s * 0.09f));
    }

    public static void magnet(Canvas c, float cx, float cy, float s, int color) {
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(cx - s * 0.3f, cy - s * 0.4f);
        p.lineTo(cx - s * 0.3f, cy + s * 0.05f);
        Ui.R.set(cx - s * 0.3f, cy - s * 0.25f, cx + s * 0.3f, cy + s * 0.35f);
        p.arcTo(Ui.R, 180, -180);
        p.lineTo(cx + s * 0.3f, cy - s * 0.4f);
        c.drawPath(p, Ui.stroke(color, s * 0.13f));
    }

    public static void hammer(Canvas c, float cx, float cy, float s, int color) {
        c.save();
        c.rotate(35f, cx, cy);
        line(c, color, s * 0.12f, cx, cy - s * 0.05f, cx, cy + s * 0.42f);
        Ui.roundRect(c, cx - s * 0.32f, cy - s * 0.40f, cx + s * 0.32f, cy - s * 0.08f, s * 0.06f, color);
        c.restore();
    }

    public static void invert(Canvas c, float cx, float cy, float s, int color) {
        float w = s * 0.09f;
        line(c, color, w, cx - s * 0.38f, cy - s * 0.18f, cx + s * 0.38f, cy - s * 0.18f);
        polyline(c, color, w, cx + s * 0.2f, cy - s * 0.34f, cx + s * 0.4f, cy - s * 0.18f, cx + s * 0.2f, cy - s * 0.02f);
        line(c, color, w, cx - s * 0.38f, cy + s * 0.18f, cx + s * 0.38f, cy + s * 0.18f);
        polyline(c, color, w, cx - s * 0.2f, cy + s * 0.02f, cx - s * 0.4f, cy + s * 0.18f, cx - s * 0.2f, cy + s * 0.34f);
    }

    public static void snow(Canvas c, float cx, float cy, float s, int color) {
        for (int k = 0; k < 3; k++) {
            double a = Math.toRadians(k * 60);
            float dx = (float) Math.cos(a) * s * 0.4f, dy = (float) Math.sin(a) * s * 0.4f;
            line(c, color, s * 0.09f, cx - dx, cy - dy, cx + dx, cy + dy);
        }
    }

    public static void bigBall(Canvas c, float cx, float cy, float s, int color) {
        c.drawCircle(cx, cy, s * 0.42f, Ui.stroke(color, s * 0.07f));
        c.drawCircle(cx, cy, s * 0.22f, Ui.fill(color));
    }

    public static void question(Canvas c, float cx, float cy, float s, int color) {
        Ui.text(c, "?", cx, cy + s * 0.34f, s * 0.95f, color, true);
    }

    // ---------- Divers ----------

    public static void flag(Canvas c, float cx, float cy, float s, int color) {
        line(c, color, s * 0.09f, cx - s * 0.25f, cy + s * 0.42f, cx - s * 0.25f, cy - s * 0.42f);
        polygon(c, color, cx - s * 0.25f, cy - s * 0.40f, cx + s * 0.38f, cy - s * 0.2f, cx - s * 0.25f, cy);
    }

    public static void lock(Canvas c, float cx, float cy, float s, int color) {
        Ui.roundRect(c, cx - s * 0.28f, cy - s * 0.02f, cx + s * 0.28f, cy + s * 0.40f, s * 0.06f, color);
        Ui.R.set(cx - s * 0.18f, cy - s * 0.40f, cx + s * 0.18f, cy + s * 0.12f);
        c.drawArc(Ui.R, 180, 180, false, Ui.stroke(color, s * 0.09f));
    }

    public static void flash(Canvas c, float cx, float cy, float s, int color) {
        polygon(c, color,
                cx + s * 0.10f, cy - s * 0.45f, cx - s * 0.25f, cy + s * 0.05f, cx - s * 0.02f, cy + s * 0.05f,
                cx - s * 0.10f, cy + s * 0.45f, cx + s * 0.25f, cy - s * 0.08f, cx + s * 0.02f, cy - s * 0.08f);
    }

    public static void check(Canvas c, float cx, float cy, float s, int color) {
        polyline(c, color, s * 0.14f, cx - s * 0.30f, cy + s * 0.02f, cx - s * 0.08f, cy + s * 0.26f, cx + s * 0.32f, cy - s * 0.24f);
    }
}
