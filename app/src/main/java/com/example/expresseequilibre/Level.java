package com.example.expresseequilibre;

/**
 * Un niveau, en coordonnées LOGIQUES (plateau fixe 1600 x 900).
 * walls     {x, y, larg, haut, fragile 0/1}
 * holes     {x, y, rayon, xRéapparition, yRéapparition}
 * stars     {x, y}                       pickups {x, y} + types
 * movers    {x, y, larg, haut, dx, dy, période, phase}      (murs mobiles)
 * spinners  {cx, cy, demi-longueur, demi-épaisseur, vitesse rad/s, angle départ}
 * conveyors {x, y, larg, haut, dirX, dirY, accélération}
 * bumpers   {x, y, rayon}                teles {x1, y1, x2, y2}
 * mholes    {cx, cy, rayon, ampX, ampY, période, phase, xRéapparition, yRéapparition}
 * doors     {x, y, larg, haut, période, fraction ouverte, phase}
 */
final class Level {
    static final float W = 1600f, H = 900f, EXIT_R = 80f;

    final String name;
    final int world;
    final float time, friction;
    float strobeHz;          // 0 = pas de stroboscope (plafonné à 2,5 dans PlayScreen)
    int starGate;            // étoiles totales nécessaires
    int req = -2;            // niveau à terminer avant (-2 = le précédent, -1 = aucun)
    float startX = 100f, startY = 450f, exitX = 1500f, exitY = 450f;
    float[][] walls = new float[0][], holes = new float[0][], stars = new float[0][], pickups = new float[0][];
    float[][] movers = new float[0][], spinners = new float[0][], conveyors = new float[0][];
    float[][] bumpers = new float[0][], teles = new float[0][], mholes = new float[0][], doors = new float[0][];
    Pickup[] ptypes = new Pickup[0];

    Level(String name, int world, float time, float friction) {
        this.name = name;
        this.world = world;
        this.time = time;
        this.friction = friction;
    }

    Level start(float x, float y) { startX = x; startY = y; return this; }

    Level exit(float x, float y) { exitX = x; exitY = y; return this; }

    Level walls(float[][] a) { walls = a; return this; }

    Level holes(float[][] a) { holes = a; return this; }

    Level stars(float[][] a) { stars = a; return this; }

    Level pickups(float[][] a, Pickup... t) { pickups = a; ptypes = t; return this; }

    Level movers(float[][] a) { movers = a; return this; }

    Level spinners(float[][] a) { spinners = a; return this; }

    Level conveyors(float[][] a) { conveyors = a; return this; }

    Level bumpers(float[][] a) { bumpers = a; return this; }

    Level teles(float[][] a) { teles = a; return this; }

    Level mholes(float[][] a) { mholes = a; return this; }

    Level doors(float[][] a) { doors = a; return this; }

    Level strobe(float hz) { strobeHz = hz; return this; }

    Level gate(int stars, int req) { starGate = stars; this.req = req; return this; }
}