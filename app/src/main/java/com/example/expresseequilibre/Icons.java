package com.example.expresseequilibre;

import android.graphics.Canvas;
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
        android.graphics.Paint p = Ui.stroke(color, s * 0.14f);
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
}