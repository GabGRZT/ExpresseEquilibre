package com.example.expresseequilibre.game;

import com.example.expresseequilibre.ui.Ui;
import android.graphics.Canvas;

import java.util.Random;


/** Effets visuels éphémères : anneaux, particules, message (toast), secousse d'écran et flash rouge. */
public final class Effects {
    private static final int MAX_RINGS = 6, MAX_PARTICLES = 80;
    private static final float RING_LIFE = 0.5f, TOAST_TIME = 1.4f, TOAST_FADE = 0.4f;
    private static final float TWO_PI = 6.2832f;

    private static final class Ring { float x, y, life; int color; }

    private static final class Particle { float x, y, vx, vy, life, maxLife, size; int color; }

    public float time;        // horloge d'animation (s), jamais remise à zéro
    public float shake;       // 1 -> 0 : intensité de la secousse d'écran
    public float flash;       // 1 -> 0 : flash rouge (chute dans un trou)

    private String toastText = "";
    private int toastColor = Ui.OK;
    private float toastLeft;

    private final Ring[] rings = new Ring[MAX_RINGS];
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private int nextRing, nextParticle;
    private final Random rnd = new Random();

    public Effects() {
        for (int i = 0; i < MAX_RINGS; i++) rings[i] = new Ring();
        for (int i = 0; i < MAX_PARTICLES; i++) particles[i] = new Particle();
    }

    public void reset() {
        shake = flash = toastLeft = 0f;
        for (Ring r : rings) r.life = 0f;
        for (Particle p : particles) p.life = 0f;
    }

    public void update(float dt) {
        time += dt;
        flash = Math.max(0f, flash - dt * 2.5f);
        shake = Math.max(0f, shake - dt * 3f);
        toastLeft = Math.max(0f, toastLeft - dt);
        for (Ring r : rings) r.life = Math.max(0f, r.life - dt);

        float drag = Math.max(0f, 1f - 2.5f * dt);
        for (Particle p : particles) {
            if (p.life <= 0f) continue;
            p.life -= dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= drag;
            p.vy *= drag;
        }
    }

    // ---------- Création ----------

    public void ring(float x, float y, int color) {
        Ring r = rings[nextRing];
        nextRing = (nextRing + 1) % MAX_RINGS;
        r.x = x;
        r.y = y;
        r.color = color;
        r.life = RING_LIFE;
    }

    private void spawn(float x, float y, float vx, float vy, float life, float size, int color) {
        Particle p = particles[nextParticle];
        nextParticle = (nextParticle + 1) % MAX_PARTICLES;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = p.maxLife = life;
        p.size = size;
        p.color = color;
    }

    /** Gerbe de n particules parties de (x, y) dans toutes les directions. */
    public void burst(float x, float y, int color, int n, float speed, float size) {
        for (int i = 0; i < n; i++) {
            float angle = rnd.nextFloat() * TWO_PI;
            float v = speed * (0.4f + 0.6f * rnd.nextFloat());
            spawn(x, y, (float) Math.cos(angle) * v, (float) Math.sin(angle) * v, 0.5f + 0.4f * rnd.nextFloat(), size, color);
        }
    }

    /** Éclats d'un mur brisé : des morceaux jaillissent de toute sa surface. */
    public void debris(Wall wall, int color) {
        for (int k = 0; k < 20; k++) {
            float x = wall.x + rnd.nextFloat() * wall.w, y = wall.y + rnd.nextFloat() * wall.h;
            float angle = rnd.nextFloat() * TWO_PI, v = 120f + rnd.nextFloat() * 260f;
            spawn(x, y, (float) Math.cos(angle) * v, (float) Math.sin(angle) * v, 0.6f + rnd.nextFloat() * 0.5f, 12f, color);
        }
        ring(wall.x + wall.w / 2f, wall.y + wall.h / 2f, color);
    }

    public void toast(String text, int color) {
        toastText = text;
        toastColor = color;
        toastLeft = TOAST_TIME;
    }

    // ---------- Lecture / dessin ----------

    public String toastText() { return toastText; }

    public int toastColor() { return toastColor; }

    /** Opacité du message : 0 = invisible, 1 = plein (fondu sur la fin). */
    public float toastOpacity() { return Math.min(1f, toastLeft / TOAST_FADE); }

    /** Dessine anneaux et particules en coordonnées logiques (px = taille d'un pixel écran dans ce repère). */
    public void draw(Canvas c, float px) {
        for (Ring r : rings) {
            if (r.life <= 0f) continue;
            float p = 1f - r.life / RING_LIFE;
            c.drawCircle(r.x, r.y, GameWorld.BALL_R + p * GameWorld.BALL_R * 2.6f,
                    Ui.stroke(Ui.alpha(r.color, (int) (220 * (1f - p))), Ui.dp(4) * px));
        }
        for (Particle p : particles) {
            if (p.life <= 0f) continue;
            int alpha = (int) (255 * Math.max(0f, p.life / p.maxLife));
            float half = p.size / 2f;
            c.drawRect(p.x - half, p.y - half, p.x + half, p.y + half, Ui.fill(Ui.alpha(p.color, alpha)));
        }
    }
}
