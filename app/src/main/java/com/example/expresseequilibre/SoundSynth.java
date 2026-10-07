package com.example.expresseequilibre;

import java.util.Random;

/** Synthèse 100 % originale : aucun fichier audio, aucune mélodie protégée. */
final class SoundSynth {
    private SoundSynth() {}

    static final int RATE = 22050;
    private static final Random RND = new Random(7);

    private static float freq(int midi) { return 440f * (float) Math.pow(2.0, (midi - 69) / 12.0); }

    private static float wave(int type, float ph, float duty) {
        switch (type) {
            case 0: return (float) Math.sin(ph * 6.2831853f);
            case 1: return ph < duty ? 0.6f : -0.6f;
            case 2: return (ph * 2f - 1f) * 0.6f;
            default: return (ph < 0.5f ? ph * 4f - 1f : 3f - ph * 4f) * 0.8f;
        }
    }

    /** Note avec glissando f0 -> f1 ; decay = 0 : note tenue. type : 0 sinus, 1 carré, 2 dent de scie, 3 triangle. */
    private static void tone(float[] b, float start, float dur, float f0, float f1,
                             int type, float vol, float decay, float duty) {
        int s0 = (int) (start * RATE), n = (int) (dur * RATE);
        float ph = 0f;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            float t = i / (float) RATE;
            float f = f0 + (f1 - f0) * (i / (float) n);
            float env = Math.min(1f, i / (RATE * 0.003f)) * (float) Math.exp(-decay * t);
            float rel = Math.min(1f, (n - i) / (RATE * 0.012f));
            b[s0 + i] += wave(type, ph, duty) * env * rel * vol;
            ph += f / RATE;
            if (ph >= 1f) ph -= 1f;
        }
    }

    private static void tone(float[] b, float start, float dur, float f, int type, float vol, float decay) {
        tone(b, start, dur, f, f, type, vol, decay, 0.5f);
    }

    private static void noise(float[] b, float start, float dur, float vol, float decay, float lp, float attack) {
        int s0 = (int) (start * RATE), n = (int) (dur * RATE);
        float y = 0f;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            float t = i / (float) RATE;
            y += lp * ((RND.nextFloat() * 2f - 1f) - y);
            float env = (attack > 0f ? Math.min(1f, t / attack) : 1f) * (float) Math.exp(-decay * t);
            float rel = Math.min(1f, (n - i) / (RATE * 0.012f));
            b[s0 + i] += y * env * rel * vol;
        }
    }

    private static short[] toShorts(float[] b, float gain) {
        short[] out = new short[b.length];
        for (int i = 0; i < b.length; i++) {
            out[i] = (short) (Math.max(-1f, Math.min(1f, b[i] * gain)) * 32000f);
        }
        return out;
    }

    // ---------- Effets sonores ----------
    static short[] sfx(Sfx s) {
        float[] b;
        switch (s) {
            case JUMP:
                b = new float[(int) (0.2f * RATE)];
                tone(b, 0f, 0.16f, 350f, 800f, 0, 0.55f, 6f, 0.5f);
                break;
            case FAN:
                b = new float[(int) (0.6f * RATE)];
                noise(b, 0f, 0.55f, 0.55f, 3f, 0.22f, 0.12f);
                break;
            case STAR:
                b = new float[(int) (0.4f * RATE)];
                tone(b, 0f, 0.09f, 1046f, 1, 0.3f, 8f);
                tone(b, 0.07f, 0.09f, 1318f, 1, 0.3f, 8f);
                tone(b, 0.14f, 0.22f, 1568f, 1, 0.3f, 6f);
                break;
            case GOOD:
                b = new float[(int) (0.35f * RATE)];
                tone(b, 0f, 0.1f, 659f, 3, 0.55f, 6f);
                tone(b, 0.09f, 0.22f, 880f, 3, 0.55f, 5f);
                break;
            case BAD:
                b = new float[(int) (0.4f * RATE)];
                tone(b, 0f, 0.32f, 300f, 110f, 2, 0.4f, 4f, 0.5f);
                break;
            case FALL:
                b = new float[(int) (0.55f * RATE)];
                tone(b, 0f, 0.45f, 420f, 70f, 0, 0.65f, 3f, 0.5f);
                noise(b, 0.05f, 0.2f, 0.25f, 10f, 0.4f, 0f);
                break;
            case BREAK:
                b = new float[(int) (0.5f * RATE)];
                noise(b, 0f, 0.4f, 0.8f, 9f, 0.35f, 0f);
                tone(b, 0f, 0.25f, 140f, 50f, 0, 0.7f, 12f, 0.5f);
                break;
            case COUNT:
                b = new float[(int) (0.12f * RATE)];
                tone(b, 0f, 0.09f, 880f, 1, 0.35f, 10f);
                break;
            case GO:
                b = new float[(int) (0.4f * RATE)];
                tone(b, 0f, 0.08f, 880f, 1, 0.35f, 6f);
                tone(b, 0.09f, 0.25f, 1320f, 1, 0.4f, 6f);
                break;
            case WIN:
                b = new float[(int) (1.4f * RATE)];
                tone(b, 0f, 0.12f, 523f, 1, 0.33f, 4f);
                tone(b, 0.11f, 0.12f, 659f, 1, 0.33f, 4f);
                tone(b, 0.22f, 0.12f, 784f, 1, 0.33f, 4f);
                tone(b, 0.33f, 0.14f, 1047f, 1, 0.33f, 4f);
                float[] chord = {523f, 659f, 784f, 1047f};
                for (float f : chord) tone(b, 0.46f, 0.85f, f, 3, 0.22f, 3.5f);
                break;
            case LOSE:
                b = new float[(int) (1.1f * RATE)];
                tone(b, 0f, 0.22f, 440f, 2, 0.3f, 3f);
                tone(b, 0.22f, 0.22f, 349f, 2, 0.3f, 3f);
                tone(b, 0.44f, 0.6f, 294f, 2, 0.3f, 3f);
                break;
            case UNLOCK:
                b = new float[(int) (0.7f * RATE)];
                tone(b, 0f, 0.1f, 523f, 3, 0.5f, 5f);
                tone(b, 0.08f, 0.1f, 659f, 3, 0.5f, 5f);
                tone(b, 0.16f, 0.1f, 784f, 3, 0.5f, 5f);
                tone(b, 0.24f, 0.4f, 1047f, 3, 0.5f, 4f);
                break;
            case TICK:
                b = new float[(int) (0.1f * RATE)];
                tone(b, 0f, 0.06f, 220f, 0, 0.6f, 35f);
                break;
            case BUMP:
                b = new float[(int) (0.2f * RATE)];
                tone(b, 0f, 0.15f, 200f, 520f, 0, 0.6f, 14f, 0.5f);
                break;
            default: // TELE
                b = new float[(int) (0.45f * RATE)];
                tone(b, 0f, 0.35f, 300f, 1200f, 0, 0.4f, 4f, 0.5f);
                tone(b, 0.05f, 0.35f, 1200f, 300f, 0, 0.3f, 4f, 0.5f);
                break;
        }
        return toShorts(b, 0.9f);
    }

    // ---------- Musiques originales (chiptune) ----------
    // 0 : menus / Prairie   1 : Glace   2 : Néon   3 : Volcan
    private static final int R = -99; // silence
    private static final int[] BPM = {128, 84, 118, 100};
    private static final int[][] ROOTS = {{48, 45, 41, 43}, {38, 34, 41, 36}, {40, 36, 43, 38}, {38, 38, 34, 33}};
    private static final int[][] LEAD = {
            {76, 0, 79, 76, 74, 0, 72, 0, 72, 0, 76, 81, 79, 0, 76, 0, 77, 0, 81, 77, 76, 0, 74, 0, 74, 76, 79, 76, 74, 72, 71, 0},
            {86, 0, 84, 0, 81, 0, 77, 0, 82, 0, 81, 0, 77, 0, 74, 0, 77, 0, 81, 0, 84, 0, 81, 0, 79, 0, 84, 0, 82, 81, 79, 0},
            {76, 0, 76, 79, 0, 83, 0, 79, 72, 0, 76, 79, 0, 76, 0, 72, 74, 0, 79, 83, 0, 79, 0, 74, 74, 0, 78, 81, 0, 78, 0, 74},
            {74, 0, 74, 77, 0, 74, 0, 72, 74, 77, 81, 0, 79, 77, 0, 74, 70, 0, 74, 77, 0, 74, 0, 70, 73, 0, 76, 79, 0, 81, 0, 73}};
    private static final int[][] ALT = {
            {79, 77, 76, 74, 72, 0, 72, 0},
            {79, 0, 77, 0, 74, 0, 74, 0},
            {83, 0, 81, 0, 79, 0, 76, 0},
            {81, 79, 77, 76, 74, 0, 74, 0}};
    private static final int[][] BASS = {
            {0, R, 7, R, 0, R, 7, R},
            {0, R, R, R, 7, R, R, R},
            {0, 12, 0, 12, 0, 12, 0, 12},
            {0, R, R, 0, R, R, 7, R}};
    private static final int[] BASS_WAVE = {3, 0, 2, 2};
    private static final int[] KICK = {0b00010001, 0, 0b01010101, 0b01001001};
    private static final int[] HAT = {0b01000100, 0, 0b10101010, 0};

    static short[] music(int theme) {
        float step = 60f / BPM[theme] / 2f;           // une croche
        int steps = 64;
        float[] b = new float[(int) (steps * step * RATE)];
        for (int st = 0; st < steps; st++) {
            int pos = st % 32, bar = pos / 8, in = pos % 8;
            float t0 = st * step;
            int note = (st >= 56) ? ALT[theme][st - 56] : LEAD[theme][pos];
            if (note > 0) {
                float f = freq(note);
                switch (theme) {
                    case 0: tone(b, t0, step * 0.9f, f, f, 1, 0.20f, 0f, 0.25f); break;
                    case 1: tone(b, t0, step * 3.5f, f, 0, 0.30f, 4.5f); break;
                    case 2: tone(b, t0, step * 1.4f, f, f, 1, 0.20f, 5f, 0.5f); break;
                    default: tone(b, t0, step * 1.8f, f, f, 2, 0.18f, 1.2f, 0.5f); break;
                }
            }
            int off = BASS[theme][in];
            if (off != R) {
                float f = freq(ROOTS[theme][bar] + off);
                float len = theme == 1 ? step * 3.5f : step * 0.95f;
                tone(b, t0, len, f, f, BASS_WAVE[theme], 0.26f, theme == 1 ? 1.5f : 0f, 0.5f);
            }
            if (((KICK[theme] >> in) & 1) == 1) tone(b, t0, 0.14f, 150f, 45f, 0, 0.55f, 18f, 0.5f);
            if (((HAT[theme] >> in) & 1) == 1) noise(b, t0, 0.05f, 0.18f, 60f, 0.9f, 0f);
        }
        int fi = Math.min(100, b.length), fo = Math.min(400, b.length);
        for (int i = 0; i < fi; i++) b[i] *= i / (float) fi;
        for (int i = 0; i < fo; i++) b[b.length - 1 - i] *= i / (float) fo;
        return toShorts(b, 0.7f);
    }
}