package com.example.expresseequilibre.game;

/** Trou circulaire : la bille y tombe puis réapparaît en (respawnX, respawnY). */
public final class Hole {
    public final float x, y, r, respawnX, respawnY;

    public Hole(float x, float y, float r, float respawnX, float respawnY) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.respawnX = respawnX;
        this.respawnY = respawnY;
    }
}
