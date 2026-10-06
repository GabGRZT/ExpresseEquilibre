package com.example.expresseequilibre;

/** Bonus (bulles rondes vertes) et malus (bulles en losange rouges). */
enum Pickup {
    CLOCK("Horloge : +5 s", true, 0f, Icons::clock),
    SPRING("Ressort : sauts longs", true, 12f, Icons::spring),
    MAGNET("Aimant : étoiles attirées", true, 10f, Icons::magnet),
    HAMMER("Marteau : casse de loin", true, 12f, Icons::hammer),
    INVERT("Commandes inversées !", false, 6f, Icons::invert),
    ICE("Glace : ça glisse !", false, 8f, Icons::snow),
    GIANT("Bille géante !", false, 8f, Icons::bigBall),
    FAST("Chrono accéléré !", false, 8f, Icons::fastClock),
    MYSTERY("Bulle mystère", true, 0f, Icons::question);

    final String label;
    final boolean bonus;
    final float duration;
    final UiButton.Icon icon;

    Pickup(String label, boolean bonus, float duration, UiButton.Icon icon) {
        this.label = label;
        this.bonus = bonus;
        this.duration = duration;
        this.icon = icon;
    }

    /** Tirage de la bulle mystère. */
    static final Pickup[] POOL = {CLOCK, SPRING, MAGNET, HAMMER, INVERT, ICE, GIANT, FAST};
}