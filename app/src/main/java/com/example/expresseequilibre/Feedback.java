package com.example.expresseequilibre;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Feedbacks multimodaux : vibration + bips courts (aucun fichier son). Activables/désactivables. */
@SuppressWarnings("deprecation")
final class Feedback {
    private final Vibrator vibrator;
    private final SharedPreferences prefs;
    private ToneGenerator tone;
    private boolean enabled;

    Feedback(Context ctx, SharedPreferences prefs) {
        this.prefs = prefs;
        this.enabled = prefs.getBoolean("feedback", true);
        this.vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (RuntimeException e) {
            tone = null;
        }
    }

    boolean isEnabled() { return enabled; }

    void toggle() {
        enabled = !enabled;
        prefs.edit().putBoolean("feedback", enabled).apply();
    }

    void vibrate(long ms) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }

    void tone(int type, int ms) {
        if (!enabled || tone == null) return;
        tone.startTone(type, ms);
    }

    void release() {
        if (tone != null) { tone.release(); tone = null; }
    }
}