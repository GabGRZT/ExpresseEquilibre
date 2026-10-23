package com.example.expresseequilibre.data;

import com.example.expresseequilibre.input.ControlMode;
import android.content.SharedPreferences;

/** Sauvegarde : étoiles, niveaux terminés et réglages (contrôle, micro, calibration, effets réduits). */
public final class Progress {
    private final SharedPreferences p;

    public boolean reducedFx;
    public ControlMode controlMode;
    public boolean calibFlat;   // true : le neutre est l'horizontale de l'appareil (défaut)
    public float micSens;       // 0..1 (0,5 = réglage d'origine)
    public boolean padLeft;     // false : manette à droite, boutons à gauche (défaut) ; true : inversé

    public Progress(SharedPreferences p) {
        this.p = p;
        reducedFx = p.getBoolean("reducedFx", false);
        controlMode = ControlMode.fromName(p.getString("ctl", "TILT"));
        calibFlat = p.getBoolean("calibFlat", true);
        padLeft = p.getBoolean("padLeft", false);
        micSens = Math.max(0f, Math.min(1f, p.getFloat("micSens", 0.5f)));
    }

    // ---------- Réglages ----------

    public void toggleReducedFx() {
        reducedFx = !reducedFx;
        p.edit().putBoolean("reducedFx", reducedFx).apply();
    }

    public void togglePadLeft() {
        padLeft = !padLeft;
        p.edit().putBoolean("padLeft", padLeft).apply();
    }

    public void setControlMode(ControlMode m) {
        controlMode = m;
        p.edit().putString("ctl", m.name()).apply();
    }

    public void setCalibFlat(boolean flat) {
        calibFlat = flat;
        p.edit().putBoolean("calibFlat", flat).apply();
    }

    public void setMicSens(float v) {
        micSens = Math.max(0f, Math.min(1f, v));
        p.edit().putFloat("micSens", micSens).apply();
    }

    // ---------- Progression ----------

    public int stars(int lv) { return p.getInt("stars_" + lv, 0); }

    public boolean done(int lv) { return p.getBoolean("done_" + lv, false); }

    public boolean unlocked(int lv) { return lv == 0 || done(lv - 1); }

    public void record(int lv, int stars) {
        p.edit().putBoolean("done_" + lv, true).putInt("stars_" + lv, Math.max(stars(lv), stars)).apply();
    }

}
