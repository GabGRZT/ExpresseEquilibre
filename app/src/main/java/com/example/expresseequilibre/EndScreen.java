package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Écran de fin (victoire ou temps écoulé). */
final class EndScreen extends Screen {
    private static final float INPUT_LOCK = 0.7f; // évite un "Rejouer" involontaire

    private final UiButton again = new UiButton("Rejouer", Ui.BUTTON, null);
    private final UiButton home = new UiButton("Accueil", Ui.SLATE, null);
    private float age;

    EndScreen(GameView g) { super(g); }

    @Override void onEnter() { age = 0f; again.onCancel(); home.onCancel(); }

    @Override void update(float dt) { age += dt; }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float lx = L + W * 0.27f, rx = L + W * 0.74f;
        boolean win = g.lastWin;
        int main = win ? Ui.OK : Ui.DANGER;

        // Colonne gauche : verdict + étoiles
        Ui.text(c, win ? "Victoire !" : "Temps écoulé", lx, h * 0.24f, h * 0.15f, main, true, Paint.Align.CENTER, W * 0.48f);
        String sub = win ? "Sortie atteinte avec " + g.lastSeconds + " s d'avance" : "Retente ta chance, tu y es presque !";
        Ui.text(c, sub, lx, h * 0.33f, Math.max(Ui.sp(16), h * 0.045f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.48f);

        float sr = h * 0.10f, sy = h * 0.58f;
        for (int i = 0; i < 3; i++) {
            float sx = lx + (i - 1) * sr * 2.7f;
            float k = Ui.clamp((age - (0.3f + i * 0.3f)) / 0.35f, 0f, 1f);
            boolean earned = i < g.lastStars && k > 0f;
            float scale = earned ? 1f + 0.35f * (float) Math.sin(k * Math.PI) : 1f;
            Icons.star(c, sx, sy, sr * scale, earned ? Ui.STAR : Ui.STAR_EMPTY, true);
            if (earned) Icons.star(c, sx, sy, sr * scale, Color.rgb(200, 130, 0), false);
        }

        // Colonne droite : score + boutons
        float p = Ui.clamp((age - 0.5f) / 0.9f, 0f, 1f);
        Ui.text(c, "SCORE", rx, h * 0.22f, Math.max(Ui.sp(14), h * 0.04f), Ui.PRIMARY, true);
        Ui.text(c, String.valueOf((int) (g.lastScore * p)), rx, h * 0.42f, h * 0.19f, Ui.PRIMARY, true);

        float info = Math.max(Ui.sp(16), h * 0.045f);
        if (g.lastRecord) Ui.text(c, "Nouveau record !", rx, h * 0.51f, info * 1.1f, Ui.GOLD_TEXT, true);
        else Ui.text(c, "Meilleur score : " + g.bestScore, rx, h * 0.51f, info, Ui.TEXT, false);

        again.set(L + W * 0.55f, h * 0.60f, L + W * 0.93f, h * 0.78f);
        home.set(L + W * 0.62f, h * 0.84f, L + W * 0.86f, h * 0.94f);
        again.draw(c);
        home.draw(c);
    }

    @Override
    void onDown(float x, float y) {
        if (age < INPUT_LOCK) return;
        again.onDown(x, y);
        home.onDown(x, y);
    }

    @Override void onMove(float x, float y) { again.onMove(x, y); home.onMove(x, y); }

    @Override
    void onUp(float x, float y) {
        if (again.onUp(x, y)) g.beginPlay();
        else if (home.onUp(x, y)) g.goStart();
    }

    @Override void onCancel() { again.onCancel(); home.onCancel(); }
}