package com.example.expresseequilibre.input;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Accéléromètre : inclinaison (direction de la bille) et secousse (saut / mur cassé).
 * Les axes sont exprimés dans le repère de l'ÉCRAN, donc compatibles paysage et paysage inversé.
 */
public final class TiltSensor implements SensorEventListener {
    // --- Réglages (à ajuster en testant sur un vrai appareil) ---
    private static final float FILTER_ALPHA = 0.15f;
    private static final float DEADZONE = 0.5f;
    private static final float MAX_TILT = 5.0f;
    private static final float SHAKE_THRESHOLD = 9.0f;
    private static final long SHAKE_COOLDOWN_MS = 350;
    private static final long SHAKE_FREEZE_MS = 250;

    private final Context context;
    private final SensorManager manager;
    private final Sensor accelerometer;

    private float filteredX, filteredY;   // inclinaison lissée (repère écran)
    private float offsetX, offsetY;       // position neutre
    private boolean initialized;
    private int lastRotation = -1;
    private boolean flatReference = true; // neutre = horizontale (sinon : position tenue au départ)
    private boolean shakePending;
    private long lastShake;

    public TiltSensor(Context context) {
        this.context = context;
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = manager != null ? manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }

    public boolean isAvailable() { return accelerometer != null; }

    public void start() {
        initialized = false;
        if (accelerometer != null) manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (manager != null) manager.unregisterListener(this);
    }

    /** true : le neutre est l'horizontale réelle de l'appareil. false : la position tenue au départ de la partie. */
    public void setFlatReference(boolean flat) {
        flatReference = flat;
        if (flat) offsetX = offsetY = 0f;
    }

    /** Début de partie : fixe le neutre (sans effet en mode « à plat »). */
    public void calibrate() {
        if (flatReference) { offsetX = offsetY = 0f; }
        else { offsetX = filteredX; offsetY = filteredY; }
    }

    /** Inclinaison -1..1 (positif = vers la droite de l'écran). */
    public float tiltX() { return shape(filteredX - offsetX); }

    /** Inclinaison -1..1 (positif = vers le bas de l'écran). */
    public float tiltY() { return shape(filteredY - offsetY); }

    public boolean consumeShake() {
        boolean r = shakePending;
        shakePending = false;
        return r;
    }

    /** Zone morte puis courbe douce : précis près du neutre, vif quand on penche franchement. */
    private static float shape(float v) {
        float a = Math.abs(v);
        if (a <= DEADZONE) return 0f;
        float n = Math.min(1f, (a - DEADZONE) / (MAX_TILT - DEADZONE));
        n = n * (0.4f + 0.6f * n);
        return v < 0 ? -n : n;
    }

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
        if (!initialized || rotationChanged) {
            filteredX = sx;
            filteredY = sy;
            initialized = true;
            if (rotationChanged && !flatReference) { offsetX = sx; offsetY = sy; } // recalibration automatique
        } else if (now - lastShake > SHAKE_FREEZE_MS) { // pas de lissage pendant une secousse
            filteredX += FILTER_ALPHA * (sx - filteredX);
            filteredY += FILTER_ALPHA * (sy - filteredY);
        }

        float magnitude = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (Math.abs(magnitude - SensorManager.GRAVITY_EARTH) > SHAKE_THRESHOLD && now - lastShake > SHAKE_COOLDOWN_MS) {
            shakePending = true;
            lastShake = now;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
