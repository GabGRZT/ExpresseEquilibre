package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.Locale;

/** Sélection des niveaux (campagne ou course contre le fantôme). */
final class LevelScreen extends Screen {
    private final UiButton[] tiles = new UiButton[Levels.ALL.length];
    private final UiButton back = new UiButton("Retour", Ui.SLATE, null);
    private final UiButton fx = new UiButton("", Ui.SLATE, null);
    private String msg = "";
    private float msgT;

    LevelScreen(GameView g) {
        super(g);
        for (int i = 0; i < tiles.length; i++) tiles[i] = new UiButton("", Ui.BUTTON, null);
    }

    @Override void onEnter() { msgT = 0f; }

    @Override void update(float dt) { msgT = Math.max(0f, msgT - dt); }

    @Override
    void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        Ui.title(c, g.ghostMode ? "Course contre le fantôme" : "Campagne", L + W / 2f, h * 0.12f, h * 0.085f,
                Ui.PRIMARY, Paint.Align.CENTER, W * 0.9f);
        String hint = g.ghostMode ? "Choisis un niveau où tu as déjà un fantôme." : "Termine un niveau pour débloquer le suivant.";
        Ui.text(c, hint, L + W / 2f, h * 0.18f, Math.max(Ui.sp(14), h * 0.04f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.9f);

        float gap = Ui.dp(14), x0 = L + W * 0.05f, x1 = L + W * 0.95f, y0 = h * 0.22f, y1 = h * 0.80f;
        float tw = (x1 - x0 - 2 * gap) / 3f, th = (y1 - y0 - gap) / 2f;
        for (int i = 0; i < tiles.length; i++) {
            int col = i % 3, row = i / 3;
            float l = x0 + col * (tw + gap), t = y0 + row * (th + gap);
            tiles[i].set(l, t, l + tw, t + th);
            drawTile(c, i, tiles[i], tw, th);
        }

        back.set(L + W * 0.05f, h * 0.86f, L + W * 0.28f, h * 0.95f);
        fx.set(L + W * 0.50f, h * 0.86f, L + W * 0.95f, h * 0.95f);
        fx.label = "Effets lumineux : " + (g.progress.reducedFx ? "RÉDUITS" : "OUI");
        fx.color = g.progress.reducedFx ? Ui.OK : Ui.SLATE;
        back.draw(c);
        fx.draw(c);

        if (msgT > 0f) {
            float size = Math.max(Ui.sp(16), h * 0.045f);
            float tww = Ui.measure(msg, size, true) + Ui.dp(36);
            float cx = L + W / 2f, cy = h * 0.5f;
            Ui.roundRect(c, cx - tww / 2f, cy - size * 1.1f, cx + tww / 2f, cy + size * 0.7f, size, Ui.alpha(Ui.PRIMARY, 235));
            Ui.text(c, msg, cx, cy, size, Color.WHITE, true, Paint.Align.CENTER, W * 0.9f);
        }
    }

    private void drawTile(Canvas c, int i, UiButton b, float tw, float th) {
        Level lv = Levels.ALL[i];
        World wd = World.of(lv.world);
        boolean unlocked = g.progress.unlocked(i);
        boolean ghostOk = !g.ghostMode || g.progress.hasGhost(i);
        boolean playable = unlocked && ghostOk;

        float off = b.pressed ? Ui.dp(3) : 0f;
        float l = b.rect.left, t = b.rect.top + off, r = b.rect.right, bo = b.rect.bottom + off;
        float rad = Ui.dp(20);
        Ui.roundRect(c, l, t + Ui.dp(5), r, bo + Ui.dp(5), rad, Ui.alpha(Ui.PRIMARY, 60));
        Ui.roundRect(c, l, t, r, bo, rad, playable ? Color.WHITE : Color.rgb(226, 232, 238));
        c.save();
        c.clipRect(l, t, l + Ui.dp(12), bo);
        Ui.roundRect(c, l, t, r, bo, rad, playable ? wd.accent : Ui.MUTED);
        c.restore();

        float cr = th * 0.15f, ccx = l + th * 0.30f, ccy = t + th * 0.27f;
        c.drawCircle(ccx, ccy, cr, Ui.fill(playable ? wd.accent : Ui.MUTED));
        Ui.text(c, String.valueOf(i + 1), ccx, ccy + cr * 0.38f, cr * 1.1f, Color.WHITE, true);

        float tx = l + th * 0.58f, maxW = r - tx - Ui.dp(10);
        Ui.title(c, lv.name, tx, t + th * 0.25f, th * 0.115f, Ui.TEXT, Paint.Align.LEFT, maxW);
        Ui.text(c, wd.name, tx, t + th * 0.38f, Math.max(Ui.sp(12), th * 0.08f), Ui.MUTED, true, Paint.Align.LEFT, maxW);

        int st = g.progress.stars(i);
        for (int k = 0; k < 3; k++) {
            float sx = l + th * 0.20f + k * th * 0.19f, sy = t + th * 0.60f;
            Icons.star(c, sx, sy, th * 0.075f, k < st ? Ui.STAR : Ui.STAR_EMPTY, true);
            if (k < st) Icons.star(c, sx, sy, th * 0.075f, Color.rgb(200, 130, 0), false);
        }

        float fs = Math.max(Ui.sp(12), th * 0.075f);
        float by = t + th * 0.88f;
        if (lv.strobe) {
            Icons.flash(c, l + th * 0.14f, by - fs * 0.35f, fs * 1.3f, Ui.ORANGE);
            Ui.text(c, "Lumière clignotante", l + th * 0.26f, by, fs, Ui.ORANGE, true, Paint.Align.LEFT, tw - th * 0.30f);
        } else if (g.ghostMode && g.progress.hasGhost(i)) {
            Icons.ghost(c, l + th * 0.14f, by - fs * 0.35f, fs * 1.4f, Ui.PURPLE);
            Ui.text(c, "Fantôme : " + String.format(Locale.getDefault(), "%.1f", g.progress.ghostTime(i)) + " s",
                    l + th * 0.26f, by, fs, Ui.PURPLE, true, Paint.Align.LEFT, tw - th * 0.30f);
        } else if (g.ghostMode) {
            Ui.text(c, "Pas de fantôme", l + th * 0.14f, by, fs, Ui.MUTED, false, Paint.Align.LEFT, tw - th * 0.2f);
        }

        if (!playable) {
            Ui.roundRect(c, l, t, r, bo, rad, Ui.alpha(Color.WHITE, 120));
            Icons.lock(c, r - th * 0.2f, t + th * 0.22f, th * 0.26f, Ui.MUTED);
        }
    }

    @Override
    void onDown(float x, float y) {
        for (UiButton b : tiles) b.onDown(x, y);
        back.onDown(x, y);
        fx.onDown(x, y);
    }

    @Override
    void onMove(float x, float y) {
        for (UiButton b : tiles) b.onMove(x, y);
        back.onMove(x, y);
        fx.onMove(x, y);
    }

    @Override
    void onUp(float x, float y) {
        for (int i = 0; i < tiles.length; i++) {
            if (!tiles[i].onUp(x, y)) continue;
            if (!g.progress.unlocked(i)) { say("Termine d'abord le niveau précédent."); }
            else if (g.ghostMode && !g.progress.hasGhost(i)) { say("Pas encore de fantôme : termine ce niveau en Campagne."); }
            else g.startLevel(i);
            return;
        }
        if (back.onUp(x, y)) g.goModes();
        else if (fx.onUp(x, y)) g.progress.toggleReducedFx();
    }

    private void say(String s) {
        msg = s;
        msgT = 2.2f;
    }

    @Override
    void onCancel() {
        for (UiButton b : tiles) b.onCancel();
        back.onCancel();
        fx.onCancel();
    }
}