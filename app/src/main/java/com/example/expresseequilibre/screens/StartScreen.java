package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public final class StartScreen extends Screen {
    private final UiButton play = new UiButton("Jouer", Ui.BUTTON, Icons::play);
    private final UiButton help = new UiButton("Aide", Ui.SLATE, null);
    private final UiButton settings = new UiButton("Réglages", Ui.SLATE, null);
    private float t;

    public StartScreen(GameView g) {
        super(g);
        play.horizontal = true;
    }

    @Override public void onEnter() { t = 0f; }

    @Override public void update(float dt) { t += dt; }

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float lx = L + W * 0.30f; // centre de la colonne gauche

        Ui.text(c, "ÉQUILIBRE", lx, h * 0.26f, h * 0.17f, Ui.PRIMARY, true, Paint.Align.CENTER, W * 0.50f);
        Ui.text(c, "EXPRESS", lx, h * 0.42f, h * 0.17f, Ui.BUTTON, true, Paint.Align.CENTER, W * 0.50f);

        float info = Math.max(Ui.sp(16), h * 0.045f);
        if (!g.sensors.hasAccelerometer()) {
            Ui.text(c, "Capteur d'inclinaison introuvable", lx, h * 0.51f, info, Ui.DANGER, true, Paint.Align.CENTER, W * 0.5f);
        } else if (g.bestScore > 0) {
            Ui.text(c, "Meilleur score : " + g.bestScore, lx, h * 0.51f, info, Ui.PRIMARY, true, Paint.Align.CENTER, W * 0.5f);
        }

        // Petite bille qui se balance.
        float bx = lx + (float) Math.sin(t * 1.4f) * W * 0.18f;
        float by = h * 0.60f, r = h * 0.035f;
        Ui.R.set(bx - r, by + r * 0.75f, bx + r, by + r * 1.25f);
        c.drawOval(Ui.R, Ui.fill(Ui.alpha(Color.BLACK, 45)));
        Ui.ball(c, bx, by, r);

        // Les trois gestes.
        float s = h * 0.16f, iy = h * 0.78f;
        Icons.tiltAnim(c, lx - W * 0.17f, iy, s, Ui.PRIMARY, (float) Math.sin(t * 2.2f) * 18f);
        Icons.shakeAnim(c, lx, iy, s, Ui.PRIMARY, (float) Math.sin(t * 14f) * s * 0.06f);
        Icons.wind(c, lx + W * 0.17f, iy, s, Ui.PRIMARY);
        float ls = Math.max(Ui.sp(14), h * 0.04f), ly = iy + s * 0.85f;
        Ui.text(c, "Incliner", lx - W * 0.17f, ly, ls, Ui.TEXT, true);
        Ui.text(c, "Secouer", lx, ly, ls, Ui.TEXT, true);
        Ui.text(c, "Souffler", lx + W * 0.17f, ly, ls, Ui.TEXT, true);

        // Boutons à droite, empilés.
        float x0 = L + W * 0.60f, x1 = L + W * 0.94f;
        play.set(x0, h * 0.14f, x1, h * 0.40f);
        help.set(x0, h * 0.50f, x1, h * 0.68f);
        settings.set(x0, h * 0.74f, x1, h * 0.92f);
        play.draw(c);
        help.draw(c);
        settings.draw(c);
    }

    @Override public void onDown(float x, float y) { play.onDown(x, y); help.onDown(x, y); settings.onDown(x, y); }

    @Override public void onMove(float x, float y) { play.onMove(x, y); help.onMove(x, y); settings.onMove(x, y); }

    @Override
    public void onUp(float x, float y) {
        if (play.onUp(x, y)) g.onPlayPressed();
        else if (help.onUp(x, y)) g.goTutorial();
        else if (settings.onUp(x, y)) g.goSettings();
    }

    @Override public void onCancel() { play.onCancel(); help.onCancel(); settings.onCancel(); }
}
