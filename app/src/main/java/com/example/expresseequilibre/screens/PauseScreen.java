package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public final class PauseScreen extends Screen {
    private final UiButton resume = add("Reprendre", Ui.BUTTON, Icons::play, g::resumeGame);
    private final UiButton restart = add("Recommencer", Ui.SLATE, null, g::beginPlay);
    private final UiButton settings = add("Réglages", Ui.SLATE, null, g::goSettingsFromPause);
    private final UiButton home = add("Accueil", Ui.SLATE, null, g::goStart);

    public PauseScreen(GameView g) {
        super(g);
        resume.horizontal = true;
    }

    @Override
    public void draw(Canvas c, int w, int h) {
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
        Ui.text(c, "Un compte à rebours précède la reprise.", lx, h * 0.82f,
                Math.max(Ui.sp(14), h * 0.04f), Ui.alpha(Color.WHITE, 220), false, Paint.Align.CENTER, W * 0.45f);

        float x0 = L + W * 0.56f, x1 = L + W * 0.93f;
        resume.set(x0, h * 0.07f, x1, h * 0.29f);
        restart.set(x0, h * 0.35f, x1, h * 0.52f);
        settings.set(x0, h * 0.58f, x1, h * 0.75f);
        home.set(x0, h * 0.81f, x1, h * 0.96f);
        resume.draw(c);
        restart.draw(c);
        settings.draw(c);
        home.draw(c);
    }
}
