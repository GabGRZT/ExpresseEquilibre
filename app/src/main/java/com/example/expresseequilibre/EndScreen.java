package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Locale;

/** Fin de partie : victoire, temps écoulé, verdict du fantôme, médaille, skin débloqué. */
final class EndScreen extends Screen {
    private static final float INPUT_LOCK = 0.7f;

    private final UiButton next = new UiButton("Niveau suivant", Ui.OK, null);
    private final UiButton again = new UiButton("Rejouer", Ui.BUTTON, null);
    private final UiButton levels = new UiButton("Niveaux", Ui.SLATE, null);
    private float age;
    private boolean hasNext;

    EndScreen(GameView g) { super(g); }

    @Override
    void onEnter() {
        age = 0f;
        next.onCancel();
        again.onCancel();
        levels.onCancel();
        if (g.lastUnlockedSkin >= 0) g.feedback.sfx(Sfx.UNLOCK);
    }

    @Override void update(float dt) { age += dt; }

    private static String fmt(float v) { return String.format(Locale.getDefault(), "%.1f", v); }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float lx = L + W * 0.27f, rx = L + W * 0.74f;

        boolean win = g.lastWin;
        boolean race = g.ghostMode && win && !Float.isNaN(g.lastGhostDelta);
        boolean beaten = race && g.lastGhostDelta > 0f;
        int main;
        String title, sub;
        if (!win) {
            main = Ui.DANGER;
            title = "Temps écoulé";
            sub = "Retente ta chance, tu y es presque !";
        } else if (race) {
            if (beaten) { main = Ui.OK; title = "Fantôme battu !"; sub = "Tu es plus rapide de " + fmt(g.lastGhostDelta) + " s"; }
            else { main = Ui.PURPLE; title = "Le fantôme gagne"; sub = "Il était plus rapide de " + fmt(-g.lastGhostDelta) + " s"; }
        } else {
            main = Ui.OK;
            title = "Victoire !";
            sub = "Sortie atteinte avec " + g.lastSeconds + " s d'avance";
        }

        if (win && (!race || beaten)) drawConfetti(c, h, L, W);

        Ui.title(c, title, lx, h * 0.22f, h * 0.14f, main, Paint.Align.CENTER, W * 0.48f);
        Ui.text(c, sub, lx, h * 0.31f, Math.max(Ui.sp(16), h * 0.045f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.48f);
        if (win) {
            String t2 = "Temps : " + fmt(g.lastRunTime) + " s" + (g.lastNewGhost ? "  ·  nouveau fantôme" : "");
            Ui.text(c, t2, lx, h * 0.38f, Math.max(Ui.sp(14), h * 0.04f), Ui.PURPLE, true, Paint.Align.CENTER, W * 0.48f);
        }

        float sr = h * 0.095f, sy = h * 0.57f;
        for (int i = 0; i < 3; i++) {
            float sx = lx + (i - 1) * sr * 2.7f;
            float k = Ui.clamp((age - (0.3f + i * 0.3f)) / 0.35f, 0f, 1f);
            boolean earned = i < g.lastStars && k > 0f;
            float scale = earned ? 1f + 0.35f * (float) Math.sin(k * Math.PI) : 1f;
            Icons.star(c, sx, sy, sr * scale, earned ? Ui.STAR : Ui.STAR_EMPTY, true);
            if (earned) Icons.star(c, sx, sy, sr * scale, Color.rgb(200, 130, 0), false);
        }

        if (g.lastUnlockedSkin >= 0) {
            float bw = W * 0.46f, by = h * 0.76f, bh = h * 0.12f;
            Ui.roundRect(c, lx - bw / 2f, by, lx + bw / 2f, by + bh, bh / 2f, Ui.PURPLE);
            Ui.text(c, "Nouveau skin : " + Skin.values()[g.lastUnlockedSkin].label + " !", lx, by + bh * 0.62f,
                    Math.max(Ui.sp(15), h * 0.042f), Color.WHITE, true, Paint.Align.CENTER, bw * 0.92f);
        }

        float p = Ui.clamp((age - 0.5f) / 0.9f, 0f, 1f);
        Ui.text(c, "SCORE", rx, h * 0.17f, Math.max(Ui.sp(14), h * 0.04f), Ui.PRIMARY, true);
        Ui.title(c, String.valueOf((int) (g.lastScore * p)), rx, h * 0.34f, h * 0.16f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.34f);
        float info = Math.max(Ui.sp(16), h * 0.043f);
        if (g.lastRecord) Ui.text(c, "Nouveau record !", rx, h * 0.42f, info * 1.1f, Ui.GOLD_TEXT, true);
        else Ui.text(c, "Meilleur score : " + g.bestScore, rx, h * 0.42f, info, Ui.TEXT, false);
        if (win && g.lastMedal > 0) {
            String[] names = {"", "Bronze", "Argent", "Or"};
            Icons.medal(c, rx - W * 0.06f, h * 0.495f, h * 0.10f, Ui.medalColor(g.lastMedal));
            Ui.text(c, "Médaille " + names[g.lastMedal], rx + W * 0.03f, h * 0.505f, info, Ui.TEXT, true,
                    Paint.Align.CENTER, W * 0.2f);
        }

        hasNext = win && !g.ghostMode && g.curLevel + 1 < Levels.ALL.length && g.progress.unlocked(g.curLevel + 1);
        float x0 = L + W * 0.55f, x1 = L + W * 0.93f, mid = x0 + (x1 - x0) * 0.5f, hg = Ui.dp(8);
        if (hasNext) {
            next.set(x0, h * 0.58f, x1, h * 0.74f);
            again.set(x0, h * 0.79f, mid - hg, h * 0.92f);
            levels.set(mid + hg, h * 0.79f, x1, h * 0.92f);
            next.draw(c);
        } else {
            again.set(x0, h * 0.58f, x1, h * 0.74f);
            levels.set(x0, h * 0.79f, x1, h * 0.92f);
        }
        again.draw(c);
        levels.draw(c);
    }

    private void drawConfetti(Canvas c, int h, float L, float W) {
        int[] pal = {Ui.STAR, Ui.BUTTON, Ui.OK, Ui.DANGER, Ui.PURPLE};
        for (int i = 0; i < 40; i++) {
            float fx = (i * 0.618f) % 1f;
            float sp = 0.25f + 0.2f * ((i * 0.77f) % 1f);
            float fy = (i * 0.381f + age * sp) % 1.1f;
            float x = L + fx * W, y = fy * h, s = h * 0.014f;
            Ui.roundRect(c, x, y, x + s, y + s * 1.6f, s * 0.2f, Ui.alpha(pal[i % pal.length], 200));
        }
    }

    @Override
    void onDown(float x, float y) {
        if (age < INPUT_LOCK) return;
        if (hasNext) next.onDown(x, y);
        again.onDown(x, y);
        levels.onDown(x, y);
    }

    @Override
    void onMove(float x, float y) {
        next.onMove(x, y);
        again.onMove(x, y);
        levels.onMove(x, y);
    }

    @Override
    void onUp(float x, float y) {
        if (hasNext && next.onUp(x, y)) { g.startLevel(g.curLevel + 1); return; }
        if (again.onUp(x, y)) g.startLevel(g.curLevel);   // repasse par l'avertissement si niveau clignotant
        else if (levels.onUp(x, y)) g.goLevels(g.ghostMode);
    }

    @Override
    void onCancel() {
        next.onCancel();
        again.onCancel();
        levels.onCancel();
    }
}