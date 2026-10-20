package com.example.expresseequilibre.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

/** Curseur tactile horizontal (valeur 0..1) avec une grosse poignée, dessiné au Canvas. */
public final class UiSlider {
    public final RectF rect = new RectF();
    public float value = 0.5f;
    public int color;
    private boolean dragging;

    public UiSlider(int color) { this.color = color; }

    public void set(float l, float t, float r, float b) { rect.set(l, t, r, b); }

    public boolean isDragging() { return dragging; }

    /** Zone tactile élargie : on n'a pas à viser précisément la poignée. */
    public boolean contains(float x, float y) {
        float sx = Ui.dp(18), sy = Ui.dp(16);
        return x >= rect.left - sx && x <= rect.right + sx && y >= rect.top - sy && y <= rect.bottom + sy;
    }

    public void onDown(float x, float y) {
        if (contains(x, y)) { dragging = true; setFromX(x); }
    }

    public void onMove(float x, float y) {
        if (dragging) setFromX(x);
    }

    /** @return true si un glissement vient de se terminer (le réglage doit alors être sauvegardé). */
    public boolean onUp() {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    public void onCancel() { dragging = false; }

    private void setFromX(float x) {
        float pad = rect.height() / 2f;
        float w = rect.width() - 2f * pad;
        if (w <= 0f) return;
        value = Ui.clamp((x - (rect.left + pad)) / w, 0f, 1f);
    }

    public void draw(Canvas c) {
        float h = rect.height(), pad = h / 2f;
        float cy = rect.centerY(), th = h * 0.30f;
        float l = rect.left + pad, r = rect.right - pad;
        Ui.roundRect(c, rect.left, cy - th / 2f, rect.right, cy + th / 2f, th / 2f, Ui.alpha(Ui.PRIMARY, 50));
        float kx = l + (r - l) * value;
        Ui.roundRect(c, rect.left, cy - th / 2f, kx, cy + th / 2f, th / 2f, color);
        float kr = dragging ? h * 0.56f : h * 0.5f;
        c.drawCircle(kx, cy + Ui.dp(2), kr, Ui.fill(Ui.alpha(Color.BLACK, 50)));
        c.drawCircle(kx, cy, kr, Ui.fill(Color.WHITE));
        c.drawCircle(kx, cy, kr, Ui.stroke(color, Ui.dp(3)));
    }
}
