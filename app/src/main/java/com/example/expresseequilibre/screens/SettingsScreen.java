package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import com.example.expresseequilibre.ui.UiSlider;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Réglages : contrôle de la bille, côté de la manette, calibration, micro (test en direct), sons, effets. */
public final class SettingsScreen extends Screen {
    private static final ControlMode[] MODES = ControlMode.values();

    private final UiButton[] mode = new UiButton[MODES.length];
    private final UiButton pad = new UiButton("", Ui.SLATE, null);
    private final UiButton calFlat = new UiButton("À plat", Ui.BUTTON, null);
    private final UiButton calCur = new UiButton("Position actuelle", Ui.MUTED, null);
    private final UiButton sound = new UiButton("", Ui.SLATE, null);
    private final UiButton fx = new UiButton("", Ui.SLATE, null);
    private final UiButton back = new UiButton("Retour", Ui.SLATE, null);
    private final UiSlider slider = new UiSlider(Ui.TEAL);
    private final UiButton[] all;
    private float blowFlash;

    public SettingsScreen(GameView g) {
        super(g);
        all = new UiButton[mode.length + 6];
        for (int i = 0; i < mode.length; i++) {
            mode[i] = new UiButton(MODES[i].label, Ui.MUTED, null);
            all[i] = mode[i];
        }
        int k = mode.length;
        all[k] = pad;
        all[k + 1] = calFlat;
        all[k + 2] = calCur;
        all[k + 3] = sound;
        all[k + 4] = fx;
        all[k + 5] = back;
    }

    @Override
    public void onEnter() {
        slider.value = g.progress.micSens;
        slider.onCancel();
        blowFlash = 0f;
        for (UiButton b : all) b.onCancel();
        g.requestMic(); // le micro sert au test de sensibilité
    }

    @Override
    public void update(float dt) {
        blowFlash = Math.max(0f, blowFlash - dt);
        g.sensors.setMicSensitivity(slider.value); // effet immédiat pendant le glissement
        g.sensors.update(dt);
        if (g.sensors.consumeBlow()) blowFlash = 0.8f;
    }

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float fl = Math.max(Ui.sp(13), h * 0.034f), fd = Math.max(Ui.sp(12), h * 0.031f);

        Ui.title(c, "Réglages", L + W / 2f, h * 0.12f, h * 0.085f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.5f);
        back.set(L + W * 0.03f, h * 0.04f, L + W * 0.20f, h * 0.15f);
        back.draw(c);

        float pr = Ui.dp(20);
        Ui.roundRect(c, L + W * 0.03f, h * 0.19f, L + W * 0.50f, h * 0.97f, pr, Ui.alpha(Color.WHITE, 170));
        Ui.roundRect(c, L + W * 0.515f, h * 0.19f, L + W * 0.97f, h * 0.97f, pr, Ui.alpha(Color.WHITE, 170));

        // ---------- Colonne gauche : contrôle, côté de la manette, calibration ----------
        float x0 = L + W * 0.05f, x1 = L + W * 0.48f, gap = Ui.dp(10), cw = x1 - x0;
        Ui.text(c, "CONTRÔLE DE LA BILLE", x0, h * 0.25f, fl, Ui.PRIMARY, true, Paint.Align.LEFT, cw);
        ControlMode cur = g.progress.controlMode;
        float bw = (cw - gap * (mode.length - 1)) / mode.length;
        for (int i = 0; i < mode.length; i++) {
            UiButton b = mode[i];
            boolean sel = MODES[i] == cur;
            b.color = sel ? Ui.BUTTON : Ui.MUTED;
            b.set(x0 + i * (bw + gap), h * 0.28f, x0 + i * (bw + gap) + bw, h * 0.40f);
            b.draw(c);
            if (sel) Icons.check(c, b.rect.right - Ui.dp(16), b.rect.top + Ui.dp(16), Ui.dp(16), Color.WHITE);
        }
        Ui.text(c, cur.line1, x0, h * 0.46f, fd, Ui.TEXT, false, Paint.Align.LEFT, cw);
        Ui.text(c, cur.line2, x0, h * 0.51f, fd, Ui.TEXT, false, Paint.Align.LEFT, cw);

        pad.label = g.progress.padLeft ? "Manette à gauche, boutons à droite" : "Manette à droite, boutons à gauche";
        pad.set(x0, h * 0.55f, x1, h * 0.65f);
        pad.draw(c);

        Ui.text(c, "CALIBRATION (MODE INCLINAISON)", x0, h * 0.71f, fl, Ui.PRIMARY, true, Paint.Align.LEFT, cw);
        boolean flat = g.progress.calibFlat;
        calFlat.color = flat ? Ui.BUTTON : Ui.MUTED;
        calCur.color = flat ? Ui.MUTED : Ui.BUTTON;
        float hw = (cw - gap) / 2f;
        calFlat.set(x0, h * 0.74f, x0 + hw, h * 0.85f);
        calCur.set(x0 + hw + gap, h * 0.74f, x1, h * 0.85f);
        calFlat.draw(c);
        calCur.draw(c);
        UiButton sel = flat ? calFlat : calCur;
        Icons.check(c, sel.rect.right - Ui.dp(16), sel.rect.top + Ui.dp(16), Ui.dp(16), Color.WHITE);
        Ui.text(c, flat ? "Neutre = horizontale, tablette posée à plat (conseillé)." : "Neutre = position tenue au départ de la partie.",
                x0, h * 0.92f, fd, Ui.TEXT, false, Paint.Align.LEFT, cw);

        // ---------- Colonne droite : micro, sons, effets ----------
        float x2 = L + W * 0.535f, x3 = L + W * 0.95f, rw = x3 - x2;
        Ui.text(c, "SENSIBILITÉ DU MICRO (SOUFFLE)", x2, h * 0.25f, fl, Ui.PRIMARY, true, Paint.Align.LEFT, rw);
        slider.set(x2 + Ui.dp(8), h * 0.29f, x3 - Ui.dp(8), h * 0.37f);
        slider.draw(c);
        Ui.text(c, "Moins sensible", x2, h * 0.43f, fd, Ui.MUTED, false, Paint.Align.LEFT, rw * 0.48f);
        Ui.text(c, "Plus sensible", x3, h * 0.43f, fd, Ui.MUTED, false, Paint.Align.RIGHT, rw * 0.48f);

        float my = h * 0.48f, mh = h * 0.06f;
        if (g.sensors.micAvailable()) {
            Ui.roundRect(c, x2, my, x3, my + mh, mh / 2f, Ui.alpha(Ui.PRIMARY, 40));
            float v = Ui.clamp(g.sensors.micLevel() / (g.sensors.micThreshold() * 1.5f), 0f, 1f);
            if (v > 0f) Ui.roundRect(c, x2, my, x2 + rw * v, my + mh, mh / 2f, v >= 0.66f ? Ui.OK : Ui.BUTTON);
            float tick = x2 + rw * 0.667f; // seuil de déclenchement
            c.drawRect(tick - Ui.dp(1.5f), my - Ui.dp(3), tick + Ui.dp(1.5f), my + mh + Ui.dp(3), Ui.fill(Ui.PRIMARY));
            if (blowFlash > 0f) {
                Ui.text(c, "Souffle détecté !", x2, h * 0.61f, fl * 1.1f, Ui.OK, true, Paint.Align.LEFT, rw);
            } else {
                Ui.text(c, "Souffle vers le micro pour tester.", x2, h * 0.61f, fd, Ui.TEXT, false, Paint.Align.LEFT, rw);
            }
        } else {
            Ui.text(c, "Micro non autorisé : active-le dans les", x2, h * 0.52f, fd, Ui.DANGER, true, Paint.Align.LEFT, rw);
            Ui.text(c, "permissions Android. Le bouton Souffle", x2, h * 0.57f, fd, Ui.DANGER, true, Paint.Align.LEFT, rw);
            Ui.text(c, "reste utilisable en jeu.", x2, h * 0.62f, fd, Ui.DANGER, true, Paint.Align.LEFT, rw);
        }

        boolean snd = g.feedback.isEnabled();
        sound.label = snd ? "Sons et vibrations : OUI" : "Sons et vibrations : NON";
        sound.color = snd ? Ui.SLATE : Ui.MUTED;
        fx.label = "Effets lumineux : " + (g.progress.reducedFx ? "RÉDUITS" : "OUI");
        fx.color = g.progress.reducedFx ? Ui.OK : Ui.SLATE;
        sound.set(x2, h * 0.67f, x3, h * 0.79f);
        fx.set(x2, h * 0.82f, x3, h * 0.94f);
        sound.draw(c);
        fx.draw(c);
    }

    @Override
    public void onDown(float x, float y) {
        for (UiButton b : all) b.onDown(x, y);
        slider.onDown(x, y);
    }

    @Override
    public void onMove(float x, float y) {
        for (UiButton b : all) b.onMove(x, y);
        slider.onMove(x, y);
    }

    @Override
    public void onUp(float x, float y) {
        if (slider.onUp()) g.progress.setMicSens(slider.value);
        UiButton clicked = null;
        for (UiButton b : all) if (b.onUp(x, y)) clicked = b;
        if (clicked == null) return;

        for (int i = 0; i < mode.length; i++) {
            if (clicked == mode[i]) {
                g.progress.setControlMode(MODES[i]);
                g.feedback.vibrate(15);
                return;
            }
        }
        if (clicked == pad) {
            g.progress.togglePadLeft();
            g.feedback.vibrate(15);
        } else if (clicked == calFlat) {
            g.progress.setCalibFlat(true);
            g.sensors.setFlatReference(true);
            g.feedback.vibrate(15);
        } else if (clicked == calCur) {
            g.progress.setCalibFlat(false);
            g.sensors.setFlatReference(false);
            g.feedback.vibrate(15);
        } else if (clicked == sound) {
            g.feedback.toggle();
            g.feedback.vibrate(15);
        } else if (clicked == fx) {
            g.progress.toggleReducedFx();
        } else if (clicked == back) {
            g.closeSettings();
        }
    }

    @Override
    public void onCancel() {
        for (UiButton b : all) b.onCancel();
        slider.onCancel();
    }
}
