package com.example.expresseequilibre.game;

/** Mur rectangulaire du plateau (coordonnées logiques). Un mur fragile peut être cassé. */
public final class Wall {
    public final float x, y, w, h;
    public final boolean fragile;

    public Wall(float x, float y, float w, float h, boolean fragile) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.fragile = fragile;
    }

    public float right() { return x + w; }

    public float bottom() { return y + h; }
}
