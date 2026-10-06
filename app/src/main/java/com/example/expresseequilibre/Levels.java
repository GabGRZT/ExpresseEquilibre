package com.example.expresseequilibre;

/** Catalogue de la campagne : 3 mondes, 6 niveaux. */
final class Levels {
    private Levels() {}

    static final Level[] ALL = {

            // ---- Monde 1 : Prairie ----
            new Level("Premiers pas", 0, 60f, 1f, false, 112, 180, 1440, 666,
                    new float[][]{{480, 0, 40, 612, 0}, {992, 288, 40, 612, 0}},
                    new float[][]{{500, 756, 135, 286, 756}, {1012, 144, 135, 798, 144}, {752, 450, 81, 752, 610}},
                    new float[][]{{160, 558}, {720, 270}, {1248, 252}},
                    new float[][]{{900, 700}}, new Pickup[]{Pickup.CLOCK}),

            new Level("Casse-murs", 0, 60f, 1f, false, 110, 450, 1450, 770,
                    new float[][]{{500, 0, 40, 600, 0}, {500, 600, 40, 300, 1},
                            {1040, 300, 40, 600, 0}, {1080, 560, 520, 40, 1}},
                    new float[][]{{790, 450, 110, 601, 450}},
                    new float[][]{{200, 750}, {800, 150}, {1220, 790}},
                    new float[][]{{1000, 150}, {1300, 300}}, new Pickup[]{Pickup.HAMMER, Pickup.CLOCK}),

            new Level("Bulles", 0, 60f, 1f, false, 100, 450, 1480, 470,
                    new float[][]{{400, 0, 40, 400, 0}, {900, 500, 40, 400, 0}},
                    new float[][]{{700, 250, 100, 700, 429}, {650, 700, 120, 650, 501},
                            {1200, 300, 110, 1200, 489}, {1250, 700, 90, 1250, 531}},
                    new float[][]{{250, 700}, {900, 200}, {1100, 500}},
                    new float[][]{{560, 450}, {1000, 300}, {300, 300}, {800, 450}, {1150, 850}, {1450, 250}},
                    new Pickup[]{Pickup.CLOCK, Pickup.SPRING, Pickup.INVERT, Pickup.MYSTERY, Pickup.MAGNET, Pickup.ICE}),

            // ---- Monde 2 : Glace (friction réduite) ----
            new Level("Patinoire", 1, 70f, 0.3f, false, 100, 120, 1480, 780,
                    new float[][]{{0, 300, 1100, 40, 0}, {500, 580, 1100, 40, 0}},
                    new float[][]{{800, 460, 90, 631, 460}, {1000, 760, 60, 861, 760}},
                    new float[][]{{400, 150}, {1400, 450}, {250, 760}},
                    new float[][]{{1300, 500}, {300, 450}}, new Pickup[]{Pickup.SPRING, Pickup.CLOCK}),

            new Level("Glissade", 1, 75f, 0.35f, false, 100, 450, 1450, 200,
                    new float[][]{{400, 0, 40, 650, 0}, {800, 0, 40, 560, 0}, {800, 560, 40, 340, 1},
                            {1200, 300, 40, 600, 0}},
                    new float[][]{{1220, 150, 110, 1031, 150}},
                    new float[][]{{200, 800}, {620, 200}, {1400, 600}},
                    new float[][]{{600, 750}, {1000, 450}, {1050, 300}},
                    new Pickup[]{Pickup.HAMMER, Pickup.MYSTERY, Pickup.SPRING}),

            // ---- Monde 3 : Néon (stroboscopique) ----
            new Level("Stroboscope", 2, 75f, 1f, true, 100, 450, 1500, 450,
                    new float[][]{{400, 0, 40, 600, 0}, {800, 300, 40, 600, 0}, {1200, 0, 40, 600, 0}},
                    new float[][]{{560, 160, 80, 560, 319}, {1020, 720, 90, 1020, 551}, {1450, 780, 90, 1450, 611}},
                    new float[][]{{250, 250}, {620, 650}, {1020, 150}},
                    new float[][]{{250, 700}, {1000, 450}, {1330, 300}},
                    new Pickup[]{Pickup.CLOCK, Pickup.CLOCK, Pickup.MYSTERY}),
    };
}