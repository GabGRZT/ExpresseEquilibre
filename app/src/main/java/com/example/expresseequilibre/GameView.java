package com.example.expresseequilibre;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Chef d'orchestre : boucle update/draw, machine à états, routage tactile. */
public class GameView extends View {

    public interface Host {
        void requestMicPermission(Runnable afterwards);
    }

    public enum GameState { START, MODES, LEVELS, TUTORIAL, PLAYING, PAUSED, WIN, LOSE }

    final SensorController sensors;
    final Feedback feedback;
    final Progress progress;
    final PlayScreen playScreen;

    private final Host host;
    private final SharedPreferences prefs;
    private final StartScreen startScreen;
    private final ModeScreen modeScreen;
    private final LevelScreen levelScreen;
    private final TutorialScreen tutorialScreen;
    private final PauseScreen pauseScreen;
    private final EndScreen endScreen;

    private GameState state = GameState.START;
    private Screen current;

    int insetTop, insetBottom, insetLeft, insetRight;

    // Mode et niveau courants
    boolean ghostMode;
    int curLevel;
    int pendingLevel = -1;

    // Résultat de la dernière partie
    boolean lastWin, lastRecord, lastNewGhost;
    int lastStars, lastScore, lastSeconds, bestScore;
    float lastRunTime, lastGhostDelta = Float.NaN;

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
        feedback = new Feedback(context, prefs);
        bestScore = prefs.getInt("best", 0);

        startScreen = new StartScreen(this);
        modeScreen = new ModeScreen(this);
        levelScreen = new LevelScreen(this);
        tutorialScreen = new TutorialScreen(this);
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

    // ---------- Boucle de jeu ----------

    public void startGame() {
        if (running) return;
        running = true;
        lastNanos = 0;
        sensors.start();
        sensors.setMicActive(state == GameState.PLAYING);
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

    void setState(GameState s) {
        state = s;
        switch (s) {
            case START: current = startScreen; break;
            case MODES: current = modeScreen; break;
            case LEVELS: current = levelScreen; break;
            case TUTORIAL: current = tutorialScreen; break;
            case PLAYING: current = playScreen; break;
            case PAUSED: current = pauseScreen; break;
            default: current = endScreen; break;
        }
        sensors.setMicActive(s == GameState.PLAYING);
        transition = (s == GameState.PAUSED) ? 0f : 1f;
        current.onEnter();
    }

    void goStart() { setState(GameState.START); }

    void goModes() { setState(GameState.MODES); }

    void goLevels(boolean ghost) {
        ghostMode = ghost;
        setState(GameState.LEVELS);
    }

    void goTutorial() { setState(GameState.TUTORIAL); }

    /** "Jouer" sur l'accueil : choix du mode. */
    void onPlayPressed() { goModes(); }

    void startLevel(int lv) {
        curLevel = lv;
        if (!prefs.getBoolean("tutorialSeen", false)) {
            pendingLevel = lv;
            goTutorial();
        } else {
            launch();
        }
    }

    void onTutorialDone() {
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

    void beginPlay() {
        playScreen.load(Levels.ALL[curLevel], curLevel, ghostMode);
        playScreen.reset();
        setState(GameState.PLAYING);
    }

    void pauseGame() { setState(GameState.PAUSED); }

    void resumeGame() {
        playScreen.beginCountdown(true);
        setState(GameState.PLAYING);
    }

    void finishGame(boolean win, int stars, int score, int secondsLeft,
                    float runTime, float ghostDelta, boolean newGhost) {
        lastWin = win;
        lastStars = stars;
        lastScore = score;
        lastSeconds = secondsLeft;
        lastRunTime = runTime;
        lastGhostDelta = ghostDelta;
        lastNewGhost = newGhost;
        lastRecord = score > bestScore;
        if (lastRecord) {
            bestScore = score;
            prefs.edit().putInt("best", score).apply();
        }
        if (win) progress.record(curLevel, stars);
        setState(win ? GameState.WIN : GameState.LOSE);
    }

    boolean handleBack() {
        switch (state) {
            case PLAYING: pauseGame(); return true;
            case PAUSED: resumeGame(); return true;
            case TUTORIAL:
                if (pendingLevel >= 0) { pendingLevel = -1; goLevels(ghostMode); }
                else goStart();
                return true;
            case LEVELS: goModes(); return true;
            case MODES: goStart(); return true;
            case WIN:
            case LOSE: goLevels(ghostMode); return true;
            default: return false;
        }
    }

    // ---------- Tactile ----------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int idx = e.getActionIndex();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                current.onDown(e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_MOVE:
                current.onMove(e.getX(), e.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                current.onUp(e.getX(idx), e.getY(idx));
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