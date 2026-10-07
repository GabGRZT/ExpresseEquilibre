package com.example.expresseequilibre;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Chef d'orchestre : boucle update/draw, machine à états, musique, routage tactile. */
public class GameView extends View {

    public interface Host {
        void requestMicPermission(Runnable afterwards);
    }

    public enum GameState { START, MODES, LEVELS, SKINS, WARNING, TUTORIAL, PLAYING, PAUSED, WIN, LOSE }

    final SensorController sensors;
    final Feedback feedback;
    final Progress progress;
    final PlayScreen playScreen;

    private final Host host;
    private final SharedPreferences prefs;
    private final StartScreen startScreen;
    private final ModeScreen modeScreen;
    private final LevelScreen levelScreen;
    private final SkinScreen skinScreen;
    private final WarnScreen warnScreen;
    private final TutorialScreen tutorialScreen;
    private final PauseScreen pauseScreen;
    private final EndScreen endScreen;

    private GameState state = GameState.START;
    private Screen current;

    int insetTop, insetBottom, insetLeft, insetRight;

    boolean ghostMode;
    int curLevel;
    int pendingLevel = -1;

    // Résultat de la dernière partie
    boolean lastWin, lastRecord, lastNewGhost;
    int lastStars, lastScore, lastSeconds, bestScore, lastMedal, lastUnlockedSkin = -1;
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
        skinScreen = new SkinScreen(this);
        warnScreen = new WarnScreen(this);
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
        feedback.sound().onAppResume();
        postOnAnimation(gameLoop);
    }

    public void stopGame() {
        running = false;
        removeCallbacks(gameLoop);
        sensors.stop();
        feedback.sound().onAppPause();
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
        if (transition > 0f) canvas.translate(transition * transition * getWidth() * 0.06f, 0f);
        current.draw(canvas, getWidth(), getHeight());
        canvas.restore();
        if (transition > 0f) {
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
            case SKINS: current = skinScreen; break;
            case WARNING: current = warnScreen; break;
            case TUTORIAL: current = tutorialScreen; break;
            case PLAYING: current = playScreen; break;
            case PAUSED: current = pauseScreen; break;
            default: current = endScreen; break;
        }
        sensors.setMicActive(s == GameState.PLAYING);
        transition = (s == GameState.PAUSED) ? 0f : 1f;
        updateMusic();
        current.onEnter();
    }

    /** Musique : menus = thème principal ; en jeu = thème du monde, volume bas, seulement si activé. */
    private void updateMusic() {
        int mode = progress.musicMode();
        int theme = -1;
        float vol = 0.5f;
        switch (state) {
            case PLAYING:
                if (mode == 2) { theme = Math.max(0, Math.min(3, playScreen.worldIndex())); vol = 0.16f; }
                break;
            case PAUSED:
            case WIN:
            case LOSE:
                break;
            default:
                if (mode >= 1) theme = 0;
                break;
        }
        feedback.sound().setMusic(theme, vol);
    }

    void cycleMusicMode() {
        progress.cycleMusicMode();
        updateMusic();
    }

    void goStart() { setState(GameState.START); }

    void goModes() { setState(GameState.MODES); }

    void goSkins() { setState(GameState.SKINS); }

    void goLevels(boolean ghost) {
        ghostMode = ghost;
        setState(GameState.LEVELS);
    }

    void goTutorial() { setState(GameState.TUTORIAL); }

    void onPlayPressed() { goModes(); }

    /** Lancement d'un niveau : avertissement stroboscope -> tutoriel (1re fois) -> micro -> jeu. */
    void startLevel(int lv) {
        curLevel = lv;
        if (Levels.ALL[lv].strobeHz > 0f && !progress.reducedFx) setState(GameState.WARNING);
        else proceedAfterWarning();
    }

    void proceedAfterWarning() {
        if (!prefs.getBoolean("tutorialSeen", false)) {
            pendingLevel = curLevel;
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
                    float runTime, float ghostDelta, boolean newGhost, int medal) {
        lastWin = win;
        lastStars = stars;
        lastScore = score;
        lastSeconds = secondsLeft;
        lastRunTime = runTime;
        lastGhostDelta = ghostDelta;
        lastNewGhost = newGhost;
        lastMedal = medal;
        lastRecord = score > bestScore;
        if (lastRecord) {
            bestScore = score;
            prefs.edit().putInt("best", score).apply();
        }
        int before = progress.totalStars();
        if (win) progress.record(curLevel, stars, medal);
        int after = progress.totalStars();
        lastUnlockedSkin = -1;
        Skin[] sk = Skin.values();
        for (int i = sk.length - 1; i >= 0; i--) {
            if (sk[i].unlock > before && sk[i].unlock <= after) { lastUnlockedSkin = i; break; }
        }
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
            case WARNING: goLevels(ghostMode); return true;
            case SKINS: goModes(); return true;
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