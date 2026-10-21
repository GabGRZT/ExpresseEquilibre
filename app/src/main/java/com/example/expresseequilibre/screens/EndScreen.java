package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.game.Levels;
import com.example.expresseequilibre.game.RunResult;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Locale;


/** Écran de fin : victoire ou temps écoulé, étoiles, score. */
public final class EndScreen extends Screen {
    private static final float INPUT_LOCK = 0.7f; // évite un appui involontaire (doigt encore sur un bouton de jeu)

    private final UiButton next = add("Niveau suivant", Ui.OK, null, () -> g.startLevel(g.curLevel + 1));
    private final UiButton again = add("Rejouer", Ui.BUTTON, null, g::beginPlay);
    private final UiButton levels = add("Niveaux", Ui.SLATE, null, g::goLevels);
    private float age;

    public EndScreen(GameView g) { super(g); }

    @Override
    public void onEnter() {
        age = 0f;
        super.onEnter();
    }

    @Override public void update(float dt) { age += dt; }

    @Override
    public void onDown(float x, float y) {
        if (age >= INPUT_LOCK) super.onDown(x, y);
    }

    private static String seconds(float v) { return String.format(Locale.getDefault(), "%.1f", v); }

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        RunResult run = g.lastRun;
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float leftX = L + W * 0.27f, rightX = L + W * 0.74f;

        if (run.win) drawConfetti(c, h, L, W);
        drawVerdict(c, run, leftX, h, W);
        drawStars(c, run, leftX, h);
        drawScore(c, run, rightX, h, W);
        drawButtons(c, run, h, L, W);
    }

    private void drawVerdict(Canvas c, RunResult run, float x, int h, float W) {
        int color = run.win ? Ui.OK : Ui.DANGER;
        String title = run.win ? "Victoire !" : "Temps écoulé";
        String sub = run.win ? "Sortie atteinte avec " + run.secondsLeft + " s d'avance" : "Retente ta chance, tu y es presque !";
        Ui.title(c, title, x, h * 0.24f, h * 0.14f, color, Paint.Align.CENTER, W * 0.48f);
        Ui.text(c, sub, x, h * 0.33f, Math.max(Ui.sp(16), h * 0.045f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.48f);
        if (run.win) {
            Ui.text(c, "Temps : " + seconds(run.time) + " s", x, h * 0.40f, Math.max(Ui.sp(14), h * 0.04f),
                    Ui.PURPLE, true, Paint.Align.CENTER, W * 0.48f);
        }
    }

    /** Les étoiles gagnées apparaissent l'une après l'autre en grossissant un instant. */
    private void drawStars(Canvas c, RunResult run, float x, int h) {
        float radius = h * 0.10f, y = h * 0.62f;
        for (int i = 0; i < 3; i++) {
            float k = Ui.clamp((age - (0.3f + i * 0.3f)) / 0.35f, 0f, 1f);
            boolean earned = i < run.stars && k > 0f;
            float scale = earned ? 1f + 0.35f * (float) Math.sin(k * Math.PI) : 1f;
            Icons.rating(c, x + (i - 1) * radius * 2.7f, y, radius * scale, earned);
        }
    }

    private void drawScore(Canvas c, RunResult run, float x, int h, float W) {
        float progress = Ui.clamp((age - 0.5f) / 0.9f, 0f, 1f); // le score défile jusqu'à sa valeur
        Ui.text(c, "SCORE", x, h * 0.18f, Math.max(Ui.sp(14), h * 0.04f), Ui.PRIMARY, true);
        Ui.title(c, String.valueOf((int) (run.score * progress)), x, h * 0.36f, h * 0.17f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.38f);
        float info = Math.max(Ui.sp(16), h * 0.043f);
        if (run.newRecord) Ui.text(c, "Nouveau record !", x, h * 0.44f, info * 1.1f, Ui.GOLD_TEXT, true);
        else Ui.text(c, "Meilleur score : " + g.bestScore, x, h * 0.44f, info, Ui.TEXT, false);
    }

    private void drawButtons(Canvas c, RunResult run, int h, float L, float W) {
        next.enabled = run.win && g.curLevel + 1 < Levels.ALL.length;
        float x0 = L + W * 0.55f, x1 = L + W * 0.93f, mid = (x0 + x1) / 2f, gap = Ui.dp(8);
        if (next.enabled) {
            next.set(x0, h * 0.52f, x1, h * 0.70f);
            again.set(x0, h * 0.76f, mid - gap, h * 0.90f);
            levels.set(mid + gap, h * 0.76f, x1, h * 0.90f);
            next.draw(c);
        } else {
            again.set(x0, h * 0.54f, x1, h * 0.72f);
            levels.set(x0, h * 0.78f, x1, h * 0.92f);
        }
        again.draw(c);
        levels.draw(c);
    }

    private void drawConfetti(Canvas c, int h, float L, float W) {
        int[] palette = {Ui.STAR, Ui.BUTTON, Ui.OK, Ui.DANGER, Ui.PURPLE};
        for (int i = 0; i < 40; i++) {
            float x = L + ((i * 0.618f) % 1f) * W;
            float y = ((i * 0.381f + age * (0.25f + 0.2f * ((i * 0.77f) % 1f))) % 1.1f) * h;
            float s = h * 0.014f;
            Ui.roundRect(c, x, y, x + s, y + s * 1.6f, s * 0.2f, Ui.alpha(palette[i % palette.length], 200));
        }
    }
}
