package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class PauseScreen extends Screen {
    private final UiButton resume = new UiButton("Reprendre", Ui.BUTTON, Icons::play);
    private final UiButton restart = new UiButton("Recommencer", Ui.SLATE, null);
    private final UiButton home = new UiButton("Accueil", Ui.SLATE, null);

    PauseScreen(GameView g) {
        super(g);
        resume.horizontal = true;
    }

    @Override
    void draw(Canvas c, int w, int h) {
        g.playScreen.drawWorld(c, w, h);          // le plateau reste visible, assombri
        Ui.dim(c, w, h, Ui.alpha(Ui.PRIMARY, 225));

        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float lx = L + W * 0.28f;
        float fs = Math.max(Ui.sp(17), h * 0.05f);

        Ui.text(c, "PAUSE", lx, h * 0.42f, h * 0.20f, Color.WHITE, true, Paint.Align.CENTER, W * 0.45f);
        Ui.text(c, "Temps restant : " + (int) Math.ceil(g.playScreen.timeLeft()) + " s",
                lx, h * 0.55f, fs, Color.WHITE, false, Paint.Align.CENTER, W * 0.45f);
        Ui.text(c, "Étoiles : " + g.playScreen.starCount() + "/3",
                lx, h * 0.62f, fs, Color.WHITE, false, Paint.Align.CENTER, W * 0.45f);

        float x0 = L + W * 0.56f, x1 = L + W * 0.93f;
        resume.set(x0, h * 0.16f, x1, h * 0.38f);
        restart.set(x0, h * 0.46f, x1, h * 0.64f);
        home.set(x0, h * 0.72f, x1, h * 0.90f);
        resume.draw(c);
        restart.draw(c);
        home.draw(c);

        Ui.text(c, "Un compte à rebours précède la reprise.", lx, h * 0.82f,
                Math.max(Ui.sp(14), h * 0.04f), Ui.alpha(Color.WHITE, 220), false, Paint.Align.CENTER, W * 0.45f);
    }

    @Override void onDown(float x, float y) { resume.onDown(x, y); restart.onDown(x, y); home.onDown(x, y); }

    @Override void onMove(float x, float y) { resume.onMove(x, y); restart.onMove(x, y); home.onMove(x, y); }

    @Override
    void onUp(float x, float y) {
        if (resume.onUp(x, y)) g.resumeGame();
        else if (restart.onUp(x, y)) g.beginPlay();
        else if (home.onUp(x, y)) g.goStart();
    }

    @Override void onCancel() { resume.onCancel(); restart.onCancel(); home.onCancel(); }
}