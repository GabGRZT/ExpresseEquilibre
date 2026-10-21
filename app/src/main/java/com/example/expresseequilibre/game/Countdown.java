package com.example.expresseequilibre.game;

import com.example.expresseequilibre.input.Feedback;
import android.media.ToneGenerator;


/** Compte à rebours de 3 s avant le départ (ou la reprise après une pause). */
public final class Countdown {
    private static final float DURATION = 3f;

    private float left;
    private int lastTick;
    private boolean resumed;

    public void start(boolean afterPause) {
        left = DURATION;
        lastTick = 0;
        resumed = afterPause;
    }

    public boolean isActive() { return left > 0f; }

    public float left() { return left; }

    public boolean isResume() { return resumed; }

    /** @return true à l'instant où le décompte se termine. */
    public boolean update(float dt, Feedback feedback) {
        left -= dt;
        int tick = (int) Math.ceil(Math.max(0f, left));
        if (tick != lastTick && tick > 0) {
            lastTick = tick;
            feedback.tone(ToneGenerator.TONE_PROP_BEEP, 80);
        }
        return left <= 0f;
    }
}
