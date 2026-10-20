package com.example.expresseequilibre.screens;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import com.example.expresseequilibre.ui.UiSlider;
import com.example.expresseequilibre.game.GameView;

/** Réglages : contrôle de la bille, calibration, sensibilité du micro (avec test en direct), sons, effets. */
public final class SettingsScreen extends Screen {
    private static final ControlMode[] MODES = ControlMode.values();

    private final UiButton[] mode = new UiButton[MODES.length];
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
        for (int i = 0; i < mode.length; i++) mode[i] = new UiButton(MODES[i].label, Ui.MUTED, null);
        all = new UiButton[]{mode[0], mode[1], mode[2], calFlat, calCur, sound, fx, back};
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

        // ---------- Colonne gauche : contrôle + calibration ----------
        float x0 = L + W * 0.05f, x1 = L + W * 0.48f, gap = Ui.dp(10), cw = x1 - x0;
        Ui.text(c, "CONTRÔLE DE LA BILLE", x0, h * 0.25f, fl, Ui.PRIMARY, true, Paint.Align.LEFT, cw);
        ControlMode cur = g.progress.controlMode;
        float bw = (cw - 2 * gap) / 3f;
        for (int i = 0; i < mode.length; i++) {
            UiButton b = mode[i];
            boolean sel = MODES[i] == cur;
            b.color = sel ? Ui.BUTTON : Ui.MUTED;
            b.set(x0 + i * (bw + gap), h * 0.28f, x0 + i * (bw + gap) + bw, h * 0.42f);
            b.draw(c);
            if (sel) Icons.check(c, b.rect.right - Ui.dp(16), b.rect.top + Ui.dp(16), Ui.dp(16), Color.WHITE);
        }
        Ui.text(c, cur.line1, x0, h * 0.49f, fd, Ui.TEXT, false, Paint.Align.LEFT, cw);
        Ui.text(c, cur.line2, x0, h * 0.54f, fd, Ui.TEXT, false, Paint.Align.LEFT, cw);
        Ui.text(c, "Dans tous les modes : secouer = saut, souffler = vent.", x0, h * 0.60f, fd, Ui.MUTED, true,
                Paint.Align.LEFT, cw);

        Ui.text(c, "CALIBRATION (MODE INCLINAISON)", x0, h * 0.66f, fl, Ui.PRIMARY, true, Paint.Align.LEFT, cw);
        boolean flat = g.progress.calibFlat;
        calFlat.color = flat ? Ui.BUTTON : Ui.MUTED;
        calCur.color = flat ? Ui.MUTED : Ui.BUTTON;
        float hw = (cw - gap) / 2f;
        calFlat.set(x0, h * 0.69f, x0 + hw, h * 0.81f);
        calCur.set(x0 + hw + gap, h * 0.69f, x1, h * 0.81f);
        calFlat.draw(c);
        calCur.draw(c);
        UiButton sel = flat ? calFlat : calCur;
        Icons.check(c, sel.rect.right - Ui.dp(16), sel.rect.top + Ui.dp(16), Ui.dp(16), Color.WHITE);
        Ui.text(c, flat ? "Le neutre est l'horizontale : tablette posée à plat." : "Le neutre est la position tenue au départ.",
                x0, h * 0.88f, fd, Ui.TEXT, false, Paint.Align.LEFT, cw);
        Ui.text(c, flat ? "Recommandé : démarrage identique à chaque partie." : "Utile si tu joues tablette inclinée.",
                x0, h * 0.93f, fd, Ui.MUTED, true, Paint.Align.LEFT, cw);

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
        if (clicked == calFlat) {
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
            g.goStart();
        }
    }

    @Override
    public void onCancel() {
        for (UiButton b : all) b.onCancel();
        slider.onCancel();
    }
}
