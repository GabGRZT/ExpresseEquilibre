package com.example.expresseequilibre.game;

import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;


/** Interface par-dessus le plateau : étoiles, chrono, effets actifs, boutons, compte à rebours et messages. */
public final class PlayHud {
    private final GameView g;
    private final GameWorld world;
    private final TouchLayout layout;
    private final Countdown countdown;
    private final UiButton pauseButton, actionButton, blowButton;

    public PlayHud(GameView g, GameWorld world, TouchLayout layout, Countdown countdown,
                   UiButton pauseButton, UiButton actionButton, UiButton blowButton) {
        this.g = g;
        this.world = world;
        this.layout = layout;
        this.countdown = countdown;
        this.pauseButton = pauseButton;
        this.actionButton = actionButton;
        this.blowButton = blowButton;
    }

    private World theme() { return World.of(world.level.world); }

    private boolean isUrgent() { return world.timeLeft <= 10f && world.isRunning(); }

    // ---------- Bandeau du haut ----------

    public void drawTopBar(Canvas c) {
        World theme = theme();
        RectF board = layout.board;
        float top = g.insetTop;
        float mid = top + (board.top - top) * 0.42f;
        boolean urgent = isUrgent();
        boolean calm = !g.progress.reducedFx;
        float time = world.effects.time;

        float starR = Ui.dp(14), step = Ui.dp(36);
        for (int i = 0; i < 3; i++) Icons.rating(c, board.left + Ui.dp(16) + i * step, mid, starR, i < world.starCount);
        drawActiveEffects(c, board.left + Ui.dp(16) + 3 * step + Ui.dp(14), mid);

        int seconds = Math.max(0, (int) Math.ceil(world.timeLeft));
        float size = Ui.sp(32);
        if (urgent && calm) size *= 1f + 0.08f * Math.abs((float) Math.sin(time * 4f));
        Ui.text(c, seconds + " s", board.right, mid + size * 0.35f, size, urgent ? Ui.DANGER : theme.hud, true, Paint.Align.RIGHT, 0f);

        float barY = board.top - Ui.dp(16), barH = Ui.dp(8);
        Ui.roundRect(c, board.left, barY, board.right, barY + barH, barH / 2f, Ui.alpha(theme.hud, 40));
        float ratio = Ui.clamp(world.timeLeft / world.level.time, 0f, 1f);
        if (ratio > 0f) {
            Ui.roundRect(c, board.left, barY, board.left + board.width() * ratio, barY + barH, barH / 2f, urgent ? Ui.DANGER : theme.accent);
        }
        if (urgent && calm) { // liseré rouge qui respire doucement
            Ui.outline(c, board, Ui.dp(16), Ui.alpha(Ui.DANGER, (int) (110 + 90 * Math.sin(time * 3f))), Ui.dp(5));
        }
    }

    /** Pastilles des effets en cours (bonus en vert, malus en rouge) avec leur compte à rebours. */
    private void drawActiveEffects(Canvas c, float x, float mid) {
        float w = Ui.dp(82), h = Ui.dp(32);
        for (Pickup pickup : Pickup.values()) {
            float t = world.remaining(pickup);
            if (t <= 0f || pickup.duration <= 0f) continue;
            Ui.roundRect(c, x, mid - h / 2f, x + w, mid + h / 2f, h / 2f, pickup.bonus ? Ui.OK : Ui.DANGER);
            pickup.icon.draw(c, x + h * 0.55f, mid, h * 0.62f, Color.WHITE);
            Ui.text(c, (int) Math.ceil(t) + " s", x + w - Ui.dp(10), mid + Ui.sp(14) * 0.35f, Ui.sp(14),
                    Color.WHITE, true, Paint.Align.RIGHT, 0f);
            x += w + Ui.dp(8);
        }
    }

    // ---------- Boutons ----------

    public void drawControls(Canvas c, float h) {
        boolean nearFragile = world.nearWall >= 0;
        actionButton.label = nearFragile ? "Casser" : "Saut";
        actionButton.icon = nearFragile ? Icons::hammer : Icons::jump;
        actionButton.color = nearFragile ? Ui.ORANGE : Ui.BUTTON;

        boolean running = world.isRunning();
        actionButton.enabled = running;
        blowButton.enabled = running;
        actionButton.fill = world.jumpCharge();
        blowButton.fill = world.fanCharge();

        layout.placeButtons(pauseButton, actionButton, blowButton, h);
        pauseButton.draw(c);
        actionButton.draw(c);
        blowButton.draw(c);
        if (running && g.sensors.blow.isAvailable()) drawMicGauge(c);
    }

    /** Jauge du micro en direct, dans le bouton Souffle : le trait marque le seuil de déclenchement. */
    private void drawMicGauge(Canvas c) {
        RectF r = blowButton.rect;
        float left = r.left + Ui.dp(14), right = r.right - Ui.dp(14);
        float y = r.top + Ui.dp(14), h = Ui.dp(6);
        Ui.roundRect(c, left, y, right, y + h, h / 2f, Ui.alpha(Color.WHITE, 70));
        float level = Ui.clamp(g.sensors.blow.level() / (g.sensors.blow.threshold() * 1.5f), 0f, 1f);
        if (level > 0f) Ui.roundRect(c, left, y, left + (right - left) * level, y + h, h / 2f, level >= 0.66f ? Ui.STAR : Color.WHITE);
        float tick = left + (right - left) * 0.667f;
        c.drawRect(tick - Ui.dp(1), y - Ui.dp(2), tick + Ui.dp(1), y + h + Ui.dp(2), Ui.fill(Color.WHITE));
    }

    // ---------- Compte à rebours ----------

    public void drawCountdown(Canvas c) {
        World theme = theme();
        RectF board = layout.board;
        float bw = board.width(), bh = board.height();
        Ui.roundRect(c, board.left, board.top, board.right, board.bottom, Ui.dp(16), Ui.alpha(theme.bg, 175));

        float cx = board.centerX();
        int shown = Math.max(1, (int) Math.ceil(Math.max(0f, countdown.left())));
        float frac = countdown.left() - (float) Math.floor(countdown.left());
        float size = bh * 0.42f * (1f + 0.25f * frac);
        Ui.title(c, String.valueOf(shown), cx, board.top + bh * 0.32f + size * 0.35f, size, theme.hud, Paint.Align.CENTER, 0f);

        float fs = Math.max(Ui.sp(16), bh * 0.045f);
        float y = board.top + bh * 0.58f;
        int warning = theme.dark ? Color.WHITE : Ui.DANGER;
        float maxW = bw * 0.94f;
        Ui.title(c, world.level.name, cx, y, fs * 1.4f, theme.hud, Paint.Align.CENTER, bw * 0.9f);
        y += fs * 1.8f;
        if (world.level.strobe) {
            Ui.text(c, "ATTENTION : lumières clignotantes (réglable dans Réglages > Effets lumineux).", cx, y, fs, warning, true, Paint.Align.CENTER, maxW);
            y += fs * 1.5f;
            Ui.text(c, "Le plateau s'éteint : mémorise ce que tu vois !", cx, y, fs, theme.hud, false, Paint.Align.CENTER, maxW);
        } else {
            Ui.text(c, countdown.isResume() ? "Reprise dans un instant" : "Prépare-toi !", cx, y, fs, theme.hud, true, Paint.Align.CENTER, maxW);
        }
        y += fs * 1.5f;
        Ui.text(c, controlHint(), cx, y, fs, theme.hud, false, Paint.Align.CENTER, maxW);
        if (!g.sensors.blow.isAvailable()) {
            y += fs * 1.5f;
            Ui.text(c, "Micro indisponible : utilise le bouton Souffle.", cx, y, fs, warning, true, Paint.Align.CENTER, maxW);
        }
    }

    private String controlHint() {
        if (g.progress.controlMode == ControlMode.JOYSTICK) return "Pose le pouce dans la zone manette : un joystick apparaît.";
        return g.progress.calibFlat ? "Garde la tablette à plat : c'est la position neutre."
                : "Tiens l'appareil comme pour jouer : c'est la position neutre.";
    }

    // ---------- Message ----------

    public void drawToast(Canvas c) {
        Effects fx = world.effects;
        float opacity = fx.toastOpacity();
        if (opacity <= 0f) return;
        RectF board = layout.board;
        String text = fx.toastText();
        float size = Math.max(Ui.sp(20), board.height() * 0.055f);
        float textW = Ui.measure(text, size, true);
        float cx = board.centerX(), base = board.top + board.height() * 0.12f;
        float padX = Ui.dp(18), padY = Ui.dp(10);
        float top = base - size * 0.85f - padY * 0.4f, bottom = base + size * 0.30f + padY * 0.4f;
        Ui.roundRect(c, cx - textW / 2f - padX, top, cx + textW / 2f + padX, bottom, (bottom - top) / 2f,
                Ui.alpha(fx.toastColor(), (int) (235 * opacity)));
        Ui.text(c, text, cx, base, size, Ui.alpha(Color.WHITE, (int) (255 * opacity)), true);
    }
}
