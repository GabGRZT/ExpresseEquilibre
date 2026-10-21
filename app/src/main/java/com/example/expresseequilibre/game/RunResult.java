package com.example.expresseequilibre.game;

/** Résultat d'une partie, affiché par l'écran de fin. */
public final class RunResult {
    public final boolean win, newRecord;
    public final int stars, score, secondsLeft;
    public final float time;

    public RunResult(boolean win, boolean newRecord, int stars, int score, int secondsLeft, float time) {
        this.win = win;
        this.newRecord = newRecord;
        this.stars = stars;
        this.score = score;
        this.secondsLeft = secondsLeft;
        this.time = time;
    }
}
