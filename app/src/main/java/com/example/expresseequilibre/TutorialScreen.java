package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class TutorialScreen extends Screen {
    private static final String[] TITLES = {"Incliner", "Secouer", "Souffler"};
    private static final String[][] SUBS = {
            {"Fais rouler la bille", "en penchant l'appareil"},
            {"Une secousse = un saut,", "ou casse un mur fragile"},
            {"Souffle dans le micro :", "le vent te pousse"}};

    private final UiButton go = new UiButton("C'est parti !", Ui.BUTTON, Icons::play);
    private float t;

    TutorialScreen(GameView g) {
        super(g);
        go.horizontal = true;
    }

    @Override
    void onEnter() {
        t = 0f;
        go.label = g.pendingLevel >= 0 ? "C'est parti !" : "Compris";
    }

    @Override void update(float dt) { t += dt; }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        float cxm = L + W / 2f;

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
            if (i == 0) Icons.tiltAnim(c, cx, icy, s, Color.WHITE, (float) Math.sin(t * 2.2f) * 18f);
            else if (i == 1) Icons.shakeAnim(c, cx, icy, s, Color.WHITE, (float) Math.sin(t * 14f) * s * 0.06f);
            else Icons.wind(c, cx, icy, s, Color.WHITE);

            Ui.title(c, TITLES[i], cx, y + ch * 0.62f, ch * 0.11f, Ui.PRIMARY, Paint.Align.CENTER, cw * 0.9f);
            Ui.text(c, SUBS[i][0], cx, y + ch * 0.78f, ch * 0.08f, Ui.TEXT, false, Paint.Align.CENTER, cw * 0.9f);
            Ui.text(c, SUBS[i][1], cx, y + ch * 0.91f, ch * 0.08f, Ui.TEXT, false, Paint.Align.CENTER, cw * 0.9f);
        }

        Ui.text(c, "Les boutons Saut/Casser et Souffle, sur les côtés, servent de secours si un geste n'est pas reconnu.",
                cxm, h * 0.78f, Math.max(Ui.sp(14), h * 0.038f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.92f);

        go.set(L + W * 0.30f, h * 0.84f, L + W * 0.70f, h * 0.96f);
        go.draw(c);
    }

    @Override void onDown(float x, float y) { go.onDown(x, y); }

    @Override void onMove(float x, float y) { go.onMove(x, y); }

    @Override void onUp(float x, float y) { if (go.onUp(x, y)) g.onTutorialDone(); }

    @Override void onCancel() { go.onCancel(); }
}