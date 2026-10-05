package com.example.expresseequilibre;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Chef d'orchestre : boucle update/draw, machine à états, routage tactile.
 * Chaque écran est une classe à part (StartScreen, PlayScreen, ...).
 */
public class GameView extends View {

    public interface Host {
        /** Demande la permission micro (si besoin) puis exécute "afterwards" quoi qu'il arrive. */
        void requestMicPermission(Runnable afterwards);
    }

    public enum GameState { START, TUTORIAL, PLAYING, PAUSED, WIN, LOSE }

    final SensorController sensors;
    final Feedback feedback;
    final PlayScreen playScreen;

    private final Host host;
    private final SharedPreferences prefs;
    private final StartScreen startScreen;
    private final TutorialScreen tutorialScreen;
    private final PauseScreen pauseScreen;
    private final EndScreen endScreen;

    private GameState state = GameState.START;
    private Screen current;

    // Zones à éviter (encoche, barres système)
    int insetTop, insetBottom, insetLeft, insetRight;

    // Résultat de la dernière partie
    boolean lastWin, lastRecord;
    int lastStars, lastScore, lastSeconds, bestScore;

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
            postOnAnimation(this); // synchronisé sur l'affichage (~60 Hz), dt mesuré
        }
    };

    public GameView(Context context, Host host) {
        super(context);
        this.host = host;
        Ui.init(context);
        prefs = context.getSharedPreferences("equilibre_express", Context.MODE_PRIVATE);
        sensors = new SensorController(context);
        feedback = new Feedback(context, prefs);
        bestScore = prefs.getInt("best", 0);

        startScreen = new StartScreen(this);
        tutorialScreen = new TutorialScreen(this);
        playScreen = new PlayScreen(this);
        pauseScreen = new PauseScreen(this);
        endScreen = new EndScreen(this);

        setFocusable(true);
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            insetTop = i.top;
            insetBottom = i.bottom;
            insetLeft = Math.max(i.left, i.right); // Uniformise les marges latérales
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
        current.draw(canvas, getWidth(), getHeight());
        if (transition > 0f) Ui.dim(canvas, getWidth(), getHeight(), Ui.alpha(Ui.BG, (int) (transition * 255)));
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
            case TUTORIAL: current = tutorialScreen; break;
            case PLAYING: current = playScreen; break;
            case PAUSED: current = pauseScreen; break;
            default: current = endScreen; break;
        }
        sensors.setMicActive(s == GameState.PLAYING); // le micro n'écoute que pendant la partie
        transition = (s == GameState.PAUSED) ? 0f : 1f;
        current.onEnter();
    }

    void goStart() { setState(GameState.START); }

    void goTutorial() { setState(GameState.TUTORIAL); }

    void onPlayPressed() {
        if (prefs.getBoolean("tutorialSeen", false)) startWithMic();
        else goTutorial();
    }

    void onTutorialDone() {
        prefs.edit().putBoolean("tutorialSeen", true).apply();
        startWithMic();
    }

    private void startWithMic() { host.requestMicPermission(this::beginPlay); }

    void beginPlay() {
        playScreen.reset();
        setState(GameState.PLAYING);
    }

    void pauseGame() { setState(GameState.PAUSED); }

    void resumeGame() {
        playScreen.beginCountdown(true);
        setState(GameState.PLAYING);
    }

    void finishGame(boolean win, int stars, int score, int secondsLeft) {
        lastWin = win;
        lastStars = stars;
        lastScore = score;
        lastSeconds = secondsLeft;
        lastRecord = score > bestScore;
        if (lastRecord) {
            bestScore = score;
            prefs.edit().putInt("best", score).apply();
        }
        setState(win ? GameState.WIN : GameState.LOSE);
    }

    /** Bouton retour : @return true si l'événement est consommé. */
    boolean handleBack() {
        switch (state) {
            case PLAYING: pauseGame(); return true;
            case PAUSED: resumeGame(); return true;
            case TUTORIAL:
            case WIN:
            case LOSE: goStart(); return true;
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