package com.example.expresseequilibre.input;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.content.ContextCompat;

/** Microphone : détecte un souffle (niveau sonore au-dessus d'un seuil adaptatif pendant une courte durée). */
public final class BlowDetector {
    private static final int SAMPLE_RATE = 16000;
    private static final float MIN_THRESHOLD = 0.10f;
    private static final float BLOW_DURATION = 0.12f;

    private final Context context;

    private volatile float level;
    private volatile boolean running;
    private volatile int session;          // invalide les anciens threads d'écoute
    private volatile float sensitivity = 0.5f; // 0 = peu sensible, 1 = très sensible
    private float baseline = 0.02f;        // bruit ambiant estimé
    private float blowTime;
    private boolean blowPending;

    public BlowDetector(Context context) { this.context = context; }

    public boolean isAvailable() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Sensibilité 0..1 (0,5 = réglage d'origine). */
    public void setSensitivity(float v) { sensitivity = Math.max(0f, Math.min(1f, v)); }

    public float level() { return level; }

    /** Niveau à dépasser : suit le bruit ambiant, puis est multiplié par 2 (0) à 0,5 (1) selon la sensibilité. */
    public float threshold() {
        float base = Math.min(0.5f, Math.max(MIN_THRESHOLD, baseline * 3f + 0.06f));
        float factor = (float) Math.pow(2.0, 1.0 - 2.0 * sensitivity);
        return Math.max(0.03f, Math.min(0.8f, base * factor));
    }

    /** À appeler à chaque image : transforme le niveau sonore en évènement « souffle ». */
    public void update(float dt) {
        float current = level;
        float thr = threshold();
        if (current > thr) {
            blowTime += dt;
        } else {
            blowTime = Math.max(0f, blowTime - dt * 3f);
            baseline += (current - baseline) * Math.min(1f, dt * 2f);
            baseline = Math.min(baseline, 0.15f);
        }
        if (blowTime >= BLOW_DURATION) {
            blowPending = true;
            blowTime = 0f;
        }
    }

    public boolean consume() {
        boolean r = blowPending;
        blowPending = false;
        return r;
    }

    @SuppressLint("MissingPermission")
    public void setActive(boolean on) {
        if (!on) {
            running = false;
            session++;
            return;
        }
        if (running || !isAvailable()) return;

        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) return;
        final AudioRecord rec;
        try {
            rec = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
        } catch (Exception ex) {
            return;
        }
        if (rec.getState() != AudioRecord.STATE_INITIALIZED) { rec.release(); return; }

        final int mySession = ++session;
        running = true;
        new Thread(() -> listen(rec, mySession), "mic-level").start();
    }

    /** Thread d'écoute : met à jour le niveau RMS (0..1) tant que la session est valide. */
    private void listen(AudioRecord rec, int mySession) {
        short[] buf = new short[512];
        try {
            rec.startRecording();
            while (running && mySession == session) {
                int n = rec.read(buf, 0, buf.length);
                if (n <= 0) continue;
                double sum = 0;
                for (int i = 0; i < n; i++) sum += (double) buf[i] * buf[i];
                level = (float) Math.sqrt(sum / n) / 32768f;
            }
        } catch (Exception ignored) {
        } finally {
            try { rec.stop(); } catch (Exception ignored) { }
            rec.release();
            level = 0f;
            if (mySession == session) running = false;
        }
    }
}
