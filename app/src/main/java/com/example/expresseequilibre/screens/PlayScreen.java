package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.BoardRenderer;
import com.example.expresseequilibre.game.Countdown;
import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.game.GameWorld;
import com.example.expresseequilibre.game.Joystick;
import com.example.expresseequilibre.game.Level;
import com.example.expresseequilibre.game.PlayHud;
import com.example.expresseequilibre.game.TouchLayout;
import com.example.expresseequilibre.game.World;
import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.ToneGenerator;


/**
 * Écran de jeu : simple chef d'orchestre. Les règles sont dans GameWorld, le dessin dans BoardRenderer et PlayHud,
 * la disposition dans TouchLayout, la manette dans Joystick.
 */
public final class PlayScreen extends Screen {
    private final UiButton actionButton = add("Saut", Ui.BUTTON, Icons::jump, null);   // déclenché au toucher, pas au relâchement
    private final UiButton blowButton = add("Souffle", Ui.TEAL, Icons::wind, null);
    private final UiButton pauseButton = add("Pause", Ui.SLATE, Icons::pause, g::pauseGame);

    private final GameWorld world;
    private final TouchLayout layout = new TouchLayout();
    private final Joystick joystick = new Joystick();
    private final Countdown countdown = new Countdown();
    private final BoardRenderer renderer;
    private final PlayHud hud;
    private final float[] move = new float[2]; // intention du joueur lue à chaque image

    public PlayScreen(GameView g) {
        super(g);
        world = new GameWorld(g.feedback, g.progress);
        renderer = new BoardRenderer(world, layout, g.progress);
        hud = new PlayHud(g, world, layout, countdown, pauseButton, actionButton, blowButton);
        actionButton.fillVertical = true;
        blowButton.fillVertical = true;
    }

    // ---------- Cycle d'une partie ----------

    public void load(Level level) { world.load(level); }

    public void reset() {
        world.reset();
        beginCountdown(false);
    }

    public void beginCountdown(boolean afterPause) {
        countdown.start(afterPause);
        world.setRunning(false);
        joystick.reset(); // un doigt resté posé avant la reprise ne pilote plus
    }

    public int starCount() { return world.starCount; }

    public float timeLeft() { return world.timeLeft; }

    public int worldIndex() { return world.level.world; }

    private void startRun() {
        world.setRunning(true);
        g.sensors.tilt.calibrate(); // mode « à plat » : aucun décalage ; sinon la position tenue devient le neutre
        world.effects.toast("GO !", Ui.OK);
        g.feedback.cue(40, ToneGenerator.TONE_PROP_BEEP2, 200);
    }

    @Override
    public void update(float dt) {
        int w = g.getWidth(), h = g.getHeight();
        if (w <= 0 || h <= 0) return;
        arrange(w, h);

        world.effects.update(dt);
        g.sensors.blow.update(dt);
        boolean shake = g.sensors.tilt.consumeShake(); // lus à chaque image, même pendant le décompte
        boolean blow = g.sensors.blow.consume();

        if (countdown.isActive()) {
            if (countdown.update(dt, g.feedback)) startRun();
            return;
        }
        if (shake) world.action();
        if (blow) world.fan();
        readMove();
        world.update(dt, move[0], move[1]);

        if (world.outcome != GameWorld.Outcome.PLAYING) {
            g.finishGame(world.outcome == GameWorld.Outcome.WON, world.starCount, world.score(),
                    (int) world.timeLeft, world.runTime);
        }
    }

    /** Inclinaison de l'appareil ou manette virtuelle, selon le réglage. */
    private void readMove() {
        if (g.progress.controlMode == ControlMode.TILT) {
            move[0] = g.sensors.tilt.tiltX();
            move[1] = g.sensors.tilt.tiltY();
        } else {
            joystick.read(move);
        }
    }

    // ---------- Dessin ----------

    private void arrange(int w, int h) {
        layout.update(w, h, g);
        joystick.setZone(layout.zone, layout.board.height());
    }

    /** Le plateau seul (utilisé aussi par l'écran de pause, en arrière-plan). */
    public void drawWorld(Canvas c, int w, int h) {
        arrange(w, h);
        renderer.draw(c);
    }

    @Override
    public void draw(Canvas c, int w, int h) {
        drawWorld(c, w, h);
        hud.drawTopBar(c);
        if (layout.hasJoystick()) {
            World theme = World.of(world.level.world);
            joystick.draw(c, theme.dark ? Color.WHITE : Ui.PRIMARY, theme.hud, world.isRunning() && world.runTime < 6f);
        }
        hud.drawControls(c, h);
        if (countdown.isActive()) hud.drawCountdown(c);
        hud.drawToast(c);
    }

    // ---------- Tactile (multi-touch) ----------

    @Override
    public void onDown(int id, float x, float y) {
        boolean onAction = actionButton.contains(x, y), onBlow = blowButton.contains(x, y);
        boolean onButton = onAction || onBlow || pauseButton.contains(x, y);
        super.onDown(id, x, y);
        if (world.isRunning()) { // saut et souffle se déclenchent dès le toucher
            if (onAction) world.action();
            if (onBlow) world.fan();
        }
        if (!onButton && layout.hasJoystick()) joystick.start(id, x, y);
    }

    @Override
    public void onMove(int id, float x, float y) {
        if (!joystick.move(id, x, y)) super.onMove(id, x, y);
    }

    @Override
    public void onUp(int id, float x, float y) {
        if (!joystick.release(id)) super.onUp(id, x, y);
    }

    @Override
    public void onCancel() {
        joystick.reset();
        super.onCancel();
    }
}
