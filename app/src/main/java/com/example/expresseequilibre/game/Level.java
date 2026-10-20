package com.example.expresseequilibre.game;

/**
 * Un niveau, en coordonnées LOGIQUES (plateau fixe de 1600 x 900) :
 * le niveau est identique sur tous les écrans.
 * walls   : {x, y, largeur, hauteur, fragile(0/1)}
 * holes   : {x, y, rayon, xRéapparition, yRéapparition}
 * stars   : {x, y}
 * pickups : {x, y} avec le type correspondant dans ptypes
 */
public final class Level {
    public static final float W = 1600f, H = 900f, EXIT_R = 80f;

    public final String name;
    public final int world;
    public final float time, friction;
    public final boolean strobe;
    public final float startX, startY, exitX, exitY;
    public final float[][] walls, holes, stars, pickups;
    public final Pickup[] ptypes;

    public Level(String name, int world, float time, float friction, boolean strobe,
          float startX, float startY, float exitX, float exitY,
          float[][] walls, float[][] holes, float[][] stars, float[][] pickups, Pickup[] ptypes) {
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
        this.ptypes = ptypes;
    }
}