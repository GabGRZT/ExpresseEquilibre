package com.example.expresseequilibre;

import android.graphics.Canvas;

/** Un écran = une classe : update (état), draw (rendu), événements tactiles. */
abstract class Screen {
    protected final GameView g;

    Screen(GameView g) { this.g = g; }

    void onEnter() { }

    void update(float dt) { }

    abstract void draw(Canvas c, int w, int h);

    void onDown(float x, float y) { }

    void onMove(float x, float y) { }

    void onUp(float x, float y) { }

    void onCancel() { }
}