package com.example.expresseequilibre;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Feedbacks multimodaux : vibrations + effets sonores + musique. "Sons : NON" coupe tout. */
@SuppressWarnings("deprecation")
final class Feedback {
    private final Vibrator vibrator;
    private final SharedPreferences prefs;
    private final SoundEngine sound;
    private boolean enabled;

    Feedback(Context ctx, SharedPreferences prefs) {
        this.prefs = prefs;
        this.enabled = prefs.getBoolean("feedback", true);
        this.vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        this.sound = new SoundEngine(ctx);
        this.sound.setMuted(!enabled);
    }

    SoundEngine sound() { return sound; }

    boolean isEnabled() { return enabled; }

    void toggle() {
        enabled = !enabled;
        prefs.edit().putBoolean("feedback", enabled).apply();
        sound.setMuted(!enabled);
    }

    void vibrate(long ms) {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }

    void sfx(Sfx s) { sound.play(s, 1f); }

    void sfx(Sfx s, float rate) { sound.play(s, rate); }

    void release() { sound.release(); }
}