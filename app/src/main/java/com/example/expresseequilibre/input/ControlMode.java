package com.example.expresseequilibre.input;

/** Façon de piloter la bille (réglage sauvegardé). La secousse et le souffle restent actifs dans les deux modes. */
public enum ControlMode {
    TILT("Inclinaison", "Penche l'appareil pour faire rouler la bille.", "Secousse : saut ou mur cassé."),
    JOYSTICK("Joystick", "Une manette à l'écran pilote la bille ;", "Saut et Vent sont de l'autre côté.");

    public final String label, line1, line2;

    ControlMode(String label, String line1, String line2) {
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
    }

    public static ControlMode fromName(String s) {
        if ("DRAG".equals(s)) return JOYSTICK; // ancien mode "Glisser" supprimé
        try {
            return valueOf(s);
        } catch (Exception e) {
            return TILT;
        }
    }
}
