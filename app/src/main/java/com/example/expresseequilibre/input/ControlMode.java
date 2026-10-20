package com.example.expresseequilibre.input;

/** Façon de piloter la bille (réglage sauvegardé). La secousse et le souffle restent actifs dans tous les modes. */
public enum ControlMode {
    TILT("Inclinaison", "Penche l'appareil pour faire rouler la bille.", "Secousse : saut ou mur cassé."),
    JOYSTICK("Joystick", "Pose un doigt sur le plateau : un joystick", "apparaît là où tu touches."),
    DRAG("Glisser", "Garde un doigt sur le plateau :", "la bille accélère vers lui.");

    public final String label, line1, line2;

    ControlMode(String label, String line1, String line2) {
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
    }

    public static ControlMode fromName(String s) {
        try {
            return valueOf(s);
        } catch (Exception e) {
            return TILT;
        }
    }
}
