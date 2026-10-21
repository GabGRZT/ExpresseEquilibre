package com.example.expresseequilibre.game;

/** État d'une étoile pendant la partie (elle peut bouger, attirée par l'aimant). */
public final class Star {
    public float x, y;
    public boolean collected;
    public boolean pulling; // attirée par l'aimant à cette image

    public void reset(Spot spot) {
        x = spot.x;
        y = spot.y;
        collected = false;
        pulling = false;
    }
}
