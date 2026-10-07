package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/** Choix du mode : Versus (course contre le fantôme) à gauche, Campagne à droite. */
final class ModeScreen extends Screen {
    private static final String[] MUSIC = {"NON", "MENUS", "MENUS + JEU"};

    private final UiButton ghostCard = new UiButton("", Ui.PURPLE, null);
    private final UiButton campaignCard = new UiButton("", Ui.BUTTON, null);
    private final UiButton back = new UiButton("Retour", Ui.SLATE, null);
    private final UiButton skins = new UiButton("Skins", Ui.TEAL, null);
    private final UiButton music = new UiButton("", Ui.SLATE, null);

    ModeScreen(GameView g) { super(g); }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        Ui.title(c, "Choisis ton mode", L + W / 2f, h * 0.14f, h * 0.09f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.9f);

        ghostCard.set(L + W * 0.05f, h * 0.20f, L + W * 0.48f, h * 0.78f);
        campaignCard.set(L + W * 0.52f, h * 0.20f, L + W * 0.95f, h * 0.78f);
        back.set(L + W * 0.05f, h * 0.85f, L + W * 0.24f, h * 0.95f);
        skins.set(L + W * 0.29f, h * 0.85f, L + W * 0.52f, h * 0.95f);
        music.set(L + W * 0.57f, h * 0.85f, L + W * 0.95f, h * 0.95f);
        skins.label = "Skins : " + Skin.values()[g.progress.skin()].label;
        music.label = "Musique : " + MUSIC[g.progress.musicMode()];

        drawCard(c, ghostCard, Ui.PURPLE, Icons::ghost, "Versus", "Course contre le fantôme",
                "Affronte ton meilleur passage,", "seconde après seconde.",
                g.progress.ghostCount() + " fantôme(s) enregistré(s)");
        drawCard(c, campaignCard, Ui.BUTTON, Icons::flag, "Campagne", Levels.ALL.length + " niveaux, 4 mondes",
                "Étoiles, murs fragiles, bonus,", "stroboscope et volcan.",
                g.progress.doneCount() + "/" + Levels.ALL.length + " niveaux · "
                        + g.progress.totalStars() + "/" + (3 * Levels.ALL.length) + " étoiles");
        back.draw(c);
        skins.draw(c);
        music.draw(c);
    }

    private void drawCard(Canvas c, UiButton b, int color, UiButton.Icon icon, String title, String sub,
                          String l1, String l2, String foot) {
        float off = b.pressed ? Ui.dp(3) : 0f;
        float l = b.rect.left, t = b.rect.top + off, r = b.rect.right, bo = b.rect.bottom + off;
        float w = r - l, h = bo - t, rad = Ui.dp(24), cx = (l + r) / 2f;
        Ui.roundRect(c, l, t + Ui.dp(5), r, bo + Ui.dp(5), rad, Ui.alpha(Ui.PRIMARY, 60));
        Ui.roundRect(c, l, t, r, bo, rad, Color.WHITE);
        float hh = h * 0.42f;
        c.save();
        c.clipRect(l, t, r, t + hh);
        Ui.roundRect(c, l, t, r, bo, rad, color);
        c.restore();
        icon.draw(c, cx, t + hh * 0.5f, hh * 0.62f, Color.WHITE);

        float fs = Math.max(Ui.sp(14), h * 0.055f);
        Ui.title(c, title, cx, t + hh + h * 0.13f, h * 0.10f, Ui.PRIMARY, Paint.Align.CENTER, w * 0.9f);
        Ui.text(c, sub, cx, t + hh + h * 0.22f, fs * 1.1f, color, true, Paint.Align.CENTER, w * 0.9f);
        Ui.text(c, l1, cx, t + hh + h * 0.32f, fs, Ui.TEXT, false, Paint.Align.CENTER, w * 0.9f);
        Ui.text(c, l2, cx, t + hh + h * 0.39f, fs, Ui.TEXT, false, Paint.Align.CENTER, w * 0.9f);
        Ui.text(c, foot, cx, bo - h * 0.05f, fs * 0.95f, Ui.PRIMARY, true, Paint.Align.CENTER, w * 0.9f);
    }

    @Override
    void onDown(float x, float y) {
        ghostCard.onDown(x, y); campaignCard.onDown(x, y); back.onDown(x, y); skins.onDown(x, y); music.onDown(x, y);
    }

    @Override
    void onMove(float x, float y) {
        ghostCard.onMove(x, y); campaignCard.onMove(x, y); back.onMove(x, y); skins.onMove(x, y); music.onMove(x, y);
    }

    @Override
    void onUp(float x, float y) {
        if (ghostCard.onUp(x, y)) g.goLevels(true);
        else if (campaignCard.onUp(x, y)) g.goLevels(false);
        else if (back.onUp(x, y)) g.goStart();
        else if (skins.onUp(x, y)) g.goSkins();
        else if (music.onUp(x, y)) g.cycleMusicMode();
    }

    @Override
    void onCancel() {
        ghostCard.onCancel(); campaignCard.onCancel(); back.onCancel(); skins.onCancel(); music.onCancel();
    }
}