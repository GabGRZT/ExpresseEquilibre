package com.example.expresseequilibre.game;

/** Un bonus ou malus posé sur le plateau. */
public final class PickupSpot {
    public final float x, y;
    public final Pickup type;

    public PickupSpot(float x, float y, Pickup type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}
