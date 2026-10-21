package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public final class TutorialScreen extends Screen {
    private static final String[][] FIRST_CARD = { // 1re carte selon le mode de contrôle (ordre de ControlMode)
            {"Fais rouler la bille", "en penchant l'appareil"},
            {"Pose le pouce sur la manette", "et glisse : la bille suit"}};

    private final UiButton go = add("C'est parti !", Ui.BUTTON, Icons::play, g::onTutorialDone);
    private float t;

    public TutorialScreen(GameView g) {
        super(g);
        go.horizontal = true;
    }

    @Override
    public void onEnter() {
        t = 0f;
        go.label = g.pendingLevel >= 0 ? "C'est parti !" : "Compris";
        super.onEnter();
    }

    @Override public void update(float dt) { t += dt; }

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float cx = L + W / 2f;

        Ui.title(c, "Comment jouer ?", cx, h * 0.12f, h * 0.10f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.9f);
        Ui.text(c, "Récupère les étoiles, évite les trous, atteins la sortie avant la fin du temps",
                cx, h * 0.19f, Math.max(Ui.sp(15), h * 0.042f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.92f);
        drawCards(c, L, W, h);

        float note = Math.max(Ui.sp(14), h * 0.038f);
        Ui.text(c, "Les boutons Saut/Casser et Souffle servent de secours si un geste n'est pas reconnu.",
                cx, h * 0.76f, note, Ui.TEXT, false, Paint.Align.CENTER, W * 0.92f);
        Ui.text(c, "Le mode de contrôle et la sensibilité du micro se changent dans Réglages.",
                cx, h * 0.81f, note, Ui.MUTED, true, Paint.Align.CENTER, W * 0.92f);

        go.set(L + W * 0.30f, h * 0.85f, L + W * 0.70f, h * 0.96f);
        go.draw(c);
    }

    /** Trois cartes : piloter (selon le mode choisi), secouer, souffler. */
    private void drawCards(Canvas c, float L, float W, int h) {
        ControlMode mode = g.progress.controlMode;
        float gap = W * 0.02f, x0 = L + W * 0.05f;
        float cardW = (W * 0.90f - 2 * gap) / 3f, cardH = h * 0.46f, y = h * 0.24f;

        for (int i = 0; i < 3; i++) {
            float left = x0 + i * (cardW + gap), cx = left + cardW / 2f;
            Ui.roundRect(c, left, y + Ui.dp(4), left + cardW, y + cardH + Ui.dp(4), Ui.dp(20), Ui.alpha(Ui.PRIMARY, 40));
            Ui.roundRect(c, left, y, left + cardW, y + cardH, Ui.dp(20), Color.WHITE);

            float d = cardH * 0.38f, iconY = y + cardH * 0.26f, s = d * 0.8f;
            c.drawCircle(cx, iconY, d / 2f, Ui.fill(Ui.BUTTON));

            String title, line1, line2;
            if (i == 0) {
                if (mode == ControlMode.TILT) Icons.tiltAnim(c, cx, iconY, s, Color.WHITE, (float) Math.sin(t * 2.2f) * 18f);
                else Icons.joystick(c, cx, iconY, s, Color.WHITE, (float) Math.sin(t * 2f), (float) Math.cos(t * 2f));
                title = mode.label;
                line1 = FIRST_CARD[mode.ordinal()][0];
                line2 = FIRST_CARD[mode.ordinal()][1];
            } else if (i == 1) {
                Icons.shakeAnim(c, cx, iconY, s, Color.WHITE, (float) Math.sin(t * 14f) * s * 0.06f);
                title = "Secouer";
                line1 = "Une secousse = un saut,";
                line2 = "ou casse un mur fragile";
            } else {
                Icons.wind(c, cx, iconY, s, Color.WHITE);
                title = "Souffler";
                line1 = "Souffle dans le micro :";
                line2 = "le vent te pousse";
            }
            Ui.title(c, title, cx, y + cardH * 0.62f, cardH * 0.11f, Ui.PRIMARY, Paint.Align.CENTER, cardW * 0.9f);
            Ui.text(c, line1, cx, y + cardH * 0.78f, cardH * 0.08f, Ui.TEXT, false, Paint.Align.CENTER, cardW * 0.9f);
            Ui.text(c, line2, cx, y + cardH * 0.91f, cardH * 0.08f, Ui.TEXT, false, Paint.Align.CENTER, cardW * 0.9f);
        }
    }
}
