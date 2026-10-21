package com.example.expresseequilibre.input;

import android.content.Context;

/** Regroupe les deux capteurs hors écran tactile : accéléromètre (tilt) et micro (blow). */
public final class SensorController {
    public final TiltSensor tilt;
    public final BlowDetector blow;

    public SensorController(Context context) {
        tilt = new TiltSensor(context);
        blow = new BlowDetector(context);
    }

    public void start() { tilt.start(); }

    public void stop() {
        tilt.stop();
        blow.setActive(false);
    }
}
