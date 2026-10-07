package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Locale;

/** Avertissement obligatoire avant un niveau stroboscopique (photosensibilité). */
final class WarnScreen extends Screen {
    private static final float LOCK = 0.8f; // évite de valider par réflexe

    private final UiButton go = new UiButton("Je comprends, je joue", Ui.ORANGE, null);
    private final UiButton reduced = new UiButton("Jouer en effets réduits", Ui.OK, null);
    private final UiButton back = new UiButton("Retour", Ui.SLATE, null);
    private float age;

    WarnScreen(GameView g) { super(g); }

    @Override
    void onEnter() {
        age = 0f;
        go.onCancel();
        reduced.onCancel();
        back.onCancel();
    }

    @Override void update(float dt) { age += dt; }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        Level lv = Levels.ALL[g.curLevel];
        float lx = L + W * 0.2f, rx = L + W * 0.62f;

        c.drawCircle(lx, h * 0.36f, h * 0.17f, Ui.fill(Ui.ORANGE));
        Icons.flash(c, lx, h * 0.36f, h * 0.24f, Color.WHITE);
        Ui.title(c, "ATTENTION", lx, h * 0.68f, h * 0.085f, Ui.DANGER, Paint.Align.CENTER, W * 0.34f);
        Ui.text(c, lv.name, lx, h * 0.76f, Math.max(Ui.sp(16), h * 0.045f), Ui.TEXT, true, Paint.Align.CENTER, W * 0.34f);

        float fs = Math.max(Ui.sp(16), h * 0.044f), ly = h * 0.20f, step = fs * 1.6f, mw = W * 0.5f;
        String hz = String.format(Locale.getDefault(), "%.1f", Math.min(lv.strobeHz, 2.5f));
        Ui.text(c, "Ce niveau contient des lumières qui clignotent", rx, ly, fs, Ui.TEXT, false, Paint.Align.CENTER, mw);
        Ui.text(c, "(jusqu'à " + hz + " flash par seconde).", rx, ly + step, fs, Ui.TEXT, true, Paint.Align.CENTER, mw);
        Ui.text(c, "Cela peut provoquer des crises chez les personnes", rx, ly + step * 2.2f, fs, Ui.TEXT, false, Paint.Align.CENTER, mw);
        Ui.text(c, "photosensibles. Arrête de jouer au moindre malaise.", rx, ly + step * 3.2f, fs, Ui.TEXT, false, Paint.Align.CENTER, mw);
        Ui.text(c, "Les effets réduits remplacent les flashs par un fondu doux.", rx, ly + step * 4.4f, fs, Ui.OK, true, Paint.Align.CENTER, mw);

        go.set(L + W * 0.40f, h * 0.55f, L + W * 0.84f, h * 0.69f);
        reduced.set(L + W * 0.40f, h * 0.73f, L + W * 0.84f, h * 0.87f);
        back.set(L + W * 0.04f, h * 0.88f, L + W * 0.24f, h * 0.96f);
        go.draw(c);
        reduced.draw(c);
        back.draw(c);
    }

    @Override
    void onDown(float x, float y) {
        if (age < LOCK) return;
        go.onDown(x, y);
        reduced.onDown(x, y);
        back.onDown(x, y);
    }

    @Override void onMove(float x, float y) { go.onMove(x, y); reduced.onMove(x, y); back.onMove(x, y); }

    @Override
    void onUp(float x, float y) {
        if (go.onUp(x, y)) g.proceedAfterWarning();
        else if (reduced.onUp(x, y)) {
            g.progress.setReducedFx(true);
            g.proceedAfterWarning();
        } else if (back.onUp(x, y)) g.goLevels(g.ghostMode);
    }

    @Override void onCancel() { go.onCancel(); reduced.onCancel(); back.onCancel(); }
}