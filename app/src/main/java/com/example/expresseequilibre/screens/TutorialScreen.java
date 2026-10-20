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
    private static final String[] TITLES = {"Incliner", "Secouer", "Souffler"};
    private static final String[][] FIRST = { // 1re carte selon le mode de contrôle choisi (ordre de ControlMode)
            {"Fais rouler la bille", "en penchant l'appareil"},
            {"Pose le pouce sur la manette", "et glisse : la bille suit"}};
    private static final String[][] SUBS = {
            {"Une secousse = un saut,", "ou casse un mur fragile"},
            {"Souffle dans le micro :", "le vent te pousse"}};

    private final UiButton go = new UiButton("C'est parti !", Ui.BUTTON, Icons::play);
    private float t;

    public TutorialScreen(GameView g) {
        super(g);
        go.horizontal = true;
    }

    @Override
    public void onEnter() {
        t = 0f;
        go.label = g.pendingLevel >= 0 ? "C'est parti !" : "Compris";
    }

    @Override public void update(float dt) { t += dt; }

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float cxm = L + W / 2f;
        ControlMode mode = g.progress.controlMode;

        Ui.title(c, "Comment jouer ?", cxm, h * 0.12f, h * 0.10f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.9f);
        Ui.text(c, "Récupère les étoiles, évite les trous, atteins la sortie avant la fin du temps",
                cxm, h * 0.19f, Math.max(Ui.sp(15), h * 0.042f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.92f);

        float gap = W * 0.02f, x0 = L + W * 0.05f;
        float cw = (W * 0.90f - 2 * gap) / 3f, ch = h * 0.46f, y = h * 0.24f;
        for (int i = 0; i < 3; i++) {
            float l = x0 + i * (cw + gap), r = l + cw, cx = (l + r) / 2f;
            Ui.roundRect(c, l, y + Ui.dp(4), r, y + ch + Ui.dp(4), Ui.dp(20), Ui.alpha(Ui.PRIMARY, 40));
            Ui.roundRect(c, l, y, r, y + ch, Ui.dp(20), Color.WHITE);

            float d = ch * 0.38f, icy = y + ch * 0.26f;
            c.drawCircle(cx, icy, d / 2f, Ui.fill(Ui.BUTTON));
            float s = d * 0.8f;
            String title, l1, l2;
            if (i == 0) {
                if (mode == ControlMode.TILT) {
                    Icons.tiltAnim(c, cx, icy, s, Color.WHITE, (float) Math.sin(t * 2.2f) * 18f);
                } else {
                    Icons.joystick(c, cx, icy, s, Color.WHITE, (float) Math.sin(t * 2f), (float) Math.cos(t * 2f));
                }
                title = mode.label;
                l1 = FIRST[mode.ordinal()][0];
                l2 = FIRST[mode.ordinal()][1];
            } else {
                if (i == 1) Icons.shakeAnim(c, cx, icy, s, Color.WHITE, (float) Math.sin(t * 14f) * s * 0.06f);
                else Icons.wind(c, cx, icy, s, Color.WHITE);
                title = TITLES[i];
                l1 = SUBS[i - 1][0];
                l2 = SUBS[i - 1][1];
            }

            Ui.title(c, title, cx, y + ch * 0.62f, ch * 0.11f, Ui.PRIMARY, Paint.Align.CENTER, cw * 0.9f);
            Ui.text(c, l1, cx, y + ch * 0.78f, ch * 0.08f, Ui.TEXT, false, Paint.Align.CENTER, cw * 0.9f);
            Ui.text(c, l2, cx, y + ch * 0.91f, ch * 0.08f, Ui.TEXT, false, Paint.Align.CENTER, cw * 0.9f);
        }

        Ui.text(c, "Les boutons Saut/Casser et Souffle servent de secours si un geste n'est pas reconnu.",
                cxm, h * 0.76f, Math.max(Ui.sp(14), h * 0.038f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.92f);
        Ui.text(c, "Le mode de contrôle et la sensibilité du micro se changent dans Réglages.",
                cxm, h * 0.81f, Math.max(Ui.sp(14), h * 0.038f), Ui.MUTED, true, Paint.Align.CENTER, W * 0.92f);

        go.set(L + W * 0.30f, h * 0.85f, L + W * 0.70f, h * 0.96f);
        go.draw(c);
    }

    @Override public void onDown(float x, float y) { go.onDown(x, y); }

    @Override public void onMove(float x, float y) { go.onMove(x, y); }

    @Override public void onUp(float x, float y) { if (go.onUp(x, y)) g.onTutorialDone(); }

    @Override public void onCancel() { go.onCancel(); }
}
