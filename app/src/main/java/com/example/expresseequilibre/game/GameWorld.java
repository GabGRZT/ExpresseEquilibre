package com.example.expresseequilibre.game;

import com.example.expresseequilibre.data.Progress;
import com.example.expresseequilibre.input.Feedback;
import com.example.expresseequilibre.ui.Ui;
import android.media.ToneGenerator;

import java.util.Arrays;
import java.util.Random;


/**
 * Règles et physique du jeu, sans aucun dessin. Tout est en coordonnées logiques (plateau 1600 x 900) :
 * le comportement est identique sur tous les appareils.
 * Les champs publics sont lus par BoardRenderer et PlayHud ; seule cette classe les modifie.
 */
public final class GameWorld {
    public enum Outcome { PLAYING, WON, LOST }

    public static final float BALL_R = 36f, STAR_R = 32f, PICK_R = 34f;

    private static final float JUMP_AIR = 0.9f, JUMP_COOLDOWN = 1.6f;
    private static final float FAN_TIME = 1.0f, FAN_COOLDOWN = 3.5f, FAN_ACC = 1620f;
    private static final float TILT_ACC = 1800f, MAX_SPEED = 810f;
    private static final float FRICTION = 2.5f, BOUNCE = 0.35f;
    private static final float FALL_PENALTY = 3f, CLOCK_BONUS = 5f;
    private static final float BREAK_REACH = 60f, HAMMER_REACH = 220f;
    private static final float MAGNET_RANGE = 450f, MAGNET_SPEED = 380f;
    private static final float STROBE_PERIOD = 1.6f; // 0,625 flash/s : sous le seuil de sécurité (3/s)

    public final Effects effects = new Effects();
    public final Trail trail = new Trail();

    private final Feedback feedback;
    private final Progress progress;
    private final Random rnd = new Random();

    // ---------- Niveau ----------
    public Level level;
    public boolean[] wallBroken;
    public boolean[] pickupGot;
    public Star[] stars;

    // ---------- Bille ----------
    public float x, y, vx, vy;
    public float tiltX, tiltY, tiltMag;        // intention du joueur (après inversion éventuelle)
    public float airLeft, airTotal = JUMP_AIR; // saut en cours
    public float fanLeft, windDx = 1f, windDy; // ventilateur en cours et direction du vent
    public int nearWall = -1;                  // mur fragile à portée (-1 : aucun)

    // ---------- Partie ----------
    public float timeLeft, runTime;
    public int starCount;
    public Outcome outcome = Outcome.PLAYING;

    private boolean running;
    private float jumpCooldown, fanCooldown, strobeTime;
    private int lastStrobeBeat;
    private final float[] activeTime = new float[Pickup.values().length]; // durée restante de chaque effet

    public GameWorld(Feedback feedback, Progress progress) {
        this.feedback = feedback;
        this.progress = progress;
        load(Levels.ALL[0]);
    }

    public void load(Level lv) {
        level = lv;
        wallBroken = new boolean[lv.walls.length];
        pickupGot = new boolean[lv.pickups.length];
        stars = new Star[lv.stars.length];
        for (int i = 0; i < stars.length; i++) stars[i] = new Star();
    }

    public void reset() {
        timeLeft = level.time;
        starCount = 0;
        runTime = strobeTime = 0f;
        lastStrobeBeat = 0;
        outcome = Outcome.PLAYING;
        Arrays.fill(wallBroken, false);
        Arrays.fill(pickupGot, false);
        Arrays.fill(activeTime, 0f);
        for (int i = 0; i < stars.length; i++) stars[i].reset(level.stars[i]);
        airLeft = jumpCooldown = fanLeft = fanCooldown = 0f;
        nearWall = -1;
        trail.clear();
        effects.reset();
        x = level.startX;
        y = level.startY;
        vx = vy = 0f;
        running = false;
    }

    /** Pendant le compte à rebours la bille est figée et les commandes sont ignorées. */
    public void setRunning(boolean on) {
        running = on;
        vx = vy = 0f;
        if (!on) nearWall = -1;
    }

    public boolean isRunning() { return running; }

    // ---------- Lecture pour l'affichage ----------

    public boolean has(Pickup p) { return activeTime[p.ordinal()] > 0f; }

    public float remaining(Pickup p) { return activeTime[p.ordinal()]; }

    public float radius() { return BALL_R * (has(Pickup.GIANT) ? 1.6f : 1f); }

    /** 0 = saut en recharge, 1 = prêt. */
    public float jumpCharge() { return 1f - Ui.clamp(jumpCooldown / JUMP_COOLDOWN, 0f, 1f); }

    public float fanCharge() { return 1f - Ui.clamp(fanCooldown / FAN_COOLDOWN, 0f, 1f); }

    /** Hauteur du saut 0..1 (0 au sol). */
    public float jumpHeight() {
        return airLeft > 0f ? (float) Math.sin((1f - airLeft / airTotal) * Math.PI) : 0f;
    }

    public int score() {
        return starCount * 100 + (outcome == Outcome.WON ? 200 + (int) (timeLeft * 5f) : 0);
    }

    /** Luminosité 0..1 du plateau. Niveau stroboscopique : impulsions lentes, jamais plus de 1 flash/s. */
    public float strobeVisibility() {
        if (!level.strobe || !running) return 1f;
        if (progress.reducedFx) return 0.575f + 0.425f * (float) Math.sin(strobeTime * Math.PI); // fondu doux, 0,5 Hz
        float p = strobeTime % STROBE_PERIOD;
        float ramp = 0.06f, lit = 0.22f, floor = 0.10f;
        if (p < ramp) return floor + (1f - floor) * (p / ramp);
        if (p < lit) return 1f;
        return Math.max(floor, (float) Math.exp(-(p - lit) / 0.45f));
    }

    // ---------- Actions du joueur ----------

    /** Secousse (ou bouton) : casse un mur fragile à portée, sinon saut. */
    public void action() {
        if (!running) return;
        int wall = nearestFragile();
        if (wall >= 0) breakWall(wall);
        else jump();
    }

    /** Souffle (ou bouton) : le ventilateur pousse la bille. */
    public void fan() {
        if (!running || fanLeft > 0f || fanCooldown > 0f) return;
        fanLeft = FAN_TIME;
        fanCooldown = FAN_COOLDOWN;
        updateWindDirection();
        feedback.cue(40, ToneGenerator.TONE_PROP_BEEP2, 90);
    }

    private void jump() {
        if (airLeft > 0f || jumpCooldown > 0f) return;
        airTotal = JUMP_AIR * (has(Pickup.SPRING) ? 2f : 1f);
        airLeft = airTotal;
        jumpCooldown = JUMP_COOLDOWN;
        effects.ring(x, y, Ui.BALL);
        feedback.cue(25, ToneGenerator.TONE_PROP_BEEP, 70);
    }

    // ---------- Mise à jour (une image) ----------

    /** moveX / moveY : intention du joueur, -1..1 (inclinaison ou manette). */
    public void update(float dt, float moveX, float moveY) {
        runTime += dt;
        strobeTime += dt;
        pulseOnStrobeBeat();
        nearWall = nearestFragile();
        tickTimers(dt);

        timeLeft -= dt * (has(Pickup.FAST) ? 2f : 1f);
        if (timeLeft <= 0f) { end(false); return; }

        steer(dt, moveX, moveY);
        blowWind(dt);
        applyFriction(dt);

        float r = radius();
        x += vx * dt;
        y += vy * dt;
        collideBoard(r);
        for (int i = 0; i < level.walls.length; i++) {
            if (!wallBroken[i]) collideWall(level.walls[i], r);
        }
        trail.record(dt, x, y);

        Hole hole = holeUnderBall();
        if (hole != null) {
            fall(hole);
            if (timeLeft <= 0f) { end(false); return; }
        }
        collectStars(dt, r);
        collectPickups(r);
        if (atExit()) {
            for (Star s : stars) if (!s.collected && s.pulling) collect(s); // l'étoile attirée est validée à la sortie
            end(true);
        }
    }

    private void pulseOnStrobeBeat() {
        if (!level.strobe || progress.reducedFx) return;
        int beat = (int) (strobeTime / STROBE_PERIOD);
        if (beat != lastStrobeBeat) {
            lastStrobeBeat = beat;
            feedback.vibrate(8);
        }
    }

    private void tickTimers(float dt) {
        jumpCooldown = Math.max(0f, jumpCooldown - dt);
        fanCooldown = Math.max(0f, fanCooldown - dt);
        if (airLeft > 0f) airLeft = Math.max(0f, airLeft - dt);
        for (int i = 0; i < activeTime.length; i++) activeTime[i] = Math.max(0f, activeTime[i] - dt);
    }

    private void steer(float dt, float moveX, float moveY) {
        float sign = has(Pickup.INVERT) ? -1f : 1f;
        tiltX = moveX * sign;
        tiltY = moveY * sign;
        vx += tiltX * TILT_ACC * dt;
        vy += tiltY * TILT_ACC * dt;
        tiltMag = Math.min(1f, (float) Math.hypot(tiltX, tiltY));
    }

    private void blowWind(float dt) {
        if (fanLeft <= 0f) return;
        fanLeft = Math.max(0f, fanLeft - dt);
        updateWindDirection();
        vx += windDx * FAN_ACC * dt;
        vy += windDy * FAN_ACC * dt;
    }

    /** Le vent suit l'inclinaison ; sans inclinaison, il pousse vers la sortie. */
    private void updateWindDirection() {
        float m = (float) Math.hypot(tiltX, tiltY);
        if (m > 0.25f) { windDx = tiltX / m; windDy = tiltY / m; return; }
        float dx = level.exitX - x, dy = level.exitY - y;
        float d = (float) Math.max(1e-3, Math.hypot(dx, dy));
        windDx = dx / d;
        windDy = dy / d;
    }

    private void applyFriction(float dt) {
        float friction = FRICTION * level.friction * (has(Pickup.ICE) ? 0.25f : 1f);
        float damping = Math.max(0f, 1f - friction * dt);
        vx *= damping;
        vy *= damping;
        float speed = (float) Math.hypot(vx, vy);
        if (speed > MAX_SPEED) { vx *= MAX_SPEED / speed; vy *= MAX_SPEED / speed; }
    }

    // ---------- Collisions ----------

    private void collideBoard(float r) {
        if (x < r) { x = r; vx = Math.abs(vx) * BOUNCE; }
        else if (x > Level.W - r) { x = Level.W - r; vx = -Math.abs(vx) * BOUNCE; }
        if (y < r) { y = r; vy = Math.abs(vy) * BOUNCE; }
        else if (y > Level.H - r) { y = Level.H - r; vy = -Math.abs(vy) * BOUNCE; }
    }

    /** Collision cercle / rectangle : repousse la bille et la fait rebondir sur la normale. */
    private void collideWall(Wall wall, float r) {
        float cx = Ui.clamp(x, wall.x, wall.right());
        float cy = Ui.clamp(y, wall.y, wall.bottom());
        float dx = x - cx, dy = y - cy;
        float d2 = dx * dx + dy * dy;
        if (d2 >= r * r) return;

        float nx, ny;
        if (d2 > 1e-4f) { // le centre est hors du mur : normale = direction du point le plus proche
            float d = (float) Math.sqrt(d2);
            nx = dx / d;
            ny = dy / d;
            x = cx + nx * r;
            y = cy + ny * r;
        } else {          // le centre est dans le mur : on le sort par le bord le plus proche
            float left = x - wall.x, right = wall.right() - x, top = y - wall.y, bottom = wall.bottom() - y;
            float m = Math.min(Math.min(left, right), Math.min(top, bottom));
            if (m == left) { nx = -1f; ny = 0f; x = wall.x - r; }
            else if (m == right) { nx = 1f; ny = 0f; x = wall.right() + r; }
            else if (m == top) { nx = 0f; ny = -1f; y = wall.y - r; }
            else { nx = 0f; ny = 1f; y = wall.bottom() + r; }
        }
        float vn = vx * nx + vy * ny;
        if (vn < 0f) {
            vx -= (1f + BOUNCE) * vn * nx;
            vy -= (1f + BOUNCE) * vn * ny;
        }
    }

    // ---------- Murs fragiles ----------

    /** Indice du mur fragile intact le plus proche à portée de casse, ou -1. */
    private int nearestFragile() {
        float reach = has(Pickup.HAMMER) ? HAMMER_REACH : BREAK_REACH;
        float r = radius();
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < level.walls.length; i++) {
            Wall wall = level.walls[i];
            if (!wall.fragile || wallBroken[i]) continue;
            float cx = Ui.clamp(x, wall.x, wall.right()), cy = Ui.clamp(y, wall.y, wall.bottom());
            float d = (float) Math.hypot(x - cx, y - cy) - r;
            if (d <= reach && d < bestDistance) { bestDistance = d; best = i; }
        }
        return best;
    }

    private void breakWall(int i) {
        wallBroken[i] = true;
        effects.debris(level.walls[i], Ui.FRAGILE);
        effects.shake = 1f;
        jumpCooldown = Math.max(jumpCooldown, 0.4f);
        effects.toast("Mur brisé !", Ui.OK);
        feedback.cue(70, ToneGenerator.TONE_PROP_BEEP2, 120);
    }

    // ---------- Trous, étoiles, bonus, sortie ----------

    /** Trou sous la bille (sans effet quand elle est en l'air), ou null. */
    private Hole holeUnderBall() {
        if (airLeft > 0f) return null;
        for (Hole hole : level.holes) {
            float dx = x - hole.x, dy = y - hole.y;
            if (dx * dx + dy * dy < hole.r * hole.r) return hole;
        }
        return null;
    }

    private void fall(Hole hole) {
        timeLeft -= FALL_PENALTY;
        x = hole.respawnX;
        y = hole.respawnY;
        vx = vy = 0f;
        airLeft = jumpCooldown = 0f;
        effects.flash = 1f;
        effects.shake = 0.6f;
        trail.clear();
        effects.toast("Dans le trou !  -3 s", Ui.DANGER);
        feedback.cue(90, ToneGenerator.TONE_PROP_NACK, 200);
    }

    private void collectStars(float dt, float r) {
        for (Star s : stars) {
            s.pulling = false;
            if (s.collected) continue;
            float dx = x - s.x, dy = y - s.y;
            float d = (float) Math.hypot(dx, dy);
            if (has(Pickup.MAGNET) && d < MAGNET_RANGE && d > 1f) {
                s.x += dx / d * MAGNET_SPEED * dt;
                s.y += dy / d * MAGNET_SPEED * dt;
                s.pulling = true;
            }
            if (d < r + STAR_R) collect(s);
        }
    }

    private void collect(Star s) {
        s.collected = true;
        starCount++;
        effects.ring(s.x, s.y, Ui.STAR);
        effects.burst(s.x, s.y, Ui.STAR, 10, 260f, 9f);
        effects.toast("Étoile " + starCount + "/3", Ui.OK);
        feedback.cue(30, ToneGenerator.TONE_PROP_ACK, 120);
    }

    private void collectPickups(float r) {
        for (int i = 0; i < pickupGot.length; i++) {
            if (pickupGot[i]) continue;
            PickupSpot p = level.pickups[i];
            float dx = x - p.x, dy = y - p.y;
            if (dx * dx + dy * dy < (r + PICK_R) * (r + PICK_R)) {
                pickupGot[i] = true;
                apply(p);
            }
        }
    }

    private void apply(PickupSpot spot) {
        Pickup pickup = spot.type == Pickup.MYSTERY ? Pickup.POOL[rnd.nextInt(Pickup.POOL.length)] : spot.type;
        if (pickup == Pickup.CLOCK) timeLeft += CLOCK_BONUS;
        else activeTime[pickup.ordinal()] = pickup.duration;
        int color = pickup.bonus ? Ui.OK : Ui.DANGER;
        effects.ring(spot.x, spot.y, color);
        effects.burst(spot.x, spot.y, color, 8, 220f, 8f);
        effects.toast(pickup.label, color);
        feedback.cue(pickup.bonus ? 25 : 60, pickup.bonus ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 120);
    }

    private boolean atExit() {
        float dx = x - level.exitX, dy = y - level.exitY;
        return dx * dx + dy * dy < Level.EXIT_R * Level.EXIT_R;
    }

    private void end(boolean win) {
        timeLeft = Math.max(0f, timeLeft);
        outcome = win ? Outcome.WON : Outcome.LOST;
        running = false;
        feedback.cue(win ? 200 : 120, win ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 300);
    }
}
