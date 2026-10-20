package com.example.expresseequilibre.game;

import com.example.expresseequilibre.data.Progress;
import com.example.expresseequilibre.input.Feedback;
import com.example.expresseequilibre.input.SensorController;
import com.example.expresseequilibre.screens.EndScreen;
import com.example.expresseequilibre.screens.LevelScreen;
import com.example.expresseequilibre.screens.PauseScreen;
import com.example.expresseequilibre.screens.Screen;
import com.example.expresseequilibre.screens.SettingsScreen;
import com.example.expresseequilibre.screens.StartScreen;
import com.example.expresseequilibre.screens.TutorialScreen;
import com.example.expresseequilibre.ui.Ui;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Chef d'orchestre : boucle update/draw, machine à états, routage tactile (multi-touch). */
public class GameView extends View {

    public interface Host {
        void requestMicPermission(Runnable afterwards);
    }

    public enum GameState { START, LEVELS, TUTORIAL, SETTINGS, PLAYING, PAUSED, WIN, LOSE }

    public final SensorController sensors;
    public final Feedback feedback;
    public final Progress progress;
    public final PlayScreen playScreen;

    private final Host host;
    private final SharedPreferences prefs;
    private final StartScreen startScreen;
    private final LevelScreen levelScreen;
    private final TutorialScreen tutorialScreen;
    private final SettingsScreen settingsScreen;
    private final PauseScreen pauseScreen;
    private final EndScreen endScreen;

    private GameState state = GameState.START;
    private Screen current;

    public int insetTop, insetBottom, insetLeft, insetRight;

    // Niveau courant
    public int curLevel;
    public int pendingLevel = -1;

    // Résultat de la dernière partie
    public boolean lastWin, lastRecord;
    public int lastStars, lastScore, lastSeconds, bestScore;
    public float lastRunTime;

    private boolean settingsFromPause;
    private float transition;
    private long lastNanos;
    private boolean running;

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = lastNanos == 0 ? 0.016f : Math.min(0.033f, (now - lastNanos) / 1_000_000_000f);
            lastNanos = now;
            update(dt);
            invalidate();
            postOnAnimation(this);
        }
    };

    public GameView(Context context, Host host) {
        super(context);
        this.host = host;
        Ui.init(context);
        prefs = context.getSharedPreferences("equilibre_express", Context.MODE_PRIVATE);
        progress = new Progress(prefs);
        sensors = new SensorController(context);
        sensors.setFlatReference(progress.calibFlat);
        sensors.setMicSensitivity(progress.micSens);
        feedback = new Feedback(context, prefs);
        bestScore = prefs.getInt("best", 0);

        startScreen = new StartScreen(this);
        levelScreen = new LevelScreen(this);
        tutorialScreen = new TutorialScreen(this);
        settingsScreen = new SettingsScreen(this);
        playScreen = new PlayScreen(this);
        pauseScreen = new PauseScreen(this);
        endScreen = new EndScreen(this);

        setFocusable(true);
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            insetTop = i.top;
            insetBottom = i.bottom;
            insetLeft = Math.max(i.left, i.right);
            insetRight = insetLeft;
            return insets;
        });
        setState(GameState.START);
    }

    public GameState getState() { return state; }

    // ---------- Boucle de jeu ----------

    public void startGame() {
        if (running) return;
        running = true;
        lastNanos = 0;
        sensors.start();
        sensors.setMicActive(micWanted());
        postOnAnimation(gameLoop);
    }

    public void stopGame() {
        running = false;
        removeCallbacks(gameLoop);
        sensors.stop();
    }

    public void pauseIfPlaying() {
        if (state == GameState.PLAYING) pauseGame();
    }

    private void update(float dt) {
        current.update(dt);
        if (transition > 0f) transition = Math.max(0f, transition - dt * 4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (transition > 0f) canvas.translate(transition * transition * getWidth() * 0.06f, 0f); // petit glissement
        current.draw(canvas, getWidth(), getHeight());
        canvas.restore();
        if (transition > 0f) {
            // Fondu de la couleur du monde en jeu (jamais d'éclair clair sur un monde sombre).
            int tc = state == GameState.PLAYING ? World.of(playScreen.worldIndex()).bg : Ui.BG;
            Ui.dim(canvas, getWidth(), getHeight(), Ui.alpha(tc, (int) (transition * 255)));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopGame();
        feedback.release();
        super.onDetachedFromWindow();
    }

    // ---------- États et navigation ----------

    /** Le micro n'écoute qu'en jeu et dans les Réglages (test de sensibilité). */
    private boolean micWanted() {
        return state == GameState.PLAYING || state == GameState.SETTINGS;
    }

    /** Demande la permission micro si besoin, puis (ré)active l'écoute si l'écran en a besoin. */
    public void requestMic() {
        host.requestMicPermission(() -> sensors.setMicActive(micWanted()));
    }

    public void setState(GameState s) {
        state = s;
        switch (s) {
            case START: current = startScreen; break;
            case LEVELS: current = levelScreen; break;
            case TUTORIAL: current = tutorialScreen; break;
            case SETTINGS: current = settingsScreen; break;
            case PLAYING: current = playScreen; break;
            case PAUSED: current = pauseScreen; break;
            default: current = endScreen; break;
        }
        sensors.setMicActive(micWanted());
        transition = (s == GameState.PAUSED) ? 0f : 1f;
        current.onEnter();
    }

    public void goStart() { setState(GameState.START); }

    public void goLevels() { setState(GameState.LEVELS); }

    public void goTutorial() { setState(GameState.TUTORIAL); }

    public void goSettings() {
        settingsFromPause = false;
        setState(GameState.SETTINGS);
    }

    /** Réglages ouverts depuis la pause : la partie en cours est conservée. */
    public void goSettingsFromPause() {
        settingsFromPause = true;
        setState(GameState.SETTINGS);
    }

    public void closeSettings() {
        if (settingsFromPause) {
            settingsFromPause = false;
            setState(GameState.PAUSED);
        } else {
            goStart();
        }
    }

    /** "Jouer" sur l'accueil : choix du niveau. */
    public void onPlayPressed() { goLevels(); }

    public void startLevel(int lv) {
        curLevel = lv;
        if (!prefs.getBoolean("tutorialSeen", false)) {
            pendingLevel = lv;
            goTutorial();
        } else {
            launch();
        }
    }

    public void onTutorialDone() {
        prefs.edit().putBoolean("tutorialSeen", true).apply();
        if (pendingLevel >= 0) {
            curLevel = pendingLevel;
            pendingLevel = -1;
            launch();
        } else {
            goStart();
        }
    }

    private void launch() { host.requestMicPermission(this::beginPlay); }

    public void beginPlay() {
        playScreen.load(Levels.ALL[curLevel]);
        playScreen.reset();
        setState(GameState.PLAYING);
    }

    public void pauseGame() { setState(GameState.PAUSED); }

    public void resumeGame() {
        playScreen.beginCountdown(true);
        setState(GameState.PLAYING);
    }

    public void finishGame(boolean win, int stars, int score, int secondsLeft, float runTime) {
        lastWin = win;
        lastStars = stars;
        lastScore = score;
        lastSeconds = secondsLeft;
        lastRunTime = runTime;
        lastRecord = score > bestScore;
        if (lastRecord) {
            bestScore = score;
            prefs.edit().putInt("best", score).apply();
        }
        if (win) progress.record(curLevel, stars);
        setState(win ? GameState.WIN : GameState.LOSE);
    }

    public boolean handleBack() {
        switch (state) {
            case PLAYING: pauseGame(); return true;
            case PAUSED: resumeGame(); return true;
            case TUTORIAL:
                if (pendingLevel >= 0) { pendingLevel = -1; goLevels(); }
                else goStart();
                return true;
            case SETTINGS: closeSettings(); return true;
            case LEVELS: goStart(); return true;
            case WIN:
            case LOSE: goLevels(); return true;
            default: return false;
        }
    }

    // ---------- Tactile (multi-touch : chaque doigt a un identifiant) ----------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int idx = e.getActionIndex();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                current.onDown(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    current.onMove(e.getPointerId(i), e.getX(i), e.getY(i));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                current.onUp(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                current.onCancel();
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
