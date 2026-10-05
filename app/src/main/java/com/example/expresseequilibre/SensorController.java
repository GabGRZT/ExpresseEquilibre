package com.example.expresseequilibre;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.view.Surface;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

/**
 * Les deux capteurs hors écran tactile :
 *  1) accéléromètre -> inclinaison (direction de la bille) + secousse (saut)
 *  2) microphone    -> souffle (ventilateur)
 * Les axes sont exprimés dans le repère de l'ÉCRAN (compatible paysage / paysage inversé).
 */
final class SensorController implements SensorEventListener {

    // --- Réglages (à ajuster en testant sur un vrai téléphone) ---
    private static final float FILTER_ALPHA = 0.15f;
    private static final float TILT_DEADZONE = 0.5f;
    private static final float TILT_MAX = 5.0f;
    private static final float SHAKE_THRESHOLD = 9.0f;
    private static final long SHAKE_COOLDOWN_MS = 350;
    private static final long SHAKE_FREEZE_MS = 250;
    private static final int MIC_RATE = 16000;
    private static final float BLOW_MIN_THRESHOLD = 0.10f;
    private static final float BLOW_DURATION = 0.12f;

    private final Context context;
    private final SensorManager sm;
    private final Sensor accel;

    private float fx, fy;
    private float offX, offY;
    private boolean init;
    private int lastRotation = -1;
    private boolean shakePending;
    private long lastShake;

    private volatile float micLevel;
    private volatile boolean micRunning;
    private volatile int micSession;
    private float baseline = 0.02f;
    private float blowTime;
    private boolean blowPending;

    SensorController(Context context) {
        this.context = context;
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accel = sm != null ? sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    boolean hasAccelerometer() { return accel != null; }

    boolean micAvailable() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ---------- Cycle de vie ----------

    void start() {
        init = false;
        if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
    }

    void stop() {
        if (sm != null) sm.unregisterListener(this);
        setMicActive(false);
    }

    // ---------- Accéléromètre ----------

    @SuppressWarnings("deprecation")
    private int rotation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm != null ? wm.getDefaultDisplay().getRotation() : Surface.ROTATION_90;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float rx = e.values[0], ry = e.values[1], rz = e.values[2];
        long now = SystemClock.uptimeMillis();

        // Repère écran : sx > 0 = la bille doit aller vers la droite, sy > 0 = vers le bas.
        int rot = rotation();
        float sx, sy;
        switch (rot) {
            case Surface.ROTATION_90:  sx = ry;  sy = rx;  break;
            case Surface.ROTATION_270: sx = -ry; sy = -rx; break;
            case Surface.ROTATION_180: sx = rx;  sy = -ry; break;
            default:                   sx = -rx; sy = ry;  break;
        }

        boolean rotationChanged = rot != lastRotation;
        lastRotation = rot;
        if (!init || rotationChanged) {
            fx = sx;
            fy = sy;
            init = true;
            if (rotationChanged) { offX = sx; offY = sy; } // recalibration automatique
        } else if (now - lastShake > SHAKE_FREEZE_MS) {
            fx += FILTER_ALPHA * (sx - fx);
            fy += FILTER_ALPHA * (sy - fy);
        }

        float magnitude = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        float deviation = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);
        if (deviation > SHAKE_THRESHOLD && now - lastShake > SHAKE_COOLDOWN_MS) {
            shakePending = true;
            lastShake = now;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /** La position actuelle du téléphone devient la position neutre. */
    void calibrate() { offX = fx; offY = fy; }

    /** Inclinaison -1..1 (positif = vers la droite de l'écran). */
    float tiltX() { return shape(fx - offX); }

    /** Inclinaison -1..1 (positif = vers le bas de l'écran). */
    float tiltY() { return shape(fy - offY); }

    private float shape(float v) {
        float a = Math.abs(v);
        if (a <= TILT_DEADZONE) return 0f;
        float n = Math.min(1f, (a - TILT_DEADZONE) / (TILT_MAX - TILT_DEADZONE));
        n = n * (0.4f + 0.6f * n);
        return v < 0 ? -n : n;
    }

    boolean consumeShake() {
        boolean r = shakePending;
        shakePending = false;
        return r;
    }

    // ---------- Microphone ----------

    @SuppressLint("MissingPermission")
    void setMicActive(boolean on) {
        if (!on) {
            micRunning = false;
            micSession++;
            return;
        }
        if (micRunning || !micAvailable()) return;

        int min = AudioRecord.getMinBufferSize(MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) return;
        final AudioRecord rec;
        try {
            rec = new AudioRecord(MediaRecorder.AudioSource.MIC, MIC_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
        } catch (Exception ex) {
            return;
        }
        if (rec.getState() != AudioRecord.STATE_INITIALIZED) { rec.release(); return; }

        final int session = ++micSession;
        micRunning = true;
        new Thread(() -> {
            short[] buf = new short[512];
            try {
                rec.startRecording();
                while (micRunning && session == micSession) {
                    int n = rec.read(buf, 0, buf.length);
                    if (n <= 0) continue;
                    double sum = 0;
                    for (int i = 0; i < n; i++) sum += (double) buf[i] * buf[i];
                    micLevel = (float) Math.sqrt(sum / n) / 32768f;
                }
            } catch (Exception ignored) {
            } finally {
                try { rec.stop(); } catch (Exception ignored) { }
                rec.release();
                micLevel = 0f;
                if (session == micSession) micRunning = false;
            }
        }, "mic-level").start();
    }

    float micLevel() { return micLevel; }

    float micThreshold() {
        return Math.min(0.5f, Math.max(BLOW_MIN_THRESHOLD, baseline * 3f + 0.06f));
    }

    void update(float dt) {
        float level = micLevel;
        float thr = micThreshold();
        if (level > thr) {
            blowTime += dt;
        } else {
            blowTime = Math.max(0f, blowTime - dt * 3f);
            baseline += (level - baseline) * Math.min(1f, dt * 2f);
            baseline = Math.min(baseline, 0.15f);
        }
        if (blowTime >= BLOW_DURATION) {
            blowPending = true;
            blowTime = 0f;
        }
    }

    boolean consumeBlow() {
        boolean r = blowPending;
        blowPending = false;
        return r;
    }
}