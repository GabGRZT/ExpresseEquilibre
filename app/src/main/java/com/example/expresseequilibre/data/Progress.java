package com.example.expresseequilibre.data;

import android.content.SharedPreferences;

import com.example.expresseequilibre.game.Levels;
import com.example.expresseequilibre.input.ControlMode;

/** Sauvegarde : étoiles, niveaux terminés et réglages (contrôle, micro, calibration, effets réduits). */
public final class Progress {
    private final SharedPreferences p;

    public boolean reducedFx;
    public ControlMode controlMode;
    public boolean calibFlat;   // true : le neutre est l'horizontale de l'appareil (défaut)
    public float micSens;       // 0..1 (0,5 = réglage d'origine)

    public Progress(SharedPreferences p) {
        this.p = p;
        reducedFx = p.getBoolean("reducedFx", false);
        controlMode = ControlMode.fromName(p.getString("ctl", "TILT"));
        calibFlat = p.getBoolean("calibFlat", true);
        micSens = Math.max(0f, Math.min(1f, p.getFloat("micSens", 0.5f)));
        purgeOldGhosts();
    }

    /** Les fantômes n'existent plus : on libère les anciennes données enregistrées. */
    private void purgeOldGhosts() {
        SharedPreferences.Editor ed = null;
        for (String k : p.getAll().keySet()) {
            if (k.startsWith("ghost")) {
                if (ed == null) ed = p.edit();
                ed.remove(k);
            }
        }
        if (ed != null) ed.apply();
    }

    // ---------- Réglages ----------

    public void toggleReducedFx() {
        reducedFx = !reducedFx;
        p.edit().putBoolean("reducedFx", reducedFx).apply();
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

    public int totalStars() {
        int t = 0;
        for (int i = 0; i < Levels.ALL.length; i++) t += stars(i);
        return t;
    }

    public int doneCount() {
        int t = 0;
        for (int i = 0; i < Levels.ALL.length; i++) if (done(i)) t++;
        return t;
    }
}
