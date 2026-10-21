package com.example.expresseequilibre.game;

/** Catalogue de la campagne : 3 mondes, 6 niveaux. */
public final class Levels {
    private Levels() {}

    private static final boolean SOLID = false, FRAGILE = true;

    public static final Level[] ALL = {

            // ---- Monde 1 : Prairie ----
            new Level("Premiers pas", 0, 60f, 1f, false, 112, 180, 1440, 666,
                    new Wall[]{new Wall(480, 0, 40, 612, SOLID), new Wall(992, 288, 40, 612, SOLID)},
                    new Hole[]{new Hole(500, 756, 135, 286, 756), new Hole(1012, 144, 135, 798, 144), new Hole(752, 450, 81, 752, 610)},
                    new Spot[]{new Spot(160, 558), new Spot(720, 270), new Spot(1248, 252)},
                    new PickupSpot[]{new PickupSpot(900, 700, Pickup.CLOCK)}),

            new Level("Casse-murs", 0, 60f, 1f, false, 110, 450, 1450, 770,
                    new Wall[]{new Wall(500, 0, 40, 600, SOLID), new Wall(500, 600, 40, 300, FRAGILE),
                            new Wall(1040, 300, 40, 600, SOLID), new Wall(1080, 560, 520, 40, FRAGILE)},
                    new Hole[]{new Hole(790, 450, 110, 601, 450)},
                    new Spot[]{new Spot(200, 750), new Spot(800, 150), new Spot(1220, 790)},
                    new PickupSpot[]{new PickupSpot(1000, 150, Pickup.HAMMER), new PickupSpot(1300, 300, Pickup.CLOCK)}),

            new Level("Bulles", 0, 60f, 1f, false, 100, 450, 1480, 470,
                    new Wall[]{new Wall(400, 0, 40, 400, SOLID), new Wall(900, 500, 40, 400, SOLID)},
                    new Hole[]{new Hole(700, 250, 100, 700, 429), new Hole(650, 700, 120, 650, 501),
                            new Hole(1200, 300, 110, 1200, 489), new Hole(1250, 700, 90, 1250, 531)},
                    new Spot[]{new Spot(250, 700), new Spot(900, 200), new Spot(1100, 500)},
                    new PickupSpot[]{new PickupSpot(560, 450, Pickup.CLOCK), new PickupSpot(1000, 300, Pickup.SPRING),
                            new PickupSpot(300, 300, Pickup.INVERT), new PickupSpot(800, 450, Pickup.MYSTERY),
                            new PickupSpot(1150, 850, Pickup.MAGNET), new PickupSpot(1450, 250, Pickup.ICE)}),

            // ---- Monde 2 : Glace (friction réduite) ----
            new Level("Patinoire", 1, 70f, 0.3f, false, 100, 120, 1480, 780,
                    new Wall[]{new Wall(0, 300, 1100, 40, SOLID), new Wall(500, 580, 1100, 40, SOLID)},
                    new Hole[]{new Hole(800, 460, 90, 631, 460), new Hole(1000, 760, 60, 861, 760)},
                    new Spot[]{new Spot(400, 150), new Spot(1400, 450), new Spot(250, 760)},
                    new PickupSpot[]{new PickupSpot(1300, 500, Pickup.SPRING), new PickupSpot(300, 450, Pickup.CLOCK)}),

            new Level("Glissade", 1, 75f, 0.35f, false, 100, 450, 1450, 200,
                    new Wall[]{new Wall(400, 0, 40, 650, SOLID), new Wall(800, 0, 40, 560, SOLID),
                            new Wall(800, 560, 40, 340, FRAGILE), new Wall(1200, 300, 40, 600, SOLID)},
                    new Hole[]{new Hole(1220, 150, 110, 1031, 150)},
                    new Spot[]{new Spot(200, 800), new Spot(620, 200), new Spot(1400, 600)},
                    new PickupSpot[]{new PickupSpot(600, 750, Pickup.HAMMER), new PickupSpot(1000, 450, Pickup.MYSTERY),
                            new PickupSpot(1050, 300, Pickup.SPRING)}),

            // ---- Monde 3 : Néon (stroboscopique) ----
            new Level("Stroboscope", 2, 75f, 1f, true, 100, 450, 1500, 450,
                    new Wall[]{new Wall(400, 0, 40, 600, SOLID), new Wall(800, 300, 40, 600, SOLID), new Wall(1200, 0, 40, 600, SOLID)},
                    new Hole[]{new Hole(560, 160, 80, 560, 319), new Hole(1020, 720, 90, 1020, 551), new Hole(1450, 780, 90, 1450, 611)},
                    new Spot[]{new Spot(250, 250), new Spot(620, 650), new Spot(1020, 150)},
                    new PickupSpot[]{new PickupSpot(250, 700, Pickup.CLOCK), new PickupSpot(1000, 450, Pickup.CLOCK),
                            new PickupSpot(1330, 300, Pickup.MYSTERY)}),
    };
}
