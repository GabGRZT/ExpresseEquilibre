package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Choix du skin de la bille (déblocage par étoiles). */
final class SkinScreen extends Screen {
    private final UiButton[] cards = new UiButton[Skin.values().length];
    private final UiButton back = new UiButton("Retour", Ui.SLATE, null);
    private float t;
    private String msg = "";
    private float msgT;

    SkinScreen(GameView g) {
        super(g);
        for (int i = 0; i < cards.length; i++) cards[i] = new UiButton("", Ui.BUTTON, null);
    }

    @Override void onEnter() { t = 0f; msgT = 0f; }

    @Override
    void update(float dt) {
        t += dt;
        msgT = Math.max(0f, msgT - dt);
    }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        Ui.title(c, "Skins de la bille", L + W / 2f, h * 0.12f, h * 0.09f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.9f);
        Ui.text(c, "Étoiles récoltées : " + g.progress.totalStars() + " / " + (3 * Levels.ALL.length)
                        + "  ·  les skins ne changent pas le gameplay",
                L + W / 2f, h * 0.175f, Math.max(Ui.sp(14), h * 0.038f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.92f);

        int cols = 3, rows = 2;
        float gap = Ui.dp(14), x0 = L + W * 0.04f, x1 = L + W * 0.96f, y0 = h * 0.21f, y1 = h * 0.82f;
        float cw = (x1 - x0 - (cols - 1) * gap) / cols, ch = (y1 - y0 - (rows - 1) * gap) / rows;
        int equipped = g.progress.skin();
        for (int i = 0; i < cards.length; i++) {
            int col = i % cols, row = i / cols;
            float l = x0 + col * (cw + gap), tp = y0 + row * (ch + gap);
            cards[i].set(l, tp, l + cw, tp + ch);
            drawCard(c, i, cards[i], cw, ch, i == equipped);
        }
        back.set(L + W * 0.04f, h * 0.88f, L + W * 0.24f, h * 0.96f);
        back.draw(c);

        if (msgT > 0f) {
            float size = Math.max(Ui.sp(16), h * 0.042f);
            float tw = Math.min(W * 0.9f, Ui.measure(msg, size, true) + Ui.dp(36));
            float cx = L + W / 2f, cy = h * 0.5f;
            Ui.roundRect(c, cx - tw / 2f, cy - size * 1.1f, cx + tw / 2f, cy + size * 0.7f, size, Ui.alpha(Ui.PRIMARY, 240));
            Ui.text(c, msg, cx, cy, size, Color.WHITE, true, Paint.Align.CENTER, tw - Ui.dp(24));
        }
    }

    private void drawCard(Canvas c, int i, UiButton b, float cw, float ch, boolean equipped) {
        Skin sk = Skin.values()[i];
        boolean unlocked = g.progress.skinUnlocked(i);
        float off = b.pressed ? Ui.dp(3) : 0f;
        float l = b.rect.left, tp = b.rect.top + off, r = b.rect.right, bo = b.rect.bottom + off;
        float rad = Ui.dp(20), cx = (l + r) / 2f;
        Ui.roundRect(c, l, tp + Ui.dp(5), r, bo + Ui.dp(5), rad, Ui.alpha(Ui.PRIMARY, 60));
        Ui.roundRect(c, l, tp, r, bo, rad, Color.WHITE);
        if (equipped) {
            Ui.R.set(l, tp, r, bo);
            c.drawRoundRect(Ui.R, rad, rad, Ui.stroke(Ui.OK, Ui.dp(4)));
        }
        float hh = ch * 0.50f;
        c.save();
        c.clipRect(l, tp, r, tp + hh);
        Ui.roundRect(c, l, tp, r, bo, rad, Ui.alpha(sk.color, 55));
        c.restore();
        sk.draw(c, cx, tp + hh * 0.52f, hh * 0.26f, t, 0.6f);

        float fs = Math.max(Ui.sp(12), ch * 0.075f);
        Ui.title(c, sk.label, cx, tp + hh + ch * 0.13f, Math.max(Ui.sp(14), ch * 0.11f), Ui.TEXT, Paint.Align.CENTER, cw * 0.9f);
        Ui.text(c, sk.tagline, cx, tp + hh + ch * 0.23f, fs, Ui.MUTED, false, Paint.Align.CENTER, cw * 0.9f);
        if (equipped) {
            Ui.text(c, "Équipé", cx, bo - ch * 0.05f, fs * 1.1f, Ui.OK, true);
        } else if (unlocked) {
            Ui.text(c, "Toucher pour équiper", cx, bo - ch * 0.05f, fs, Ui.BUTTON, true, Paint.Align.CENTER, cw * 0.9f);
        } else {
            Ui.text(c, sk.unlock + " étoiles (" + g.progress.totalStars() + "/" + sk.unlock + ")", cx, bo - ch * 0.05f, fs,
                    Ui.DANGER, true, Paint.Align.CENTER, cw * 0.9f);
            Ui.roundRect(c, l, tp, r, bo, rad, Ui.alpha(Color.WHITE, 120));
            Icons.lock(c, r - ch * 0.12f, tp + ch * 0.12f, ch * 0.14f, Ui.MUTED);
        }
    }

    @Override
    void onDown(float x, float y) {
        for (UiButton b : cards) b.onDown(x, y);
        back.onDown(x, y);
    }

    @Override
    void onMove(float x, float y) {
        for (UiButton b : cards) b.onMove(x, y);
        back.onMove(x, y);
    }

    @Override
    void onUp(float x, float y) {
        for (int i = 0; i < cards.length; i++) {
            if (!cards[i].onUp(x, y)) continue;
            if (g.progress.skinUnlocked(i)) {
                g.progress.setSkin(i);
                g.feedback.sfx(Sfx.GOOD, Skin.values()[i].pitch);
            } else {
                msg = "Encore " + (Skin.values()[i].unlock - g.progress.totalStars()) + " étoiles pour le débloquer.";
                msgT = 2.2f;
            }
            return;
        }
        if (back.onUp(x, y)) g.goModes();
    }

    @Override
    void onCancel() {
        for (UiButton b : cards) b.onCancel();
        back.onCancel();
    }
}