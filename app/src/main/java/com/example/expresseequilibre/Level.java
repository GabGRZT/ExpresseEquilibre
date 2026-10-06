package com.example.expresseequilibre;

/**
 * Un niveau, en coordonnées LOGIQUES (plateau fixe de 1600 x 900) :
 * le niveau est identique sur tous les écrans.
 * walls   : {x, y, largeur, hauteur, fragile(0/1)}
 * holes   : {x, y, rayon, xRéapparition, yRéapparition}
 * stars   : {x, y}
 * pickups : {x, y} avec le type correspondant dans ptypes
 */
final class Level {
    static final float W = 1600f, H = 900f, EXIT_R = 80f;

    final String name;
    final int world;
    final float time, friction;
    final boolean strobe;
    final float startX, startY, exitX, exitY;
    final float[][] walls, holes, stars, pickups;
    final Pickup[] ptypes;

    Level(String name, int world, float time, float friction, boolean strobe,
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