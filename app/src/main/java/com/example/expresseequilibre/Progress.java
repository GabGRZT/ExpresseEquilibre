package com.example.expresseequilibre;

import android.content.SharedPreferences;

/** Sauvegarde : étoiles, niveaux terminés, fantômes (meilleur passage) et réglage "effets réduits". */
final class Progress {
    static final float STEP = 0.05f; // intervalle d'enregistrement du fantôme (s)

    /** Parcours enregistré, en coordonnées logiques. */
    static final class Ghost {
        final float time;
        final short[] x, y;
        final int n;

        Ghost(float time, short[] x, short[] y, int n) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.n = n;
        }

        void pos(float t, float[] out) {
            if (n == 0) { out[0] = 0f; out[1] = 0f; return; }
            float f = Math.max(0f, t / STEP);
            int i = (int) f;
            if (i >= n - 1) { out[0] = x[n - 1]; out[1] = y[n - 1]; return; }
            float k = f - i;
            out[0] = x[i] + (x[i + 1] - x[i]) * k;
            out[1] = y[i] + (y[i + 1] - y[i]) * k;
        }
    }

    private final SharedPreferences p;
    boolean reducedFx;

    Progress(SharedPreferences p) {
        this.p = p;
        reducedFx = p.getBoolean("reducedFx", false);
    }

    void toggleReducedFx() {
        reducedFx = !reducedFx;
        p.edit().putBoolean("reducedFx", reducedFx).apply();
    }

    int stars(int lv) { return p.getInt("stars_" + lv, 0); }

    boolean done(int lv) { return p.getBoolean("done_" + lv, false); }

    boolean unlocked(int lv) { return lv == 0 || done(lv - 1); }

    void record(int lv, int stars) {
        p.edit().putBoolean("done_" + lv, true).putInt("stars_" + lv, Math.max(stars(lv), stars)).apply();
    }

    int totalStars() {
        int t = 0;
        for (int i = 0; i < Levels.ALL.length; i++) t += stars(i);
        return t;
    }

    int doneCount() {
        int t = 0;
        for (int i = 0; i < Levels.ALL.length; i++) if (done(i)) t++;
        return t;
    }

    // ---------- Fantômes ----------

    boolean hasGhost(int lv) { return p.contains("ghostT_" + lv); }

    float ghostTime(int lv) { return p.getFloat("ghostT_" + lv, 0f); }

    int ghostCount() {
        int t = 0;
        for (int i = 0; i < Levels.ALL.length; i++) if (hasGhost(i)) t++;
        return t;
    }

    Ghost loadGhost(int lv) {
        if (!hasGhost(lv)) return null;
        String s = p.getString("ghost_" + lv, "");
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(";");
            short[] xs = new short[parts.length], ys = new short[parts.length];
            int n = 0;
            for (String part : parts) {
                if (part.isEmpty()) continue;
                int c = part.indexOf(',');
                xs[n] = Short.parseShort(part.substring(0, c));
                ys[n] = Short.parseShort(part.substring(c + 1));
                n++;
            }
            return new Ghost(ghostTime(lv), xs, ys, n);
        } catch (Exception e) {
            return null;
        }
    }

    /** @return true si ce passage devient le nouveau fantôme (premier passage ou record). */
    boolean saveGhostIfBetter(int lv, float time, short[] xs, short[] ys, int n) {
        if (n <= 0) return false;
        if (hasGhost(lv) && time >= ghostTime(lv)) return false;
        StringBuilder sb = new StringBuilder(n * 9);
        for (int i = 0; i < n; i++) sb.append(xs[i]).append(',').append(ys[i]).append(';');
        p.edit().putFloat("ghostT_" + lv, time).putString("ghost_" + lv, sb.toString()).apply();
        return true;
    }
}