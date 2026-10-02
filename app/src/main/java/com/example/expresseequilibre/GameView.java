package com.example.expresseequilibre;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.MotionEvent;

public class GameView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float ballX;
    private float ballY;
    private static final float BALL_RADIUS = 35f;

    private float ballSpeedX = 6f;
    private float ballSpeedY = 4f;
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        // Initialisation seulement lors de la première apparition de la vue.
        if (ballX == 0 && ballY == 0) {
            ballX = width * 0.20f;
            ballY = height * 0.50f;
        }
    }
    final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            update();
            invalidate(); // demande un nouveau dessin
            postDelayed(this, 16); // environ 60 mises à jour/s
        }
    };
    public void startGame() {
        post(gameLoop);
    }
    public void stopGame() {
        removeCallbacks(gameLoop);
    }

    private boolean playerWon = false;


    public GameView(Context context) {
        super(context);

        setFocusable(true);
    }
    enum GameState {
        START,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private GameState gameState = GameState.START;
    private void update() {
        // Rien ne bouge en dehors d'une partie en cours.
        if (gameState != GameState.PLAYING) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Tant que la vue n'est pas encore mesurée, sa taille peut être 0.
        if (width <= 0 || height <= 0) {
            return;
        }

        // Zone de jeu calculée à partir de la taille réelle de la vue.
        float margin = width * 0.06f;
        float top = height * 0.18f;
        float bottom = height * 0.85f;

        // Déplacement.
        ballX += ballSpeedX;
        ballY += ballSpeedY;

        // Rebond horizontal.
        if (ballX - BALL_RADIUS < margin) {
            ballX = margin + BALL_RADIUS;
            ballSpeedX = Math.abs(ballSpeedX);
        }

        if (ballX + BALL_RADIUS > width - margin) {
            ballX = width - margin - BALL_RADIUS;
            ballSpeedX = -Math.abs(ballSpeedX);
        }

        // Rebond vertical.
        if (ballY - BALL_RADIUS < top) {
            ballY = top + BALL_RADIUS;
            ballSpeedY = Math.abs(ballSpeedY);
        }

        if (ballY + BALL_RADIUS > bottom) {
            ballY = bottom - BALL_RADIUS;
            ballSpeedY = -Math.abs(ballSpeedY);
        }
    }

    private void drawGameScreen(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        // Fond de l'écran.
        canvas.drawColor(Color.rgb(220, 240, 255));

        // Dimensions du terrain, proportionnelles à l'écran.
        float top = height * 0.18f;
        float bottom = height * 0.85f;
        float cornerRadius = width * 0.05f;

        // Zone de jeu.
        paint.setColor(Color.rgb(245, 0, 0));
        RectF gameArea = new RectF(
                (float) width,
                top,
                width - (float) width,
                bottom
        );
        canvas.drawRoundRect(gameArea, cornerRadius, cornerRadius, paint);

        // Texte de statut.
        paint.setColor(Color.rgb(30, 70, 120));
        paint.setTextSize(width * 0.06f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(
                "EN JEU",
                width / 2f,
                height * 0.10f,
                paint
        );

        // Bille.
        paint.setColor(Color.rgb(33, 150, 243));
        canvas.drawCircle(ballX, ballY, BALL_RADIUS, paint);

        // Reflet de la bille.
        paint.setColor(Color.WHITE);
        canvas.drawCircle(
                ballX - BALL_RADIUS * 0.35f,
                ballY - BALL_RADIUS * 0.35f,
                BALL_RADIUS * 0.28f,
                paint
        );

        // Consigne placée sous le terrain.
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(width * 0.045f);
        canvas.drawText(
                "La bille rebondit dans la zone de jeu",
                width / 2f,
                height * 0.93f,
                paint
        );
    }

    private void drawStartScreen(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(Color.rgb(220, 240, 255));

        paint.setTextAlign(Paint.Align.CENTER);

        paint.setColor(Color.rgb(30, 70, 120));
        paint.setTextSize(width * 0.09f);
        canvas.drawText(
                "ÉQUILIBRE EXPRESS",
                width / 2f,
                height * 0.35f,
                paint
        );

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(width * 0.055f);
        canvas.drawText(
                "Toucher pour jouer",
                width / 2f,
                height * 0.55f,
                paint
        );
    }

    private void drawPauseScreen(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(Color.rgb(30, 70, 120));

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(width * 0.10f);
        canvas.drawText(
                "PAUSE",
                width / 2f,
                height * 0.45f,
                paint
        );

        paint.setTextSize(width * 0.05f);
        canvas.drawText(
                "Toucher pour reprendre",
                width / 2f,
                height * 0.60f,
                paint
        );
    }

    private void drawGameOverScreen(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(Color.rgb(198, 40, 40));

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(width * 0.10f);
        canvas.drawText(
                "GAME OVER",
                width / 2f,
                height * 0.45f,
                paint
        );

        paint.setTextSize(width * 0.05f);
        canvas.drawText(
                "Toucher pour recommencer",
                width / 2f,
                height * 0.60f,
                paint
        );
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (gameState) {
            case START:
                drawStartScreen(canvas);
                break;
            case PLAYING:
                drawGameScreen(canvas);
                break;
            case PAUSED:
                drawPauseScreen(canvas);
                break;
            case GAME_OVER:
                drawGameOverScreen(canvas);
                break;
        }

        int width = getWidth();
        int height = getHeight();

        // Fond.
        canvas.drawColor(Color.rgb(220, 240, 255));

        // Zone de jeu
        float margin = 40;
        float top = 170;
        float bottom = height - 170;

        paint.setColor(Color.rgb(245, 245, 245));
        @SuppressLint("DrawAllocation") RectF gameArea = new RectF(margin, top, width - margin, bottom);
        canvas.drawRoundRect(gameArea, 30, 30, paint);

        // Bille : sa position est maintenant fournie par ballX / ballY.
        paint.setColor(Color.rgb(33, 150, 243));
        canvas.drawCircle(ballX, ballY, 35, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(ballX - 12, ballY - 12, 10, paint);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        if (gameState == GameState.START) {
            gameState = GameState.PLAYING;
            invalidate();
            return true;
        }

        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING;
            invalidate();
            return true;
        }

        if (gameState == GameState.GAME_OVER) {
            ballX = getWidth() * 0.20f;
            ballY = getHeight() * 0.50f;
            gameState = GameState.PLAYING;
            invalidate();
            return true;
        }

        return true;
    }
}