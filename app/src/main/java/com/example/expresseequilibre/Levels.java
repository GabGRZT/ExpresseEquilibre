package com.example.expresseequilibre;

/** Campagne : 12 niveaux, 4 mondes (Prairie, Glace, Néon stroboscopique, Volcan). */
final class Levels {
    private Levels() {}

    static final Level[] ALL = {

            // ================= Monde 1 : Prairie =================
            new Level("Premiers pas", 0, 60f, 1f)
                    .start(112, 180).exit(1440, 666)
                    .walls(new float[][]{{480, 0, 40, 612, 0}, {992, 288, 40, 612, 0}})
                    .holes(new float[][]{{500, 756, 135, 286, 756}, {1012, 144, 135, 798, 144}, {752, 450, 81, 752, 610}})
                    .stars(new float[][]{{160, 558}, {720, 270}, {1248, 252}})
                    .pickups(new float[][]{{900, 700}}, Pickup.CLOCK),

            new Level("Casse-murs", 0, 60f, 1f)
                    .start(110, 450).exit(1450, 770)
                    .walls(new float[][]{{500, 0, 40, 600, 0}, {500, 600, 40, 300, 1},
                            {1040, 300, 40, 600, 0}, {1080, 560, 520, 40, 1}})
                    .holes(new float[][]{{790, 450, 110, 601, 450}})
                    .stars(new float[][]{{200, 750}, {800, 150}, {1220, 790}})
                    .pickups(new float[][]{{1000, 150}, {1300, 300}}, Pickup.HAMMER, Pickup.CLOCK),

            new Level("Bulles", 0, 60f, 1f)
                    .start(100, 450).exit(1480, 470)
                    .walls(new float[][]{{400, 0, 40, 400, 0}, {900, 500, 40, 400, 0}})
                    .holes(new float[][]{{700, 250, 100, 700, 429}, {650, 700, 120, 650, 501},
                            {1200, 300, 110, 1200, 489}, {1250, 700, 90, 1250, 531}})
                    .stars(new float[][]{{250, 700}, {900, 200}, {1100, 500}})
                    .pickups(new float[][]{{560, 450}, {1000, 300}, {300, 300}, {800, 450}, {1150, 850}, {1450, 250}},
                    Pickup.CLOCK, Pickup.SPRING, Pickup.INVERT, Pickup.MYSTERY, Pickup.MAGNET, Pickup.ICE),

            // ================= Monde 2 : Glace =================
            new Level("Patinoire", 1, 70f, 0.3f)
                    .start(100, 120).exit(1480, 780)
                    .walls(new float[][]{{0, 300, 1100, 40, 0}, {500, 580, 1100, 40, 0}})
                    .holes(new float[][]{{800, 460, 90, 631, 460}, {1000, 760, 60, 861, 760}})
                    .stars(new float[][]{{400, 150}, {1400, 450}, {250, 760}})
                    .pickups(new float[][]{{1300, 500}, {300, 450}}, Pickup.SPRING, Pickup.CLOCK),

            new Level("Glissade", 1, 75f, 0.35f)
                    .start(100, 450).exit(1450, 200)
                    .walls(new float[][]{{400, 0, 40, 650, 0}, {800, 0, 40, 560, 0}, {800, 560, 40, 340, 1},
                            {1200, 300, 40, 600, 0}})
                    .holes(new float[][]{{1220, 150, 110, 1031, 150}})
                    .stars(new float[][]{{200, 800}, {620, 200}, {1400, 600}})
                    .pickups(new float[][]{{600, 750}, {1000, 450}, {1050, 300}},
                    Pickup.HAMMER, Pickup.MYSTERY, Pickup.SPRING),

            // ================= Monde 3 : Néon (stroboscopique, facultatif) =================
            new Level("Stroboscope", 2, 75f, 1f)
                    .start(100, 450).exit(1500, 450).strobe(1.0f)
                    .walls(new float[][]{{400, 0, 40, 600, 0}, {800, 300, 40, 600, 0}, {1200, 0, 40, 600, 0}})
                    .holes(new float[][]{{560, 160, 80, 560, 319}, {1020, 720, 90, 1020, 551}, {1450, 780, 90, 1450, 611}})
                    .stars(new float[][]{{250, 250}, {620, 650}, {1020, 150}})
                    .pickups(new float[][]{{250, 700}, {1000, 450}, {1330, 300}},
                    Pickup.CLOCK, Pickup.CLOCK, Pickup.MYSTERY),

            new Level("Pulsation", 2, 80f, 1f)
                    .start(100, 150).exit(1500, 750).strobe(1.8f)
                    .walls(new float[][]{{300, 200, 40, 700, 0}, {700, 0, 40, 700, 0}, {1100, 200, 40, 700, 0}})
                    .holes(new float[][]{{320, 100, 85, 150, 100}, {720, 800, 85, 560, 800}, {1120, 100, 85, 950, 100}})
                    .movers(new float[][]{{760, 430, 150, 36, 190, 0, 2.4f, 0}})
                    .stars(new float[][]{{200, 600}, {550, 450}, {950, 600}})
                    .pickups(new float[][]{{250, 300}, {930, 780}}, Pickup.CLOCK, Pickup.MYSTERY),

            new Level("Rave", 2, 90f, 1f)
                    .start(100, 450).exit(1500, 450).strobe(2.5f)
                    .walls(new float[][]{{350, 0, 40, 380, 0}, {350, 520, 40, 380, 0},
                            {750, 0, 40, 300, 0}, {750, 420, 40, 480, 0},
                            {1150, 0, 40, 500, 0}, {1150, 640, 40, 260, 0}})
                    .holes(new float[][]{{570, 450, 100, 430, 450}, {970, 430, 70, 845, 430}, {1350, 450, 80, 1232, 450}})
                    .stars(new float[][]{{570, 150}, {960, 780}, {1350, 200}})
                    .pickups(new float[][]{{570, 780}, {960, 150}, {1350, 780}},
                    Pickup.CLOCK, Pickup.SPRING, Pickup.MYSTERY),

            // ================= Monde 4 : Volcan (difficile, 12 étoiles requises) =================
            new Level("Brasier", 3, 80f, 1f)
                    .start(100, 150).exit(1500, 750).gate(12, 4)
                    .walls(new float[][]{{400, 0, 40, 560, 0}, {800, 340, 40, 560, 0}, {1200, 0, 40, 560, 0}})
                    .holes(new float[][]{{420, 730, 120, 250, 730}, {820, 170, 110, 650, 170}, {1220, 730, 110, 1050, 730}})
                    .movers(new float[][]{{480, 430, 200, 40, 120, 0, 2.2f, 0}})
                    .stars(new float[][]{{250, 350}, {620, 250}, {1000, 700}})
                    .pickups(new float[][]{{250, 600}, {1000, 450}, {1400, 400}},
                    Pickup.CLOCK, Pickup.SPRING, Pickup.CLOCK),

            new Level("Pistons", 3, 85f, 1f)
                    .start(100, 450).exit(1500, 450)
                    .walls(new float[][]{{450, 0, 40, 300, 0}, {450, 600, 40, 300, 0},
                            {1000, 0, 40, 300, 0}, {1000, 600, 40, 300, 0}})
                    .holes(new float[][]{{730, 140, 100, 590, 140}, {730, 760, 100, 590, 760}})
                    .spinners(new float[][]{{470, 450, 90, 18, 1.2f, 0}, {1020, 450, 90, 18, -1.2f, 0},
                            {730, 450, 150, 18, 0.9f, 0}})
                    .stars(new float[][]{{250, 700}, {900, 300}, {1300, 200}})
                    .pickups(new float[][]{{250, 200}, {1300, 700}, {900, 600}},
                    Pickup.CLOCK, Pickup.SPRING, Pickup.MYSTERY),

            new Level("Téléporteurs", 3, 90f, 1f)
                    .start(100, 300).exit(1500, 150)
                    .walls(new float[][]{{350, 0, 40, 700, 0}, {750, 200, 40, 700, 0}, {1150, 0, 40, 520, 0}})
                    .conveyors(new float[][]{{300, 700, 140, 200, -1, 0, 1200}, {1100, 520, 140, 380, -1, 0, 1200}})
                    .teles(new float[][]{{560, 600, 960, 300}})
                    .holes(new float[][]{{770, 100, 95, 610, 100}, {680, 620, 65, 500, 640}, {980, 450, 100, 840, 450}})
                    .stars(new float[][]{{200, 250}, {560, 250}, {1000, 800}})
                    .pickups(new float[][]{{200, 650}, {570, 800}, {1400, 600}},
                    Pickup.CLOCK, Pickup.SPRING, Pickup.MYSTERY),

            new Level("Cratère", 3, 100f, 1f)
                    .start(100, 800).exit(1500, 100)
                    .walls(new float[][]{{300, 0, 40, 640, 0}, {700, 260, 40, 640, 0}, {1100, 0, 40, 640, 0},
                            {1140, 300, 460, 40, 1}})
                    .doors(new float[][]{{300, 640, 40, 260, 3.0f, 0.55f, 0}})
                    .spinners(new float[][]{{720, 130, 100, 16, 1.3f, 0}})
                    .bumpers(new float[][]{{480, 350, 50}, {580, 600, 50}})
                    .movers(new float[][]{{760, 450, 150, 36, 190, 0, 2.0f, 0.25f}})
                    .holes(new float[][]{{1120, 770, 110, 960, 770}})
                    .mholes(new float[][]{{1370, 600, 70, 0, 150, 3.0f, 0, 1220, 820}})
                    .stars(new float[][]{{200, 300}, {540, 120}, {1500, 820}})
                    .pickups(new float[][]{{200, 600}, {1260, 640}, {900, 780}, {1400, 200}},
                    Pickup.CLOCK, Pickup.HAMMER, Pickup.CLOCK, Pickup.MYSTERY),
    };
}