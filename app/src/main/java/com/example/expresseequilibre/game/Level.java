package com.example.expresseequilibre.game;

/**
 * Un niveau, en coordonnées LOGIQUES (plateau fixe de 1600 x 900) :
 * le niveau est identique sur tous les écrans.
 */
public final class Level {
    public static final float W = 1600f, H = 900f, EXIT_R = 80f;

    public final String name;
    public final int world;               // thème visuel (voir World)
    public final float time, friction;    // durée (s) et adhérence (1 = normale, < 1 = glissant)
    public final boolean strobe;          // niveau stroboscopique
    public final float startX, startY, exitX, exitY;
    public final Wall[] walls;
    public final Hole[] holes;
    public final Spot[] stars;
    public final PickupSpot[] pickups;

    public Level(String name, int world, float time, float friction, boolean strobe,
                 float startX, float startY, float exitX, float exitY,
                 Wall[] walls, Hole[] holes, Spot[] stars, PickupSpot[] pickups) {
        this.name = name;
        this.world = world;
        this.time = time;
        this.friction = friction;
        this.strobe = strobe;
        this.startX = startX;
        this.startY = startY;
        this.exitX = exitX;
        this.exitY = exitY;
        this.walls = walls;
        this.holes = holes;
        this.stars = stars;
        this.pickups = pickups;
    }
}
