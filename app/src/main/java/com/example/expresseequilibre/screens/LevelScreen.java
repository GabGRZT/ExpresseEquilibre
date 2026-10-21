package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.game.Level;
import com.example.expresseequilibre.game.Levels;
import com.example.expresseequilibre.game.World;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


/** Sélection des niveaux de la campagne. */
public final class LevelScreen extends Screen {
    private static final int COLUMNS = 3;

    private final UiButton[] tiles = new UiButton[Levels.ALL.length];
    private final UiButton back = add("Retour", Ui.SLATE, null, g::goStart);
    private String message = "";
    private float messageTime;

    public LevelScreen(GameView g) {
        super(g);
        for (int i = 0; i < tiles.length; i++) {
            final int level = i;
            tiles[i] = add("", Ui.BUTTON, null, () -> onTileClicked(level));
        }
    }

    @Override
    public void onEnter() {
        messageTime = 0f;
        super.onEnter();
    }

    @Override public void update(float dt) { messageTime = Math.max(0f, messageTime - dt); }

    private void onTileClicked(int level) {
        if (g.progress.unlocked(level)) {
            g.startLevel(level);
        } else {
            message = "Termine d'abord le niveau précédent.";
            messageTime = 2.2f;
        }
    }

    @Override
    public void draw(Canvas c, int w, int h) {
        c.drawColor(Ui.BG);
        float L = g.insetLeft, W = w - g.insetLeft - g.insetRight;
        Ui.title(c, "Campagne", L + W / 2f, h * 0.12f, h * 0.085f, Ui.PRIMARY, Paint.Align.CENTER, W * 0.9f);
        Ui.text(c, "Termine un niveau pour débloquer le suivant.", L + W / 2f, h * 0.18f,
                Math.max(Ui.sp(14), h * 0.04f), Ui.TEXT, false, Paint.Align.CENTER, W * 0.9f);

        float gap = Ui.dp(14), x0 = L + W * 0.05f, x1 = L + W * 0.95f, y0 = h * 0.22f, y1 = h * 0.80f;
        float tileW = (x1 - x0 - 2 * gap) / COLUMNS, tileH = (y1 - y0 - gap) / 2f;
        for (int i = 0; i < tiles.length; i++) {
            float left = x0 + (i % COLUMNS) * (tileW + gap), top = y0 + (i / COLUMNS) * (tileH + gap);
            tiles[i].set(left, top, left + tileW, top + tileH);
            drawTile(c, i, tileW, tileH);
        }

        back.set(L + W * 0.05f, h * 0.86f, L + W * 0.28f, h * 0.95f);
        back.draw(c);
        if (messageTime > 0f) drawMessage(c, L + W / 2f, h, W);
    }

    private void drawTile(Canvas c, int i, float tw, float th) {
        UiButton b = tiles[i];
        Level level = Levels.ALL[i];
        World world = World.of(level.world);
        boolean playable = g.progress.unlocked(i);
        int accent = playable ? world.accent : Ui.MUTED;

        float off = b.pressed ? Ui.dp(3) : 0f;
        float l = b.rect.left, t = b.rect.top + off, r = b.rect.right, bottom = b.rect.bottom + off;
        float rad = Ui.dp(20);
        Ui.roundRect(c, l, t + Ui.dp(5), r, bottom + Ui.dp(5), rad, Ui.alpha(Ui.PRIMARY, 60));
        Ui.roundRect(c, l, t, r, bottom, rad, playable ? Color.WHITE : Color.rgb(226, 232, 238));
        c.save();
        c.clipRect(l, t, l + Ui.dp(12), bottom); // bandeau de couleur du monde, à gauche
        Ui.roundRect(c, l, t, r, bottom, rad, accent);
        c.restore();

        float badgeR = th * 0.15f, badgeX = l + th * 0.30f, badgeY = t + th * 0.27f;
        c.drawCircle(badgeX, badgeY, badgeR, Ui.fill(accent));
        Ui.text(c, String.valueOf(i + 1), badgeX, badgeY + badgeR * 0.38f, badgeR * 1.1f, Color.WHITE, true);

        float tx = l + th * 0.58f, maxW = r - tx - Ui.dp(10);
        Ui.title(c, level.name, tx, t + th * 0.25f, th * 0.115f, Ui.TEXT, Paint.Align.LEFT, maxW);
        Ui.text(c, world.name, tx, t + th * 0.38f, Math.max(Ui.sp(12), th * 0.08f), Ui.MUTED, true, Paint.Align.LEFT, maxW);

        int stars = g.progress.stars(i);
        for (int k = 0; k < 3; k++) Icons.rating(c, l + th * 0.20f + k * th * 0.19f, t + th * 0.60f, th * 0.075f, k < stars);

        if (level.strobe) {
            float fs = Math.max(Ui.sp(12), th * 0.075f), y = t + th * 0.88f;
            Icons.flash(c, l + th * 0.14f, y - fs * 0.35f, fs * 1.3f, Ui.ORANGE);
            Ui.text(c, "Lumière clignotante", l + th * 0.26f, y, fs, Ui.ORANGE, true, Paint.Align.LEFT, tw - th * 0.30f);
        }
        if (!playable) {
            Ui.roundRect(c, l, t, r, bottom, rad, Ui.alpha(Color.WHITE, 120));
            Icons.lock(c, r - th * 0.2f, t + th * 0.22f, th * 0.26f, Ui.MUTED);
        }
    }

    private void drawMessage(Canvas c, float cx, int h, float W) {
        float size = Math.max(Ui.sp(16), h * 0.045f), cy = h * 0.5f;
        float halfW = (Ui.measure(message, size, true) + Ui.dp(36)) / 2f;
        Ui.roundRect(c, cx - halfW, cy - size * 1.1f, cx + halfW, cy + size * 0.7f, size, Ui.alpha(Ui.PRIMARY, 235));
        Ui.text(c, message, cx, cy, size, Color.WHITE, true, Paint.Align.CENTER, W * 0.9f);
    }
}
