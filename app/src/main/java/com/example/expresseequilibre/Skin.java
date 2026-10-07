package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;

/** Apparences de la bille (purement cosmétiques : la physique ne change pas). Dessinées au Canvas. */
enum Skin {
    CLASSIC("Classique", "Simple et efficace", 0, 1.00f, Ui.BALL, 0),
    CRYSTAL("Orbe cristal", "Des étoiles prisonnières", 3, 1.30f, Color.rgb(77, 208, 225), 3),
    NINJA("Ombre ninja", "Silencieux comme la fumée", 8, 0.85f, Color.rgb(120, 134, 143), 1),
    FOX("Esprit renard", "Flammes de kitsune", 14, 1.15f, Color.rgb(255, 138, 61), 2),
    AURA("Super-aura", "Une énergie sans limite", 20, 0.70f, Color.rgb(255, 213, 79), 3),
    MECHA("Mécha", "Néon et acier", 28, 1.50f, Color.rgb(0, 229, 255), 4);

    final String label, tagline;
    final int unlock, color, emit;   // emit : 0 rien, 1 fumée, 2 flammes, 3 étincelles, 4 néon
    final float pitch;               // hauteur du son de saut

    Skin(String label, String tagline, int unlock, float pitch, int color, int emit) {
        this.label = label;
        this.tagline = tagline;
        this.unlock = unlock;
        this.pitch = pitch;
        this.color = color;
        this.emit = emit;
    }

    /** speed : 0..1 (vitesse relative). */
    void draw(Canvas c, float x, float y, float r, float t, float speed) {
        switch (this) {
            case CRYSTAL: crystal(c, x, y, r, t); break;
            case NINJA: ninja(c, x, y, r, t); break;
            case FOX: fox(c, x, y, r, t); break;
            case AURA: aura(c, x, y, r, t, speed); break;
            case MECHA: mecha(c, x, y, r, t); break;
            default: Ui.ball(c, x, y, r); break;
        }
    }

    private static void crystal(Canvas c, float x, float y, float r, float t) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3f);
        c.drawCircle(x, y, r * (1.45f + 0.1f * pulse), Ui.fill(Ui.alpha(Color.rgb(77, 208, 225), 45)));
        c.drawCircle(x, y, r, Ui.fill(Color.argb(215, 128, 222, 234)));
        c.drawCircle(x, y, r * 0.96f, Ui.stroke(Ui.alpha(Color.WHITE, 210), r * 0.06f));
        for (int k = 0; k < 4; k++) {
            double a = t * 1.6 + k * Math.PI / 2;
            Icons.star(c, x + (float) Math.cos(a) * r * 0.5f, y + (float) Math.sin(a) * r * 0.5f, r * 0.2f, Color.WHITE, true);
        }
        c.drawCircle(x - r * 0.4f, y - r * 0.4f, r * 0.2f, Ui.fill(Ui.alpha(Color.WHITE, 200)));
    }

    private static void ninja(Canvas c, float x, float y, float r, float t) {
        int red = Color.rgb(211, 47, 47);
        float w1 = (float) Math.sin(t * 9f) * r * 0.25f, w2 = (float) Math.sin(t * 9f + 1.3f) * r * 0.25f;
        c.drawLine(x - r * 0.9f, y - r * 0.3f, x - r * 1.7f, y - r * 0.15f + w1, Ui.stroke(red, r * 0.22f));
        c.drawLine(x - r * 0.9f, y - r * 0.3f, x - r * 1.6f, y - r * 0.45f + w2, Ui.stroke(red, r * 0.22f));
        c.drawCircle(x, y, r, Ui.fill(Color.rgb(38, 50, 56)));
        c.drawCircle(x, y, r * 0.97f, Ui.stroke(Color.rgb(84, 110, 122), r * 0.06f));
        c.save();
        Ui.PATH.reset();
        Ui.PATH.addCircle(x, y, r, Path.Direction.CW);
        c.clipPath(Ui.PATH);
        c.drawRect(x - r, y - r * 0.5f, x + r, y - r * 0.08f, Ui.fill(red));
        c.restore();
        Ui.roundRect(c, x - r * 0.3f, y - r * 0.44f, x + r * 0.3f, y - r * 0.14f, r * 0.06f, Color.rgb(176, 190, 197));
        Ui.roundRect(c, x - r * 0.52f, y + r * 0.12f, x - r * 0.12f, y + r * 0.3f, r * 0.08f, Color.WHITE);
        Ui.roundRect(c, x + r * 0.12f, y + r * 0.12f, x + r * 0.52f, y + r * 0.3f, r * 0.08f, Color.WHITE);
        c.drawCircle(x - r * 0.3f, y + r * 0.21f, r * 0.06f, Ui.fill(Color.BLACK));
        c.drawCircle(x + r * 0.3f, y + r * 0.21f, r * 0.06f, Ui.fill(Color.BLACK));
    }

    private static void fox(Canvas c, float x, float y, float r, float t) {
        int body = Color.rgb(255, 112, 67);
        c.drawCircle(x, y, r * (1.35f + 0.08f * (float) Math.sin(t * 6f)), Ui.fill(Ui.alpha(Color.rgb(255, 138, 61), 55)));
        Path p = Ui.PATH;
        for (int s = -1; s <= 1; s += 2) {
            p.reset();
            p.moveTo(x + s * r * 0.85f, y - r * 0.2f);
            p.lineTo(x + s * r * 0.62f, y - r * 1.45f);
            p.lineTo(x + s * r * 0.05f, y - r * 0.8f);
            p.close();
            c.drawPath(p, Ui.fill(body));
            p.reset();
            p.moveTo(x + s * r * 0.72f, y - r * 0.45f);
            p.lineTo(x + s * r * 0.6f, y - r * 1.12f);
            p.lineTo(x + s * r * 0.3f, y - r * 0.78f);
            p.close();
            c.drawPath(p, Ui.fill(Color.rgb(255, 205, 210)));
        }
        c.drawCircle(x, y, r, Ui.fill(body));
        c.drawCircle(x, y, r * 0.97f, Ui.stroke(Color.rgb(191, 54, 12), r * 0.06f));
        Ui.R.set(x - r * 0.55f, y + r * 0.05f, x + r * 0.55f, y + r * 0.82f);
        c.drawOval(Ui.R, Ui.fill(Color.WHITE));
        c.drawCircle(x - r * 0.35f, y - r * 0.1f, r * 0.1f, Ui.fill(Color.rgb(38, 50, 56)));
        c.drawCircle(x + r * 0.35f, y - r * 0.1f, r * 0.1f, Ui.fill(Color.rgb(38, 50, 56)));
        c.drawCircle(x, y + r * 0.26f, r * 0.08f, Ui.fill(Color.rgb(38, 50, 56)));
    }

    private static void aura(Canvas c, float x, float y, float r, float t, float speed) {
        float k = 1f + speed * 0.5f;
        Path p = Ui.PATH;
        p.reset();
        int n = 12;
        for (int i = 0; i < n * 2; i++) {
            double a = i * Math.PI / n + t * 0.5;
            float rad = (i % 2 == 0) ? r * (1.5f + 0.3f * (float) Math.sin(t * 9f + i * 1.7f)) * k : r * 1.1f;
            float px = x + (float) Math.cos(a) * rad, py = y + (float) Math.sin(a) * rad;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.close();
        c.drawPath(p, Ui.fill(Ui.alpha(Color.rgb(255, 160, 0), 110)));
        c.drawCircle(x, y, r * 1.2f, Ui.fill(Ui.alpha(Color.rgb(255, 235, 59), 120)));
        c.drawCircle(x, y, r, Ui.fill(Color.rgb(255, 213, 79)));
        c.drawCircle(x, y, r * 0.97f, Ui.stroke(Color.rgb(255, 160, 0), r * 0.08f));
        c.drawCircle(x - r * 0.35f, y - r * 0.35f, r * 0.24f, Ui.fill(Ui.alpha(Color.WHITE, 230)));
        if (speed > 0.45f) {
            for (int k2 = 0; k2 < 3; k2++) {
                double a0 = t * 3 + k2 * 2.1;
                float jx = (float) Math.sin(t * 14f + k2) * r * 0.25f, jy = (float) Math.cos(t * 13f + k2 * 2f) * r * 0.25f;
                Path q = Ui.PATH;
                q.reset();
                q.moveTo(x + (float) Math.cos(a0) * r * 1.15f, y + (float) Math.sin(a0) * r * 1.15f);
                q.lineTo(x + (float) Math.cos(a0) * r * 1.5f + jx, y + (float) Math.sin(a0) * r * 1.5f + jy);
                q.lineTo(x + (float) Math.cos(a0) * r * 1.9f - jx, y + (float) Math.sin(a0) * r * 1.9f - jy);
                c.drawPath(q, Ui.stroke(Color.rgb(255, 255, 200), r * 0.08f));
            }
        }
    }

    private static void mecha(Canvas c, float x, float y, float r, float t) {
        int neon = Color.rgb(0, 229, 255);
        c.drawCircle(x, y, r * 1.3f, Ui.fill(Ui.alpha(neon, 40)));
        c.drawCircle(x, y, r, Ui.fill(Color.rgb(176, 190, 197)));
        c.drawCircle(x, y, r * 0.96f, Ui.stroke(Color.rgb(96, 125, 139), r * 0.08f));
        c.drawCircle(x, y, r * 0.72f, Ui.stroke(Ui.alpha(neon, 200), r * 0.08f));
        Ui.roundRect(c, x - r * 0.62f, y - r * 0.2f, x + r * 0.62f, y + r * 0.14f, r * 0.12f, Color.rgb(38, 50, 56));
        float s = 0.5f + 0.5f * (float) Math.sin(t * 4f);
        Ui.roundRect(c, x - r * 0.5f, y - r * 0.08f, x - r * 0.5f + r * (0.4f + 0.6f * s), y + r * 0.02f, r * 0.05f, neon);
        c.drawCircle(x - r * 0.4f, y + r * 0.55f, r * 0.06f, Ui.fill(Color.rgb(96, 125, 139)));
        c.drawCircle(x + r * 0.4f, y + r * 0.55f, r * 0.06f, Ui.fill(Color.rgb(96, 125, 139)));
        c.drawCircle(x - r * 0.4f, y - r * 0.45f, r * 0.16f, Ui.fill(Ui.alpha(Color.WHITE, 190)));
    }
}