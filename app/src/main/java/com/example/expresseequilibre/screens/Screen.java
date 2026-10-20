package com.example.expresseequilibre.screens;

import android.graphics.Canvas;

import com.example.expresseequilibre.game.GameView;

/** Un écran = une classe : update (état), draw (rendu), événements tactiles. */
public abstract class Screen {
    protected final GameView g;

    public Screen(GameView g) { this.g = g; }

    public void onEnter() { }

    public void update(float dt) { }

    public abstract void draw(Canvas c, int w, int h);

    // Version simple (un seul doigt) : suffisante pour les menus.
    public void onDown(float x, float y) { }

    public void onMove(float x, float y) { }

    public void onUp(float x, float y) { }

    // Version multi-touch (avec identifiant du doigt) : par défaut, renvoie vers la version simple.
    public void onDown(int id, float x, float y) { onDown(x, y); }

    public void onMove(int id, float x, float y) { onMove(x, y); }

    public void onUp(int id, float x, float y) { onUp(x, y); }

    public void onCancel() { }
}
