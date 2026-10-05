package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/** Gros bouton tactile : état pressé, jauge de recharge, cible élargie, annulation si le doigt sort. */
final class UiButton {
    interface Icon { void draw(Canvas c, float cx, float cy, float size, int color); }

    final RectF rect = new RectF();
    String label;
    int color;
    Icon icon;
    boolean horizontal;      // icône à gauche du texte (sinon au-dessus)
    boolean fillVertical;    // jauge de bas en haut (boutons hauts)
    boolean enabled = true;
    boolean pressed;
    float fill = 1f;         // 0..1 : jauge de recharge
    private boolean armed;

    UiButton(String label, int color, Icon icon) {
        this.label = label;
        this.color = color;
        this.icon = icon;
    }

    void set(float l, float t, float r, float b) { rect.set(l, t, r, b); }

    boolean contains(float x, float y) {
        float s = Ui.dp(4);
        return x >= rect.left - s && x <= rect.right + s && y >= rect.top - s && y <= rect.bottom + s;
    }

    void onDown(float x, float y) {
        if (enabled && contains(x, y)) { armed = true; pressed = true; }
    }

    void onMove(float x, float y) {
        if (armed) pressed = contains(x, y);
    }

    boolean onUp(float x, float y) {
        boolean click = armed && enabled && contains(x, y);
        armed = false;
        pressed = false;
        return click;
    }

    void onCancel() { armed = false; pressed = false; }

    void draw(Canvas c) {
        float w = rect.width(), h = rect.height();
        float m = Math.min(w, h);
        float rad = Math.min(m * 0.26f, Ui.dp(28));
        float depth = Ui.dp(5);
        float off = pressed ? depth * 0.8f : 0f;
        int base = enabled ? color : Ui.MUTED;

        Ui.roundRect(c, rect.left, rect.top + depth, rect.right, rect.bottom + depth, rad, Ui.shade(base, 0.7f));
        float l = rect.left, t = rect.top + off, r = rect.right, b = rect.bottom + off;
        if (fill >= 0.999f || !enabled) {
            Ui.roundRect(c, l, t, r, b, rad, base);
        } else {
            Ui.roundRect(c, l, t, r, b, rad, Ui.MUTED);
            c.save();
            if (fillVertical) c.clipRect(l, b - h * fill, r, b);
            else c.clipRect(l, t, l + w * fill, b);
            Ui.roundRect(c, l, t, r, b, rad, base);
            c.restore();
        }

        float cx = (l + r) / 2f, cy = (t + b) / 2f;
        if (icon != null && !horizontal) {
            float iconS = m * 0.38f, labS = m * 0.17f, gap = m * 0.08f;
            float gy = cy - (iconS + gap + labS) / 2f;
            icon.draw(c, cx, gy + iconS / 2f, iconS, Color.WHITE);
            Ui.text(c, label, cx, gy + iconS + gap + labS * 0.9f, labS, Color.WHITE, true, Paint.Align.CENTER, w * 0.9f);
        } else if (icon != null) {
            float size = h * 0.36f, iconS = h * 0.42f, gap = h * 0.12f;
            float textW = Ui.measure(label, size, true);
            float x0 = cx - (iconS + gap + textW) / 2f;
            icon.draw(c, x0 + iconS / 2f, cy, iconS, Color.WHITE);
            Ui.text(c, label, x0 + iconS + gap, cy + size * 0.35f, size, Color.WHITE, true, Paint.Align.LEFT, 0f);
        } else {
            float size = h * 0.38f;
            Ui.text(c, label, cx, cy + size * 0.35f, size, Color.WHITE, true, Paint.Align.CENTER, w * 0.9f);
        }
    }
}