package com.example.expresseequilibre;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;

/** Thème visuel d'un monde. */
final class World {
    final String name;
    final boolean dark, lava;
    final int bg, board1, board2, wall, wallTop, rim, hud, accent, grid;
    private LinearGradient shader;

    private World(String name, boolean dark, boolean lava, int bg, int board1, int board2, int wall, int wallTop,
                  int rim, int hud, int accent, int grid) {
        this.name = name;
        this.dark = dark;
        this.lava = lava;
        this.bg = bg;
        this.board1 = board1;
        this.board2 = board2;
        this.wall = wall;
        this.wallTop = wallTop;
        this.rim = rim;
        this.hud = hud;
        this.accent = accent;
        this.grid = grid;
    }

    Shader boardShader() {
        if (shader == null) shader = new LinearGradient(0, 0, 0, Level.H, board1, board2, Shader.TileMode.CLAMP);
        return shader;
    }

    static final World[] ALL = {
            new World("Prairie", false, false, Ui.BG, Color.rgb(250, 252, 255), Color.rgb(232, 244, 236),
                    Ui.WALL, Color.rgb(110, 135, 170), Color.rgb(70, 82, 110), Ui.PRIMARY, Ui.BUTTON,
                    Ui.alpha(Ui.PRIMARY, 18)),
            new World("Glace", false, false, Color.rgb(205, 232, 247), Color.rgb(240, 250, 255), Color.rgb(212, 235, 250),
                    Color.rgb(84, 124, 164), Color.rgb(140, 180, 215), Color.rgb(120, 160, 200),
                    Color.rgb(20, 60, 100), Color.rgb(2, 119, 189), Ui.alpha(Color.rgb(20, 60, 100), 20)),
            new World("Néon", true, false, Color.rgb(14, 14, 34), Color.rgb(26, 26, 56), Color.rgb(18, 18, 42),
                    Color.rgb(0, 172, 193), Color.rgb(77, 208, 225), Color.rgb(213, 0, 249),
                    Color.WHITE, Color.rgb(0, 131, 143), Ui.alpha(Color.rgb(0, 229, 255), 40)),
            new World("Volcan", true, true, Color.rgb(30, 10, 8), Color.rgb(62, 26, 22), Color.rgb(42, 18, 16),
                    Color.rgb(150, 84, 66), Color.rgb(205, 130, 104), Color.rgb(255, 112, 67),
                    Color.WHITE, Color.rgb(230, 81, 0), Ui.alpha(Color.rgb(255, 138, 61), 28))
    };

    static World of(int i) {
        return ALL[Math.max(0, Math.min(ALL.length - 1, i))];
    }
}