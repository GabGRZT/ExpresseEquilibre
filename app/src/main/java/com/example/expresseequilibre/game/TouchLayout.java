package com.example.expresseequilibre.game;

import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.RectF;


/**
 * Disposition de l'écran de jeu selon le mode de contrôle :
 *  - Inclinaison : Pause + Saut à gauche, Souffle à droite, plateau au centre ;
 *  - Joystick    : les 3 boutons empilés d'un côté (sous le même pouce), la manette de l'autre côté.
 */
public final class TouchLayout {
    public final RectF board = new RectF();    // plateau à l'écran
    public final RectF actions = new RectF();  // colonne de boutons
    public final RectF side = new RectF();     // colonne du bouton Souffle (mode Inclinaison)
    public final RectF zone = new RectF();     // zone de la manette (mode Joystick)
    public float scale = 1f;                   // pixels écran par unité logique

    private boolean hasZone;
    private long key = -1;

    public boolean hasJoystick() { return hasZone; }

    /** Recalcule les zones si la taille, les marges ou les réglages ont changé. */
    public void update(int w, int h, GameView g) {
        int il = g.insetLeft, ir = g.insetRight, it = g.insetTop;
        ControlMode mode = g.progress.controlMode;
        boolean padLeft = g.progress.padLeft;
        long k = (((((long) w * 31 + h) * 31 + il) * 31 + ir) * 31 + it) * 31 + mode.ordinal() * 2 + (padLeft ? 1 : 0);
        if (k == key) return;
        key = k;

        float margin = Ui.dp(10), usable = w - il - ir;
        hasZone = mode == ControlMode.JOYSTICK;
        float freeLeft, freeRight; // espace laissé au plateau

        if (!hasZone) {
            float col = Math.max(Ui.dp(70), usable * 0.12f);
            actions.set(il + margin, 0, il + margin + col, h);
            side.set(w - ir - margin - col, 0, w - ir - margin, h);
            zone.setEmpty();
            freeLeft = actions.right + margin;
            freeRight = side.left - margin;
        } else {
            float col = Math.max(Ui.dp(80), usable * 0.13f), zoneW = Math.max(Ui.dp(150), usable * 0.19f);
            float zoneTop = h * 0.06f, zoneBottom = h * 0.94f;
            if (!padLeft) { // boutons à gauche, manette à droite
                actions.set(il + margin, 0, il + margin + col, h);
                zone.set(w - ir - margin - zoneW, zoneTop, w - ir - margin, zoneBottom);
                freeLeft = actions.right + margin;
                freeRight = zone.left - margin;
            } else {        // inversé
                actions.set(w - ir - margin - col, 0, w - ir - margin, h);
                zone.set(il + margin, zoneTop, il + margin + zoneW, zoneBottom);
                freeLeft = zone.right + margin;
                freeRight = actions.left - margin;
            }
            side.setEmpty();
        }

        float top = Math.max(h * 0.15f, it + h * 0.10f), bottom = h * 0.96f;
        float areaW = freeRight - freeLeft, areaH = bottom - top;
        scale = Math.min(areaW / Level.W, areaH / Level.H); // échelle uniforme : jamais d'écrasement
        float boardW = Level.W * scale, boardH = Level.H * scale;
        float left = freeLeft + (areaW - boardW) / 2f;
        board.set(left, top, left + boardW, top + boardH);
    }

    public void placeButtons(UiButton pause, UiButton action, UiButton blow, float h) {
        if (hasZone) {
            pause.set(actions.left, h * 0.05f, actions.right, h * 0.20f);
            action.set(actions.left, h * 0.25f, actions.right, h * 0.58f);
            blow.set(actions.left, h * 0.62f, actions.right, h * 0.95f);
        } else {
            pause.set(actions.left, h * 0.06f, actions.right, h * 0.28f);
            action.set(actions.left, h * 0.36f, actions.right, h * 0.94f);
            blow.set(side.left, h * 0.36f, side.right, h * 0.94f);
        }
    }
}
