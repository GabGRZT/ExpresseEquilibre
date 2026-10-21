package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.input.BlowDetector;
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

    private final UiButton[] modeButtons = new UiButton[MODES.length];
    private final UiButton padSide = add("", Ui.SLATE, null, () -> tap(g.progress::togglePadLeft));
    private final UiButton flat = add("À plat", Ui.BUTTON, null, () -> tap(() -> setFlatReference(true)));
    private final UiButton current = add("Position actuelle", Ui.MUTED, null, () -> tap(() -> setFlatReference(false)));
    private final UiButton sound = add("", Ui.SLATE, null, () -> tap(g.feedback::toggle));
    private final UiButton effects = add("", Ui.SLATE, null, g.progress::toggleReducedFx);
    private final UiButton back = add("Retour", Ui.SLATE, null, g::closeSettings);
    private final UiSlider sensitivity = new UiSlider(Ui.TEAL);
    private float blowFlash;

    public SettingsScreen(GameView g) {
        super(g);
        for (int i = 0; i < MODES.length; i++) {
            final ControlMode mode = MODES[i];
            modeButtons[i] = add(mode.label, Ui.MUTED, null, () -> tap(() -> g.progress.setControlMode(mode)));
        }
    }

    /** Applique un réglage avec une courte vibration de confirmation. */
    private void tap(Runnable change) {
        change.run();
        g.feedback.vibrate(15);
    }

    private void setFlatReference(boolean isFlat) {
        g.progress.setCalibFlat(isFlat);
        g.sensors.tilt.setFlatReference(isFlat);
    }

    @Override
    public void onEnter() {
        sensitivity.value = g.progress.micSens;
        blowFlash = 0f;
        super.onEnter();
        g.requestMic(); // le micro sert au test de sensibilité
    }

    @Override
    public void update(float dt) {
        BlowDetector blow = g.sensors.blow;
        blowFlash = Math.max(0f, blowFlash - dt);
        blow.setSensitivity(sensitivity.value); // effet immédiat pendant le glissement
        blow.update(dt);
        if (blow.consume()) blowFlash = 0.8f;
    }

    // ---------- Dessin ----------

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;

        Ui.title(c, "Réglages", L + W / 2f, h * 0.12f, h * 0.085f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.5f);
        back.set(L + W * 0.03f, h * 0.04f, L + W * 0.20f, h * 0.15f);
        back.draw(c);

        float radius = Ui.dp(20);
        Ui.roundRect(c, L + W * 0.03f, h * 0.19f, L + W * 0.50f, h * 0.97f, radius, Ui.alpha(Color.WHITE, 170));
        Ui.roundRect(c, L + W * 0.515f, h * 0.19f, L + W * 0.97f, h * 0.97f, radius, Ui.alpha(Color.WHITE, 170));

        drawControlSection(c, L + W * 0.05f, L + W * 0.48f, h);
        drawMicSection(c, L + W * 0.535f, L + W * 0.95f, h);
    }

    private void label(Canvas c, String text, float x, float y, float width, int h) {
        Ui.text(c, text, x, y, Math.max(Ui.sp(13), h * 0.034f), Ui.PRIMARY, true, Paint.Align.LEFT, width);
    }

    private void note(Canvas c, String text, float x, float y, float width, int h, int color, boolean bold) {
        Ui.text(c, text, x, y, Math.max(Ui.sp(12), h * 0.031f), color, bold, Paint.Align.LEFT, width);
    }

    /** Bouton de choix : bleu avec une coche s'il est sélectionné, gris sinon. */
    private void drawChoice(Canvas c, UiButton b, boolean selected) {
        b.color = selected ? Ui.BUTTON : Ui.MUTED;
        b.draw(c);
        if (selected) Icons.check(c, b.rect.right - Ui.dp(16), b.rect.top + Ui.dp(16), Ui.dp(16), Color.WHITE);
    }

    /** Colonne de gauche : contrôle de la bille, côté de la manette, calibration. */
    private void drawControlSection(Canvas c, float x0, float x1, int h) {
        float width = x1 - x0, gap = Ui.dp(10);
        label(c, "CONTRÔLE DE LA BILLE", x0, h * 0.25f, width, h);
        ControlMode selected = g.progress.controlMode;
        float buttonW = (width - gap * (MODES.length - 1)) / MODES.length;
        for (int i = 0; i < MODES.length; i++) {
            float left = x0 + i * (buttonW + gap);
            modeButtons[i].set(left, h * 0.28f, left + buttonW, h * 0.40f);
            drawChoice(c, modeButtons[i], MODES[i] == selected);
        }
        note(c, selected.line1, x0, h * 0.46f, width, h, Ui.TEXT, false);
        note(c, selected.line2, x0, h * 0.51f, width, h, Ui.TEXT, false);

        padSide.label = g.progress.padLeft ? "Manette à gauche, boutons à droite" : "Manette à droite, boutons à gauche";
        padSide.set(x0, h * 0.55f, x1, h * 0.65f);
        padSide.draw(c);

        label(c, "CALIBRATION (MODE INCLINAISON)", x0, h * 0.71f, width, h);
        boolean isFlat = g.progress.calibFlat;
        float half = (width - gap) / 2f;
        flat.set(x0, h * 0.74f, x0 + half, h * 0.85f);
        current.set(x0 + half + gap, h * 0.74f, x1, h * 0.85f);
        drawChoice(c, flat, isFlat);
        drawChoice(c, current, !isFlat);
        note(c, isFlat ? "Neutre = horizontale, tablette posée à plat (conseillé)." : "Neutre = position tenue au départ de la partie.",
                x0, h * 0.92f, width, h, Ui.TEXT, false);
    }

    /** Colonne de droite : sensibilité du micro avec jauge de test, sons, effets lumineux. */
    private void drawMicSection(Canvas c, float x0, float x1, int h) {
        float width = x1 - x0;
        label(c, "SENSIBILITÉ DU MICRO (SOUFFLE)", x0, h * 0.25f, width, h);
        sensitivity.set(x0 + Ui.dp(8), h * 0.29f, x1 - Ui.dp(8), h * 0.37f);
        sensitivity.draw(c);
        float small = Math.max(Ui.sp(12), h * 0.031f);
        Ui.text(c, "Moins sensible", x0, h * 0.43f, small, Ui.MUTED, false, Paint.Align.LEFT, width * 0.48f);
        Ui.text(c, "Plus sensible", x1, h * 0.43f, small, Ui.MUTED, false, Paint.Align.RIGHT, width * 0.48f);

        if (g.sensors.blow.isAvailable()) drawMicMeter(c, x0, x1, h);
        else {
            note(c, "Micro non autorisé : active-le dans les", x0, h * 0.52f, width, h, Ui.DANGER, true);
            note(c, "permissions Android. Le bouton Souffle", x0, h * 0.57f, width, h, Ui.DANGER, true);
            note(c, "reste utilisable en jeu.", x0, h * 0.62f, width, h, Ui.DANGER, true);
        }

        sound.label = g.feedback.isEnabled() ? "Sons et vibrations : OUI" : "Sons et vibrations : NON";
        sound.color = g.feedback.isEnabled() ? Ui.SLATE : Ui.MUTED;
        effects.label = "Effets lumineux : " + (g.progress.reducedFx ? "RÉDUITS" : "OUI");
        effects.color = g.progress.reducedFx ? Ui.OK : Ui.SLATE;
        sound.set(x0, h * 0.67f, x1, h * 0.79f);
        effects.set(x0, h * 0.82f, x1, h * 0.94f);
        sound.draw(c);
        effects.draw(c);
    }

    /** Jauge du niveau sonore en direct ; le trait marque le seuil à dépasser pour déclencher le souffle. */
    private void drawMicMeter(Canvas c, float x0, float x1, int h) {
        BlowDetector blow = g.sensors.blow;
        float width = x1 - x0, y = h * 0.48f, meterH = h * 0.06f;
        Ui.roundRect(c, x0, y, x1, y + meterH, meterH / 2f, Ui.alpha(Ui.PRIMARY, 40));
        float level = Ui.clamp(blow.level() / (blow.threshold() * 1.5f), 0f, 1f);
        if (level > 0f) Ui.roundRect(c, x0, y, x0 + width * level, y + meterH, meterH / 2f, level >= 0.66f ? Ui.OK : Ui.BUTTON);
        float tick = x0 + width * 0.667f;
        c.drawRect(tick - Ui.dp(1.5f), y - Ui.dp(3), tick + Ui.dp(1.5f), y + meterH + Ui.dp(3), Ui.fill(Ui.PRIMARY));
        if (blowFlash > 0f) {
            Ui.text(c, "Souffle détecté !", x0, h * 0.61f, Math.max(Ui.sp(13), h * 0.034f) * 1.1f, Ui.OK, true, Paint.Align.LEFT, width);
        } else {
            note(c, "Souffle vers le micro pour tester.", x0, h * 0.61f, width, h, Ui.TEXT, false);
        }
    }

    // ---------- Tactile : le curseur s'ajoute aux boutons gérés par Screen ----------

    @Override
    public void onDown(float x, float y) {
        super.onDown(x, y);
        sensitivity.onDown(x, y);
    }

    @Override
    public void onMove(float x, float y) {
        super.onMove(x, y);
        sensitivity.onMove(x, y);
    }

    @Override
    public void onUp(float x, float y) {
        if (sensitivity.onUp()) g.progress.setMicSens(sensitivity.value);
        super.onUp(x, y);
    }

    @Override
    public void onCancel() {
        super.onCancel();
        sensitivity.onCancel();
    }
}
