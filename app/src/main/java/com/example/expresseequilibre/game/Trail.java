package com.example.expresseequilibre.game;

import com.example.expresseequilibre.ui.Ui;
import android.graphics.Canvas;


/** Traînée de la bille : les dernières positions, échantillonnées toutes les 30 ms. */
public final class Trail {
    private static final int SIZE = 10;
    private static final float INTERVAL = 0.03f;

    private final float[] xs = new float[SIZE], ys = new float[SIZE];
    private int head, count;
    private float elapsed;

    public void record(float dt, float x, float y) {
        elapsed += dt;
        if (elapsed < INTERVAL) return;
        elapsed = 0f;
        xs[head] = x;
        ys[head] = y;
        head = (head + 1) % SIZE;
        count = Math.min(SIZE, count + 1);
    }

    public void clear() { count = 0; }

    /** Des disques de plus en plus petits et transparents, du plus récent au plus ancien. */
    public void draw(Canvas c, float ballRadius, int color) {
        for (int k = count - 1; k >= 0; k--) {
            int i = (head - 1 - k + SIZE * 2) % SIZE;
            float f = 1f - k / (float) SIZE;
            c.drawCircle(xs[i], ys[i], ballRadius * (0.35f + 0.45f * f), Ui.fill(Ui.alpha(color, (int) (70 * f))));
        }
    }
}
