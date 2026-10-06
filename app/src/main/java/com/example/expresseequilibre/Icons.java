package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Pictogrammes dessinés au Canvas (aucun asset à importer). */
final class Icons {
    private Icons() {}

    static void play(Canvas c, float cx, float cy, float s, int color) {
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(cx - s * 0.28f, cy - s * 0.40f);
        p.lineTo(cx - s * 0.28f, cy + s * 0.40f);
        p.lineTo(cx + s * 0.42f, cy);
        p.close();
        c.drawPath(p, Ui.fill(color));
    }

    static void pause(Canvas c, float cx, float cy, float s, int color) {
        float bw = s * 0.22f, bh = s * 0.72f;
        Ui.roundRect(c, cx - s * 0.30f, cy - bh / 2, cx - s * 0.30f + bw, cy + bh / 2, bw * 0.3f, color);
        Ui.roundRect(c, cx + s * 0.08f, cy - bh / 2, cx + s * 0.08f + bw, cy + bh / 2, bw * 0.3f, color);
    }

    static void jump(Canvas c, float cx, float cy, float s, int color) {
        Paint p = Ui.stroke(color, s * 0.14f);
        c.drawLine(cx, cy + s * 0.42f, cx, cy - s * 0.34f, p);
        Path path = Ui.PATH;
        path.reset();
        path.moveTo(cx - s * 0.30f, cy - s * 0.04f);
        path.lineTo(cx, cy - s * 0.38f);
        path.lineTo(cx + s * 0.30f, cy - s * 0.04f);
        c.drawPath(path, Ui.stroke(color, s * 0.14f));
    }

    static void wind(Canvas c, float cx, float cy, float s, int color) {
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

    static void tiltAnim(Canvas c, float cx, float cy, float s, int color, float angle) {
        c.save();
        c.rotate(angle, cx, cy);
        Ui.R.set(cx - s * 0.2f, cy - s * 0.36f, cx + s * 0.2f, cy + s * 0.36f);
        c.drawRoundRect(Ui.R, s * 0.06f, s * 0.06f, Ui.stroke(color, s * 0.08f));
        c.drawLine(cx - s * 0.06f, cy - s * 0.26f, cx + s * 0.06f, cy - s * 0.26f, Ui.stroke(color, s * 0.06f));
        c.restore();
        Ui.R.set(cx - s * 0.52f, cy - s * 0.52f, cx + s * 0.52f, cy + s * 0.52f);
        c.drawArc(Ui.R, 20, 35, false, Ui.stroke(color, s * 0.07f));
        c.drawArc(Ui.R, 125, 35, false, Ui.stroke(color, s * 0.07f));
    }

    static void shakeAnim(Canvas c, float cx, float cy, float s, int color, float dx) {
        c.save();
        c.translate(dx, 0f);
        Ui.R.set(cx - s * 0.2f, cy - s * 0.36f, cx + s * 0.2f, cy + s * 0.36f);
        c.drawRoundRect(Ui.R, s * 0.06f, s * 0.06f, Ui.stroke(color, s * 0.08f));
        c.drawLine(cx - s * 0.06f, cy - s * 0.26f, cx + s * 0.06f, cy - s * 0.26f, Ui.stroke(color, s * 0.06f));
        c.restore();
        float m = s * 0.38f;
        for (int i = -1; i <= 1; i += 2) {
            c.drawLine(cx + i * m, cy - s * 0.2f, cx + i * (m + s * 0.12f), cy - s * 0.2f, Ui.stroke(color, s * 0.07f));
            c.drawLine(cx + i * m, cy + s * 0.2f, cx + i * (m + s * 0.12f), cy + s * 0.2f, Ui.stroke(color, s * 0.07f));
        }
    }

    static void star(Canvas c, float cx, float cy, float r, int color, boolean filled) {
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

    // ---------- Bonus / malus ----------

    static void clock(Canvas c, float cx, float cy, float s, int color) {
        c.drawCircle(cx, cy, s * 0.38f, Ui.stroke(color, s * 0.09f));
        c.drawLine(cx, cy, cx, cy - s * 0.24f, Ui.stroke(color, s * 0.09f));
        c.drawLine(cx, cy, cx + s * 0.17f, cy + s * 0.08f, Ui.stroke(color, s * 0.09f));
    }

    static void fastClock(Canvas c, float cx, float cy, float s, int color) {
        clock(c, cx - s * 0.1f, cy, s * 0.85f, color);
        for (int i = 0; i < 2; i++) {
            float x = cx + s * 0.28f + i * s * 0.12f;
            Path p = Ui.PATH;
            p.reset();
            p.moveTo(x, cy - s * 0.14f);
            p.lineTo(x + s * 0.1f, cy);
            p.lineTo(x, cy + s * 0.14f);
            c.drawPath(p, Ui.stroke(color, s * 0.07f));
        }
    }

    static void spring(Canvas c, float cx, float cy, float s, int color) {
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

    static void magnet(Canvas c, float cx, float cy, float s, int color) {
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(cx - s * 0.3f, cy - s * 0.4f);
        p.lineTo(cx - s * 0.3f, cy + s * 0.05f);
        Ui.R.set(cx - s * 0.3f, cy - s * 0.25f, cx + s * 0.3f, cy + s * 0.35f);
        p.arcTo(Ui.R, 180, -180);
        p.lineTo(cx + s * 0.3f, cy - s * 0.4f);
        c.drawPath(p, Ui.stroke(color, s * 0.13f));
    }

    static void hammer(Canvas c, float cx, float cy, float s, int color) {
        c.save();
        c.rotate(35f, cx, cy);
        c.drawLine(cx, cy - s * 0.05f, cx, cy + s * 0.42f, Ui.stroke(color, s * 0.12f));
        Ui.roundRect(c, cx - s * 0.32f, cy - s * 0.40f, cx + s * 0.32f, cy - s * 0.08f, s * 0.06f, color);
        c.restore();
    }

    static void invert(Canvas c, float cx, float cy, float s, int color) {
        c.drawLine(cx - s * 0.38f, cy - s * 0.18f, cx + s * 0.38f, cy - s * 0.18f, Ui.stroke(color, s * 0.09f));
        Path a = Ui.PATH;
        a.reset();
        a.moveTo(cx + s * 0.2f, cy - s * 0.34f);
        a.lineTo(cx + s * 0.4f, cy - s * 0.18f);
        a.lineTo(cx + s * 0.2f, cy - s * 0.02f);
        c.drawPath(a, Ui.stroke(color, s * 0.09f));
        c.drawLine(cx - s * 0.38f, cy + s * 0.18f, cx + s * 0.38f, cy + s * 0.18f, Ui.stroke(color, s * 0.09f));
        a.reset();
        a.moveTo(cx - s * 0.2f, cy + s * 0.02f);
        a.lineTo(cx - s * 0.4f, cy + s * 0.18f);
        a.lineTo(cx - s * 0.2f, cy + s * 0.34f);
        c.drawPath(a, Ui.stroke(color, s * 0.09f));
    }

    static void snow(Canvas c, float cx, float cy, float s, int color) {
        for (int k = 0; k < 3; k++) {
            double a = Math.toRadians(k * 60);
            float dx = (float) Math.cos(a) * s * 0.4f, dy = (float) Math.sin(a) * s * 0.4f;
            c.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, Ui.stroke(color, s * 0.09f));
        }
    }

    static void bigBall(Canvas c, float cx, float cy, float s, int color) {
        c.drawCircle(cx, cy, s * 0.42f, Ui.stroke(color, s * 0.07f));
        c.drawCircle(cx, cy, s * 0.22f, Ui.fill(color));
    }

    static void question(Canvas c, float cx, float cy, float s, int color) {
        Ui.text(c, "?", cx, cy + s * 0.34f, s * 0.95f, color, true);
    }

    // ---------- Divers ----------

    static void ghost(Canvas c, float cx, float cy, float s, int color) {
        float l = cx - s * 0.3f, r = cx + s * 0.3f, top = cy - s * 0.38f, bot = cy + s * 0.38f;
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(l, bot);
        p.lineTo(l, cy - s * 0.08f);
        Ui.R.set(l, top, r, top + s * 0.6f);
        p.arcTo(Ui.R, 180, 180);
        p.lineTo(r, bot);
        float w3 = (r - l) / 3f;
        p.lineTo(r - w3 * 0.5f, bot - s * 0.1f);
        p.lineTo(r - w3, bot);
        p.lineTo(r - w3 * 1.5f, bot - s * 0.1f);
        p.lineTo(l + w3, bot);
        p.lineTo(l + w3 * 0.5f, bot - s * 0.1f);
        p.close();
        c.drawPath(p, Ui.stroke(color, s * 0.08f));
        c.drawCircle(cx - s * 0.12f, cy - s * 0.12f, s * 0.05f, Ui.fill(color));
        c.drawCircle(cx + s * 0.12f, cy - s * 0.12f, s * 0.05f, Ui.fill(color));
    }

    static void flag(Canvas c, float cx, float cy, float s, int color) {
        c.drawLine(cx - s * 0.25f, cy + s * 0.42f, cx - s * 0.25f, cy - s * 0.42f, Ui.stroke(color, s * 0.09f));
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(cx - s * 0.25f, cy - s * 0.40f);
        p.lineTo(cx + s * 0.38f, cy - s * 0.2f);
        p.lineTo(cx - s * 0.25f, cy);
        p.close();
        c.drawPath(p, Ui.fill(color));
    }

    static void lock(Canvas c, float cx, float cy, float s, int color) {
        Ui.roundRect(c, cx - s * 0.28f, cy - s * 0.02f, cx + s * 0.28f, cy + s * 0.40f, s * 0.06f, color);
        Ui.R.set(cx - s * 0.18f, cy - s * 0.40f, cx + s * 0.18f, cy + s * 0.12f);
        c.drawArc(Ui.R, 180, 180, false, Ui.stroke(color, s * 0.09f));
    }

    static void flash(Canvas c, float cx, float cy, float s, int color) {
        Path p = Ui.PATH;
        p.reset();
        p.moveTo(cx + s * 0.10f, cy - s * 0.45f);
        p.lineTo(cx - s * 0.25f, cy + s * 0.05f);
        p.lineTo(cx - s * 0.02f, cy + s * 0.05f);
        p.lineTo(cx - s * 0.10f, cy + s * 0.45f);
        p.lineTo(cx + s * 0.25f, cy - s * 0.08f);
        p.lineTo(cx + s * 0.02f, cy - s * 0.08f);
        p.close();
        c.drawPath(p, Ui.fill(color));
    }
}