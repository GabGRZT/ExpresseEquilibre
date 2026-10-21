package com.example.expresseequilibre.screens;

import com.example.expresseequilibre.game.GameView;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;


/**
 * Un écran = une classe : update (état), draw (rendu), évènements tactiles.
 * Les boutons déclarés avec add(...) reçoivent automatiquement les touches, et leur action se déclenche au relâchement.
 */
public abstract class Screen {
    protected final GameView g;
    private final List<UiButton> buttons = new ArrayList<>();

    public Screen(GameView g) { this.g = g; }

    /** Déclare un bouton géré par l'écran ; onClick peut être null (bouton piloté à la main). */
    protected UiButton add(String label, int color, UiButton.Icon icon, Runnable onClick) {
        return add(new UiButton(label, color, icon).onClick(onClick));
    }

    protected UiButton add(UiButton button) {
        buttons.add(button);
        return button;
    }

    public void onEnter() { onCancel(); }

    public void update(float dt) { }

    public abstract void draw(Canvas c, int w, int h);

    // ---------- Tactile : un seul doigt (menus) ----------

    public void onDown(float x, float y) {
        for (UiButton b : buttons) b.onDown(x, y);
    }

    public void onMove(float x, float y) {
        for (UiButton b : buttons) b.onMove(x, y);
    }

    public void onUp(float x, float y) {
        UiButton clicked = null;
        for (UiButton b : buttons) if (b.onUp(x, y) && clicked == null) clicked = b;
        if (clicked != null) clicked.click(); // après la boucle : le clic peut changer d'écran
    }

    public void onCancel() {
        for (UiButton b : buttons) b.onCancel();
    }

    // ---------- Tactile : multi-touch (avec identifiant du doigt) ----------
    // Par défaut, renvoient vers la version à un doigt ; l'écran de jeu les redéfinit.

    public void onDown(int id, float x, float y) { onDown(x, y); }

    public void onMove(int id, float x, float y) { onMove(x, y); }

    public void onUp(int id, float x, float y) { onUp(x, y); }
}
