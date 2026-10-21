package com.example.expresseequilibre.game;

import com.example.expresseequilibre.ui.Ui;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;


/** Manette virtuelle flottante : le socle apparaît là où le pouce se pose dans la zone. */
public final class Joystick {
    private static final float DEAD_ZONE = 0.12f; // fraction du rayon sans effet

    private final RectF zone = new RectF();
    private float radius, textSize;
    private int pointerId = -1;
    private float baseX, baseY, curX, curY;

    /** À rappeler quand la mise en page change. */
    public void setZone(RectF area, float boardHeight) {
        zone.set(area);
        radius = Math.min(zone.width() * 0.40f, Math.max(Ui.dp(60), boardHeight * 0.20f));
        textSize = Math.max(Ui.sp(14), boardHeight * 0.04f);
    }

    public void reset() { pointerId = -1; }

    /** Un doigt se pose : prend le pilotage s'il est dans la zone et que la manette est libre. */
    public boolean start(int id, float x, float y) {
        if (pointerId >= 0 || !zone.contains(x, y)) return false;
        pointerId = id;
        baseX = curX = Ui.clamp(x, zone.left + radius, zone.right - radius);
        baseY = curY = Ui.clamp(y, zone.top + radius, zone.bottom - radius);
        return true;
    }

    /** @return true si ce doigt est celui de la manette. */
    public boolean move(int id, float x, float y) {
        if (id != pointerId) return false;
        curX = x;
        curY = y;
        return true;
    }

    /** @return true si ce doigt était celui de la manette. */
    public boolean release(int id) {
        if (id != pointerId) return false;
        pointerId = -1;
        return true;
    }

    /** Direction voulue dans out[0] (x) et out[1] (y), -1..1 : zone morte puis courbe douce. */
    public void read(float[] out) {
        out[0] = out[1] = 0f;
        if (pointerId < 0) return;
        float dx = curX - baseX, dy = curY - baseY;
        float d = (float) Math.hypot(dx, dy);
        if (d <= radius * DEAD_ZONE) return;
        float k = Math.min(1f, d / radius);
        k *= 0.4f + 0.6f * k;
        out[0] = dx / d * k;
        out[1] = dy / d * k;
    }

    public void draw(Canvas c, int color, int hintColor, boolean showHint) {
        c.drawRoundRect(zone, Ui.dp(20), Ui.dp(20), Ui.fill(Ui.alpha(color, 16)));
        Ui.outline(c, zone, Ui.dp(20), Ui.alpha(color, 70), Ui.dp(2));

        if (pointerId >= 0) {
            float dx = curX - baseX, dy = curY - baseY;
            float d = (float) Math.hypot(dx, dy);
            if (d > radius) { dx *= radius / d; dy *= radius / d; }
            c.drawCircle(baseX, baseY, radius, Ui.fill(Ui.alpha(color, 35)));
            c.drawCircle(baseX, baseY, radius, Ui.stroke(Ui.alpha(color, 150), Ui.dp(3)));
            c.drawCircle(baseX + dx, baseY + dy, radius * 0.38f, Ui.fill(Ui.alpha(color, 190)));
        } else {
            float cx = zone.centerX(), cy = zone.centerY();
            c.drawCircle(cx, cy, radius, Ui.stroke(Ui.alpha(color, 90), Ui.dp(3)));
            c.drawCircle(cx, cy, radius * 0.38f, Ui.fill(Ui.alpha(color, 70)));
            if (showHint) {
                Ui.text(c, "Pose ton pouce ici", cx, cy + radius * 1.45f, textSize, hintColor, true,
                        Paint.Align.CENTER, zone.width() * 0.9f);
            }
        }
    }
}
