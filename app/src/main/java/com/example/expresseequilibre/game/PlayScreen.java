package com.example.expresseequilibre.game;

import com.example.expresseequilibre.input.ControlMode;
import com.example.expresseequilibre.screens.Screen;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import com.example.expresseequilibre.ui.UiButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ToneGenerator;

import java.util.Random;

/**
 * Écran de jeu. Toute la physique est en coordonnées logiques (plateau 1600 x 900) :
 * le comportement est identique sur tous les appareils.
 */
public final class PlayScreen extends Screen {

    private enum Phase { COUNTDOWN, RUN }

    private static final float BALL_R = 36f, STAR_R = 32f, PICK_R = 34f;
    private static final float COUNTDOWN_TIME = 3f;
    private static final float JUMP_AIR = 0.9f, JUMP_COOLDOWN = 1.6f;
    private static final float FAN_TIME = 1.0f, FAN_COOLDOWN = 3.5f;
    private static final float FALL_PENALTY = 3f;
    private static final float TILT_ACC = 1800f, MAX_SPEED = 810f, FAN_ACC = 1620f;
    private static final float FRICTION = 2.5f, BOUNCE = 0.35f;
    private static final float BREAK_REACH = 60f, HAMMER_REACH = 220f;
    private static final float STROBE_PERIOD = 1.6f; // 0,625 flash/s : sous le seuil de sécurité (3/s)
    private static final float RING_LIFE = 0.5f;
    private static final int PMAX = 80;
    private static final int FRAG = Color.rgb(186, 140, 92);
    private static final int FRAG_TOP = Color.rgb(214, 172, 120);

    // ---------- Niveau courant ----------
    private Level level = Levels.ALL[0];
    private boolean[] wallBroken = new boolean[0];
    private boolean[] starGot = new boolean[0];
    private boolean[] pickGot = new boolean[0];
    private boolean[] pulling = new boolean[0];   // étoile attirée par l'aimant à cette image
    private float[] starX = new float[0], starY = new float[0];

    // ---------- Boutons ----------
    private final UiButton pauseBtn = new UiButton("Pause", Ui.SLATE, Icons::pause);
    private final UiButton actionBtn = new UiButton("Saut", Ui.BUTTON, Icons::jump);
    private final UiButton blowBtn = new UiButton("Souffle", Ui.TEAL, Icons::wind);

    // ---------- État ----------
    private Phase phase = Phase.COUNTDOWN;
    private float countdown = COUNTDOWN_TIME;
    private int lastCi;
    private boolean resumeMode;
    private float timeLeft, runT, strobeT;
    private int lastStrobeBeat, starCount;
    private final float[] eff = new float[Pickup.values().length];

    private float x, y, vx, vy;
    private float airLeft, airTotal = JUMP_AIR, jumpCd, fanLeft, fanCd;
    private float tiltX, tiltY, tiltMag;
    private float windDx = 1f, windDy;
    private float anim, flash, shakeT;
    private int nearIdx = -1;

    // contrôle tactile (joystick / glisser) : doigt qui pilote la bille
    private int ctlId = -1;
    private float ctlBaseX, ctlBaseY, ctlX, ctlY;

    private String toastText = "";
    private int toastColor = Ui.OK;
    private float toastT;

    private final float[] ringX = new float[6], ringY = new float[6], ringLife = new float[6];
    private final int[] ringColor = new int[6];
    private int ringNext;

    private final float[] pX = new float[PMAX], pY = new float[PMAX], pVX = new float[PMAX], pVY = new float[PMAX];
    private final float[] pLife = new float[PMAX], pMax = new float[PMAX], pSize = new float[PMAX];
    private final int[] pCol = new int[PMAX];
    private int pNext;

    private final float[] trX = new float[10], trY = new float[10];
    private int trHead, trCount;
    private float trAcc;

    private final Random rnd = new Random();

    // ---------- Géométrie écran ----------
    private long layoutKey = -1;
    private float bl, bt, br, bb, bw, bh, s = 1f;
    private float acl, acr;                 // colonne gauche (ou colonne des boutons en mode Joystick)
    private float rcl, rcr;                 // colonne droite (Souffle) : modes sans manette
    private float zl, zr, zt, zb;           // zone de la manette (mode Joystick)
    private boolean joyZone;
    private final Path clip = new Path();

    public PlayScreen(GameView g) {
        super(g);
        actionBtn.fillVertical = true;
        blowBtn.fillVertical = true;
    }

    public int starCount() { return starCount; }

    public float timeLeft() { return timeLeft; }

    public int worldIndex() { return level.world; }

    // ---------- Chargement / démarrage ----------

    public void load(Level lv) {
        level = lv;
        wallBroken = new boolean[lv.walls.length];
        starGot = new boolean[lv.stars.length];
        pickGot = new boolean[lv.pickups.length];
        pulling = new boolean[lv.stars.length];
        starX = new float[lv.stars.length];
        starY = new float[lv.stars.length];
    }

    public void reset() {
        timeLeft = level.time;
        starCount = 0;
        runT = 0f;
        strobeT = 0f;
        lastStrobeBeat = 0;
        for (int i = 0; i < wallBroken.length; i++) wallBroken[i] = false;
        for (int i = 0; i < starGot.length; i++) {
            starGot[i] = false;
            starX[i] = level.stars[i][0];
            starY[i] = level.stars[i][1];
        }
        for (int i = 0; i < pickGot.length; i++) pickGot[i] = false;
        for (int i = 0; i < pulling.length; i++) pulling[i] = false;
        for (int i = 0; i < eff.length; i++) eff[i] = 0f;
        airLeft = jumpCd = fanLeft = fanCd = 0f;
        flash = shakeT = toastT = 0f;
        nearIdx = -1;
        trCount = 0;
        for (int i = 0; i < ringLife.length; i++) ringLife[i] = 0f;
        for (int i = 0; i < PMAX; i++) pLife[i] = 0f;
        x = level.startX;
        y = level.startY;
        vx = vy = 0f;
        beginCountdown(false);
    }

    public void beginCountdown(boolean resume) {
        phase = Phase.COUNTDOWN;
        countdown = COUNTDOWN_TIME;
        lastCi = 0;
        resumeMode = resume;
        vx = vy = 0f;
        ctlId = -1; // un doigt resté posé avant la reprise ne pilote plus
    }

    // ---------- Mise en page ----------

    private void layout(int w, int h) {
        int il = g.insetLeft, ir = g.insetRight, it = g.insetTop;
        ControlMode mode = g.progress.controlMode;
        boolean padLeft = g.progress.padLeft;
        long key = ((((long) w * 31 + h) * 31 + il) * 31 + ir) * 31 + it;
        key = key * 31 + mode.ordinal() * 2 + (padLeft ? 1 : 0);
        if (key == layoutKey) return;
        layoutKey = key;

        float margin = Ui.dp(10);
        float usable = w - il - ir;
        float colW = Math.max(Ui.dp(80), usable * 0.13f);
        joyZone = mode == ControlMode.JOYSTICK;
        float zoneW = joyZone ? Math.max(Ui.dp(150), usable * 0.19f) : 0f;
        zt = h * 0.06f;
        zb = h * 0.94f;

        float al, ar;
        if (!joyZone) { // Inclinaison : comme avant, un bouton Souffle de chaque côté
            float cw = Math.max(Ui.dp(70), usable * 0.12f);
            acl = il + margin;
            acr = acl + cw;
            rcr = w - ir - margin;
            rcl = rcr - cw;
            al = acr + margin;
            ar = rcl - margin;
        } else if (!padLeft) { // Joystick : boutons à gauche, manette à droite (défaut)
            acl = il + margin;
            acr = acl + colW;
            zr = w - ir - margin;
            zl = zr - zoneW;
            al = acr + margin;
            ar = zl - margin;
        } else {               // Joystick inversé
            acr = w - ir - margin;
            acl = acr - colW;
            zl = il + margin;
            zr = zl + zoneW;
            al = zr + margin;
            ar = acl - margin;
        }
        float at = Math.max(h * 0.15f, it + h * 0.10f), ab = h * 0.96f;
        float aw = ar - al, ah = ab - at;
        s = Math.min(aw / Level.W, ah / Level.H);   // échelle uniforme : jamais d'écrasement
        bw = Level.W * s;
        bh = Level.H * s;
        bl = al + (aw - bw) / 2f;
        bt = at;
        br = bl + bw;
        bb = bt + bh;
    }

    // ---------- Mise à jour ----------

    private float radius() { return BALL_R * (on(Pickup.GIANT) ? 1.6f : 1f); }

    private boolean on(Pickup p) { return eff[p.ordinal()] > 0f; }

    @Override
    public void update(float dt) {
        int w = g.getWidth(), h = g.getHeight();
        if (w <= 0 || h <= 0) return;
        layout(w, h);

        anim += dt;
        flash = Math.max(0f, flash - dt * 2.5f);
        shakeT = Math.max(0f, shakeT - dt * 3f);
        toastT = Math.max(0f, toastT - dt);
        for (int i = 0; i < ringLife.length; i++) ringLife[i] = Math.max(0f, ringLife[i] - dt);
        updateParticles(dt);

        g.sensors.update(dt);
        boolean shake = g.sensors.consumeShake();
        boolean blow = g.sensors.consumeBlow();

        if (phase == Phase.COUNTDOWN) { nearIdx = -1; updateCountdown(dt); return; }

        runT += dt;
        strobeT += dt;
        if (level.strobe && !g.progress.reducedFx) {
            int beat = (int) (strobeT / STROBE_PERIOD);
            if (beat != lastStrobeBeat) { lastStrobeBeat = beat; g.feedback.vibrate(8); }
        }

        nearIdx = nearestFragile();
        if (shake) onAction();
        if (blow) tryFan();

        jumpCd = Math.max(0f, jumpCd - dt);
        fanCd = Math.max(0f, fanCd - dt);
        if (airLeft > 0f) airLeft = Math.max(0f, airLeft - dt);
        for (int i = 0; i < eff.length; i++) if (eff[i] > 0f) eff[i] = Math.max(0f, eff[i] - dt);

        timeLeft -= dt * (on(Pickup.FAST) ? 2f : 1f);
        if (timeLeft <= 0f) { timeLeft = 0f; finish(false); return; }

        // intention du joueur -> accélération (inversée par le malus), selon le mode de contrôle choisi
        float sgn = on(Pickup.INVERT) ? -1f : 1f;
        float ax = 0f, ay = 0f;
        if (g.progress.controlMode == ControlMode.TILT) {
            ax = g.sensors.tiltX();
            ay = g.sensors.tiltY();
        } else if (ctlId >= 0) { // manette flottante
            float jr = stickRadius();
            float dx = ctlX - ctlBaseX, dy = ctlY - ctlBaseY;
            float d = (float) Math.hypot(dx, dy);
            if (d > jr * 0.12f) {
                float k = Math.min(1f, d / jr);
                k *= 0.4f + 0.6f * k;
                ax = dx / d * k;
                ay = dy / d * k;
            }
        }
        tiltX = ax * sgn;
        tiltY = ay * sgn;
        vx += tiltX * TILT_ACC * dt;
        vy += tiltY * TILT_ACC * dt;
        tiltMag = Math.min(1f, (float) Math.hypot(tiltX, tiltY));

        if (fanLeft > 0f) {
            fanLeft = Math.max(0f, fanLeft - dt);
            updateWindDir();
            vx += windDx * FAN_ACC * dt;
            vy += windDy * FAN_ACC * dt;
        }

        float fr = FRICTION * level.friction * (on(Pickup.ICE) ? 0.25f : 1f);
        float damp = Math.max(0f, 1f - fr * dt);
        vx *= damp;
        vy *= damp;
        float sp = (float) Math.hypot(vx, vy);
        if (sp > MAX_SPEED) { vx *= MAX_SPEED / sp; vy *= MAX_SPEED / sp; }

        x += vx * dt;
        y += vy * dt;
        float r = radius();
        collideBoard(r);
        for (int i = 0; i < level.walls.length; i++) {
            if (wallBroken[i]) continue;
            float[] wl = level.walls[i];
            collideRect(wl[0], wl[1], wl[0] + wl[2], wl[1] + wl[3], r);
        }

        // traînée
        trAcc += dt;
        if (trAcc >= 0.03f) {
            trAcc = 0f;
            trX[trHead] = x;
            trY[trHead] = y;
            trHead = (trHead + 1) % trX.length;
            trCount = Math.min(trX.length, trCount + 1);
        }

        // trous : sans effet quand la bille est en l'air
        if (airLeft <= 0f) {
            for (int i = 0; i < level.holes.length; i++) {
                float[] hl = level.holes[i];
                float dx = x - hl[0], dy = y - hl[1];
                if (dx * dx + dy * dy < hl[2] * hl[2]) {
                    fall(i);
                    if (timeLeft <= 0f) { timeLeft = 0f; finish(false); return; }
                    break;
                }
            }
        }

        // étoiles (+ aimant)
        for (int i = 0; i < starGot.length; i++) {
            pulling[i] = false;
            if (starGot[i]) continue;
            float dx = x - starX[i], dy = y - starY[i];
            float d = (float) Math.hypot(dx, dy);
            if (on(Pickup.MAGNET) && d < 450f && d > 1f) {
                starX[i] += dx / d * 380f * dt;
                starY[i] += dy / d * 380f * dt;
                pulling[i] = true;
            }
            if (d < r + STAR_R) collectStar(i);
        }

        // bonus / malus
        for (int i = 0; i < pickGot.length; i++) {
            if (pickGot[i]) continue;
            float dx = x - level.pickups[i][0], dy = y - level.pickups[i][1];
            if (dx * dx + dy * dy < (r + PICK_R) * (r + PICK_R)) {
                pickGot[i] = true;
                apply(level.ptypes[i], level.pickups[i][0], level.pickups[i][1]);
            }
        }

        // sortie
        float ex = x - level.exitX, ey = y - level.exitY;
        if (ex * ex + ey * ey < Level.EXIT_R * Level.EXIT_R) {
            // filet de sécurité : une étoile en train d'être attirée par l'aimant est validée à la sortie
            for (int i = 0; i < starGot.length; i++) if (!starGot[i] && pulling[i]) collectStar(i);
            finish(true);
        }
    }

    private void collectStar(int i) {
        starGot[i] = true;
        starCount++;
        addRing(starX[i], starY[i], Ui.STAR);
        burst(starX[i], starY[i], Ui.STAR, 10, 260f, 9f);
        toast("Étoile " + starCount + "/3", Ui.OK);
        g.feedback.vibrate(30);
        g.feedback.tone(ToneGenerator.TONE_PROP_ACK, 120);
    }

    private void apply(Pickup pk, float px, float py) {
        if (pk == Pickup.MYSTERY) pk = Pickup.POOL[rnd.nextInt(Pickup.POOL.length)];
        if (pk == Pickup.CLOCK) timeLeft += 5f;
        else eff[pk.ordinal()] = pk.duration;
        int col = pk.bonus ? Ui.OK : Ui.DANGER;
        addRing(px, py, col);
        burst(px, py, col, 8, 220f, 8f);
        toast(pk.label, col);
        g.feedback.vibrate(pk.bonus ? 25 : 60);
        g.feedback.tone(pk.bonus ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 120);
    }

    private void updateCountdown(float dt) {
        countdown -= dt;
        int ci = (int) Math.ceil(Math.max(0f, countdown));
        if (ci != lastCi && ci > 0) {
            lastCi = ci;
            g.feedback.tone(ToneGenerator.TONE_PROP_BEEP, 80);
        }
        if (countdown <= 0f) {
            phase = Phase.RUN;
            g.sensors.calibrate(); // mode "à plat" : neutre = horizontale (aucun décalage) ; sinon position actuelle
            toast("GO !", Ui.OK);
            g.feedback.vibrate(40);
            g.feedback.tone(ToneGenerator.TONE_PROP_BEEP2, 200);
        }
    }

    /** Secousse (ou bouton) : casse un mur fragile à portée, sinon saut. */
    private void onAction() {
        if (phase != Phase.RUN) return;
        int w = nearestFragile();
        if (w >= 0) { breakWall(w); return; }
        tryJump();
    }

    private int nearestFragile() {
        float reach = on(Pickup.HAMMER) ? HAMMER_REACH : BREAK_REACH;
        float r = radius();
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < level.walls.length; i++) {
            float[] wl = level.walls[i];
            if (wl[4] != 1f || wallBroken[i]) continue;
            float cx = Ui.clamp(x, wl[0], wl[0] + wl[2]);
            float cy = Ui.clamp(y, wl[1], wl[1] + wl[3]);
            float d = (float) Math.hypot(x - cx, y - cy) - r;
            if (d <= reach && d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    private void breakWall(int i) {
        wallBroken[i] = true;
        float[] wl = level.walls[i];
        for (int k = 0; k < 20; k++) {
            float sx = wl[0] + rnd.nextFloat() * wl[2], sy = wl[1] + rnd.nextFloat() * wl[3];
            float a = rnd.nextFloat() * 6.2832f, v = 120f + rnd.nextFloat() * 260f;
            spawn(sx, sy, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 0.6f + rnd.nextFloat() * 0.5f, 12f, FRAG);
        }
        addRing(wl[0] + wl[2] / 2f, wl[1] + wl[3] / 2f, FRAG);
        shakeT = 1f;
        jumpCd = Math.max(jumpCd, 0.4f);
        toast("Mur brisé !", Ui.OK);
        g.feedback.vibrate(70);
        g.feedback.tone(ToneGenerator.TONE_PROP_BEEP2, 120);
    }

    private void tryJump() {
        if (phase != Phase.RUN || airLeft > 0f || jumpCd > 0f) return;
        airTotal = JUMP_AIR * (on(Pickup.SPRING) ? 2f : 1f);
        airLeft = airTotal;
        jumpCd = JUMP_COOLDOWN;
        addRing(x, y, Ui.BALL);
        g.feedback.vibrate(25);
        g.feedback.tone(ToneGenerator.TONE_PROP_BEEP, 70);
    }

    private void tryFan() {
        if (phase != Phase.RUN || fanLeft > 0f || fanCd > 0f) return;
        fanLeft = FAN_TIME;
        fanCd = FAN_COOLDOWN;
        updateWindDir();
        g.feedback.vibrate(40);
        g.feedback.tone(ToneGenerator.TONE_PROP_BEEP2, 90);
    }

    private void updateWindDir() {
        float m = (float) Math.hypot(tiltX, tiltY);
        if (m > 0.25f) { windDx = tiltX / m; windDy = tiltY / m; return; }
        float dx = level.exitX - x, dy = level.exitY - y;
        float d = (float) Math.max(1e-3, Math.hypot(dx, dy));
        windDx = dx / d;
        windDy = dy / d;
    }

    private void fall(int i) {
        timeLeft -= FALL_PENALTY;
        x = level.holes[i][3];
        y = level.holes[i][4];
        vx = vy = 0f;
        airLeft = 0f;
        jumpCd = 0f;
        flash = 1f;
        shakeT = 0.6f;
        trCount = 0;
        toast("Dans le trou !  -3 s", Ui.DANGER);
        g.feedback.vibrate(90);
        g.feedback.tone(ToneGenerator.TONE_PROP_NACK, 200);
    }

    private void finish(boolean win) {
        int score = starCount * 100 + (win ? 200 + (int) (timeLeft * 5f) : 0);
        g.feedback.vibrate(win ? 200 : 120);
        g.feedback.tone(win ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 300);
        g.finishGame(win, starCount, score, (int) timeLeft, runT);
    }

    private float stickRadius() { return Math.min((zr - zl) * 0.40f, Math.max(Ui.dp(60), bh * 0.20f)); }

    private void collideBoard(float r) {
        if (x < r) { x = r; vx = Math.abs(vx) * BOUNCE; }
        else if (x > Level.W - r) { x = Level.W - r; vx = -Math.abs(vx) * BOUNCE; }
        if (y < r) { y = r; vy = Math.abs(vy) * BOUNCE; }
        else if (y > Level.H - r) { y = Level.H - r; vy = -Math.abs(vy) * BOUNCE; }
    }

    private void collideRect(float l, float t, float rt, float b, float r) {
        float cx = Math.max(l, Math.min(x, rt));
        float cy = Math.max(t, Math.min(y, b));
        float dx = x - cx, dy = y - cy;
        float d2 = dx * dx + dy * dy;
        if (d2 >= r * r) return;

        float nx, ny;
        if (d2 > 1e-4f) {
            float d = (float) Math.sqrt(d2);
            nx = dx / d;
            ny = dy / d;
            x = cx + nx * r;
            y = cy + ny * r;
        } else {
            float pl = x - l, pr = rt - x, pt = y - t, pb = b - y;
            float m = Math.min(Math.min(pl, pr), Math.min(pt, pb));
            if (m == pl) { nx = -1f; ny = 0f; x = l - r; }
            else if (m == pr) { nx = 1f; ny = 0f; x = rt + r; }
            else if (m == pt) { nx = 0f; ny = -1f; y = t - r; }
            else { nx = 0f; ny = 1f; y = b + r; }
        }
        float vn = vx * nx + vy * ny;
        if (vn < 0f) {
            vx -= (1f + BOUNCE) * vn * nx;
            vy -= (1f + BOUNCE) * vn * ny;
        }
    }

    private void toast(String t, int color) {
        toastText = t;
        toastColor = color;
        toastT = 1.4f;
    }

    private void addRing(float rx, float ry, int color) {
        ringX[ringNext] = rx;
        ringY[ringNext] = ry;
        ringColor[ringNext] = color;
        ringLife[ringNext] = RING_LIFE;
        ringNext = (ringNext + 1) % ringLife.length;
    }

    private void spawn(float sx, float sy, float svx, float svy, float life, float size, int col) {
        int i = pNext;
        pNext = (pNext + 1) % PMAX;
        pX[i] = sx; pY[i] = sy; pVX[i] = svx; pVY[i] = svy;
        pLife[i] = life; pMax[i] = life; pSize[i] = size; pCol[i] = col;
    }

    private void burst(float cx, float cy, int col, int n, float speed, float size) {
        for (int i = 0; i < n; i++) {
            float a = rnd.nextFloat() * 6.2832f;
            float v = speed * (0.4f + 0.6f * rnd.nextFloat());
            spawn(cx, cy, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 0.5f + 0.4f * rnd.nextFloat(), size, col);
        }
    }

    private void updateParticles(float dt) {
        float k = Math.max(0f, 1f - 2.5f * dt);
        for (int i = 0; i < PMAX; i++) {
            if (pLife[i] <= 0f) continue;
            pLife[i] -= dt;
            pX[i] += pVX[i] * dt;
            pY[i] += pVY[i] * dt;
            pVX[i] *= k;
            pVY[i] *= k;
        }
    }

    /** Luminosité 0..1 du plateau. Niveau stroboscopique : impulsions lentes, jamais plus de 1 flash/s. */
    private float strobeVis() {
        if (!level.strobe || phase == Phase.COUNTDOWN) return 1f;
        if (g.progress.reducedFx) {
            return 0.575f + 0.425f * (float) Math.sin(strobeT * Math.PI); // fondu doux, 0,5 Hz
        }
        float p = strobeT % STROBE_PERIOD;
        float ramp = 0.06f, on = 0.22f, floor = 0.10f;
        if (p < ramp) return floor + (1f - floor) * (p / ramp);
        if (p < on) return 1f;
        return Math.max(floor, (float) Math.exp(-(p - on) / 0.45f));
    }

    // ---------- Dessin ----------

    @Override
    public void draw(Canvas c, int w, int h) {
        layout(w, h);
        drawWorld(c, w, h);
        drawHud(c, w, h);
        drawTouchControl(c);
        drawControls(c, w, h);
        if (phase == Phase.COUNTDOWN) drawCountdown(c, w, h);
        drawToast(c, w);
    }

    /** Plateau + objets. Utilisé aussi par l'écran de pause (en fond). */
    public void drawWorld(Canvas c, int w, int h) {
        layout(w, h);
        World wd = World.of(level.world);
        c.drawColor(wd.bg);

        float rad = Ui.dp(16);
        Ui.roundRect(c, bl, bt + Ui.dp(4), br, bb + Ui.dp(4), rad, Ui.alpha(wd.dark ? Color.BLACK : Ui.PRIMARY, 60));

        float shx = 0f, shy = 0f;
        if (shakeT > 0f && !g.progress.reducedFx) {
            shx = (float) Math.sin(anim * 70f) * shakeT * Ui.dp(6);
            shy = (float) Math.cos(anim * 83f) * shakeT * Ui.dp(6);
        }
        float px = 1f / s;
        c.save();
        c.translate(bl + shx, bt + shy);
        c.scale(s, s);

        Ui.R.set(0, 0, Level.W, Level.H);
        clip.reset();
        clip.addRoundRect(Ui.R, rad * px, rad * px, Path.Direction.CW);
        c.clipPath(clip);

        Paint bp = Ui.fill(Color.WHITE);
        bp.setShader(wd.boardShader());
        c.drawRect(0, 0, Level.W, Level.H, bp);
        bp.setShader(null);

        Paint gp = Ui.stroke(wd.grid, px * 1.5f);
        for (int gx = 100; gx < 1600; gx += 100) c.drawLine(gx, 0, gx, Level.H, gp);
        for (int gy = 100; gy < 900; gy += 100) c.drawLine(0, gy, Level.W, gy, gp);
        if (level.world == 1) { // reflets de glace
            Paint ip = Ui.stroke(Ui.alpha(Color.WHITE, 45), 46f);
            for (int i = 0; i < 7; i++) c.drawLine(i * 260f, -20f, i * 260f - 360f, Level.H + 20f, ip);
        }

        drawWalls(c, wd, px);
        drawHoles(c, wd, px);
        drawExit(c, px);
        drawStars(c, px);
        drawPickups(c, px);

        for (int i = 0; i < ringLife.length; i++) {
            if (ringLife[i] <= 0f) continue;
            float p = 1f - ringLife[i] / RING_LIFE;
            c.drawCircle(ringX[i], ringY[i], BALL_R + p * BALL_R * 2.6f,
                    Ui.stroke(Ui.alpha(ringColor[i], (int) (220 * (1f - p))), Ui.dp(4) * px));
        }
        for (int i = 0; i < PMAX; i++) {
            if (pLife[i] <= 0f) continue;
            int a = (int) (255 * Math.max(0f, pLife[i] / pMax[i]));
            float hs = pSize[i] / 2f;
            c.drawRect(pX[i] - hs, pY[i] - hs, pX[i] + hs, pY[i] + hs, Ui.fill(Ui.alpha(pCol[i], a)));
        }

        if (fanLeft > 0f) drawWind(c, px);

        // niveau stroboscopique : le plateau s'éteint, la bille reste visible
        float vis = strobeVis();
        if (vis < 0.999f) {
            c.drawRect(0, 0, Level.W, Level.H, Ui.fill(Ui.alpha(Color.rgb(4, 4, 18), (int) ((1f - vis) * 228))));
        }

        drawBall(c, wd, px, level.strobe);

        if (flash > 0f) {
            int a = g.progress.reducedFx ? (int) (40 * flash) : (int) (110 * flash);
            c.drawRect(0, 0, Level.W, Level.H, Ui.fill(Ui.alpha(Ui.DANGER, a)));
        }
        c.restore();

        Ui.R.set(bl, bt, br, bb);
        c.drawRoundRect(Ui.R, rad, rad, Ui.stroke(Ui.alpha(wd.dark ? wd.rim : Ui.PRIMARY, 150), Ui.dp(3)));
    }

    private void drawWalls(Canvas c, World wd, float px) {
        for (int i = 0; i < level.walls.length; i++) {
            if (wallBroken[i]) continue;
            float[] wl = level.walls[i];
            boolean frag = wl[4] == 1f;
            float wx = wl[0], wy = wl[1], ww = wl[2], wh = wl[3];
            if (frag && i == nearIdx) wx += (float) Math.sin(anim * 40f) * 2.5f; // tremble : "secoue-moi"
            c.drawRect(wx + 8, wy + 10, wx + ww + 8, wy + wh + 10, Ui.fill(Ui.alpha(Color.BLACK, 45)));
            c.drawRect(wx, wy, wx + ww, wy + wh, Ui.fill(frag ? FRAG : wd.wall));
            c.drawRect(wx, wy, wx + ww, wy + Math.min(8f, wh), Ui.fill(frag ? FRAG_TOP : wd.wallTop));
            if (frag) {
                Path p = Ui.PATH;
                p.reset();
                boolean horiz = ww > wh;
                int n = 7;
                for (int k = 0; k <= n; k++) {
                    float t = k / (float) n;
                    float off = (k % 2 == 0 ? -1f : 1f) * (horiz ? wh : ww) * 0.28f;
                    float cx = horiz ? wx + ww * t : wx + ww / 2f + off;
                    float cy = horiz ? wy + wh / 2f + off : wy + wh * t;
                    if (k == 0) p.moveTo(cx, cy); else p.lineTo(cx, cy);
                }
                c.drawPath(p, Ui.stroke(Color.rgb(90, 60, 35), 4f));
            }
        }
    }

    private void drawHoles(Canvas c, World wd, float px) {
        for (float[] hl : level.holes) {
            float hx = hl[0], hy = hl[1], hr = hl[2];
            c.drawCircle(hx, hy, hr + 6f, Ui.fill(Ui.alpha(Color.BLACK, 40)));
            c.drawCircle(hx, hy, hr, Ui.fill(Ui.HOLE));
            c.drawCircle(hx, hy, hr * 0.78f, Ui.fill(Color.rgb(16, 19, 32)));
            c.drawCircle(hx, hy, hr * 0.5f, Ui.fill(Color.rgb(6, 8, 16)));
            c.drawCircle(hx, hy, hr - 2f, Ui.stroke(wd.rim, 5f));
        }
    }

    private void drawExit(Canvas c, float px) {
        float er = Level.EXIT_R;
        float pulse = 1f + 0.06f * (float) Math.sin(anim * 4f);
        c.drawCircle(level.exitX, level.exitY, er * pulse, Ui.fill(Ui.alpha(Ui.OK, 70)));
        c.drawCircle(level.exitX, level.exitY, er * 0.8f, Ui.fill(Ui.OK));
        c.drawCircle(level.exitX, level.exitY, er * 0.55f, Ui.stroke(Color.WHITE, Ui.dp(3) * px));
        Ui.text(c, "SORTIE", level.exitX, level.exitY + er + Ui.sp(16) * px, Ui.sp(13) * px, Ui.OK, true);
    }

    private void drawStars(Canvas c, float px) {
        for (int i = 0; i < starGot.length; i++) {
            if (starGot[i]) continue;
            float k = 1f + 0.08f * (float) Math.sin(anim * 3f + i);
            Icons.star(c, starX[i], starY[i], STAR_R * k, Ui.STAR, true);
            Icons.star(c, starX[i], starY[i], STAR_R * k, Color.rgb(200, 130, 0), false);
            int a = (int) (140 + 110 * Math.sin(anim * 5f + i * 2f));
            float tx = starX[i] + STAR_R * 0.95f, ty = starY[i] - STAR_R * 0.95f;
            Paint tp = Ui.stroke(Ui.alpha(Color.WHITE, a), 3f);
            c.drawLine(tx - 9, ty, tx + 9, ty, tp);
            c.drawLine(tx, ty - 9, tx, ty + 9, tp);
        }
    }

    private void drawPickups(Canvas c, float px) {
        for (int i = 0; i < pickGot.length; i++) {
            if (pickGot[i]) continue;
            Pickup pk = level.ptypes[i];
            float cx = level.pickups[i][0], cy = level.pickups[i][1];
            float k = 1f + 0.07f * (float) Math.sin(anim * 3f + i);
            float rr = PICK_R * k;
            boolean myst = pk == Pickup.MYSTERY;
            int col = myst ? Ui.PURPLE : (pk.bonus ? Ui.OK : Ui.DANGER);
            if (pk.bonus) {
                c.drawCircle(cx, cy, rr, Ui.fill(col));
                c.drawCircle(cx, cy, rr, Ui.stroke(Ui.alpha(Color.WHITE, 220), 3f));
            } else { // malus : losange (forme + couleur, pour le daltonisme)
                c.save();
                c.rotate(45f, cx, cy);
                Ui.R.set(cx - rr * 0.82f, cy - rr * 0.82f, cx + rr * 0.82f, cy + rr * 0.82f);
                c.drawRoundRect(Ui.R, 8f, 8f, Ui.fill(col));
                c.drawRoundRect(Ui.R, 8f, 8f, Ui.stroke(Ui.alpha(Color.WHITE, 220), 3f));
                c.restore();
            }
            pk.icon.draw(c, cx, cy, PICK_R * 1.15f, Color.WHITE);
        }
    }

    private void drawWind(Canvas c, float px) {
        float len = 140f, diag = (float) Math.hypot(Level.W, Level.H);
        float ppx = -windDy, ppy = windDx;
        int a = (int) (170 * Math.min(1f, fanLeft / 0.25f));
        Paint p = Ui.stroke(Ui.alpha(Color.rgb(80, 150, 215), a), Ui.dp(3) * px);
        for (int i = 0; i < 13; i++) {
            float t = (anim * 1.6f + i * 0.37f) % 1f;
            float off = (i - 6) * 170f;
            float cx = Level.W / 2f + windDx * (t - 0.5f) * diag * 1.1f + ppx * off;
            float cy = Level.H / 2f + windDy * (t - 0.5f) * diag * 1.1f + ppy * off;
            c.drawLine(cx - windDx * len / 2f, cy - windDy * len / 2f, cx + windDx * len / 2f, cy + windDy * len / 2f, p);
        }
    }

    private void drawBall(Canvas c, World wd, float px, boolean glow) {
        float r = radius();
        float sp = (float) Math.hypot(vx, vy);
        if (sp > 80f && phase == Phase.RUN) { // traînée
            for (int k = trCount - 1; k >= 0; k--) {
                int idx = (trHead - 1 - k + trX.length * 2) % trX.length;
                float f = 1f - k / (float) trX.length;
                c.drawCircle(trX[idx], trY[idx], r * (0.35f + 0.45f * f), Ui.fill(Ui.alpha(Ui.BALL, (int) (70 * f))));
            }
        }
        float h = 0f;
        if (airLeft > 0f) h = (float) Math.sin((1f - airLeft / airTotal) * Math.PI);
        float sr = r * (0.95f - 0.25f * h);
        Ui.R.set(x - sr, y + r * 0.55f, x + sr, y + r * 0.55f + r * 0.45f);
        c.drawOval(Ui.R, Ui.fill(Ui.alpha(Color.BLACK, (int) (70 - 35 * h))));

        float bx = x, by = y - h * r * 1.6f, br2 = r * (1f + 0.3f * h);
        if (glow) c.drawCircle(bx, by, br2 * 1.9f, Ui.fill(Ui.alpha(Ui.BALL, 55)));
        Ui.ball(c, bx, by, br2);

        if (phase == Phase.RUN && tiltMag >= 0.08f) { // flèche de direction
            float m = (float) Math.hypot(tiltX, tiltY);
            float nx = tiltX / m, ny = tiltY / m;
            float sx = bx + nx * br2 * 1.35f, sy = by + ny * br2 * 1.35f;
            float len = r * (1.1f + 2.2f * tiltMag);
            float ex = sx + nx * len, ey = sy + ny * len;
            int col = Ui.alpha(wd.dark ? Color.WHITE : Ui.PRIMARY, 215);
            c.drawLine(sx, sy, ex, ey, Ui.stroke(col, Ui.dp(4) * px));
            float hl = r * 0.7f, qx = -ny, qy = nx;
            Path path = Ui.PATH;
            path.reset();
            path.moveTo(ex + nx * hl, ey + ny * hl);
            path.lineTo(ex + qx * hl * 0.7f, ey + qy * hl * 0.7f);
            path.lineTo(ex - qx * hl * 0.7f, ey - qy * hl * 0.7f);
            path.close();
            c.drawPath(path, Ui.fill(col));
        }
        if (phase == Phase.RUN && nearIdx >= 0) { // indice : "secoue pour casser"
            Icons.shakeAnim(c, x, y - r * 2.9f, r * 2.6f, wd.hud, (float) Math.sin(anim * 14f) * r * 0.12f);
        }
    }

    private void drawHud(Canvas c, int w, int h) {
        World wd = World.of(level.world);
        float top = g.insetTop;
        float mid = top + (bt - top) * 0.42f;

        float sr = Ui.dp(14), step = Ui.dp(36);
        for (int i = 0; i < 3; i++) {
            float sx = bl + Ui.dp(16) + i * step;
            Icons.star(c, sx, mid, sr, i < starCount ? Ui.STAR : Ui.STAR_EMPTY, true);
            if (i < starCount) Icons.star(c, sx, mid, sr, Color.rgb(200, 130, 0), false);
        }

        // effets actifs (bonus en vert, malus en rouge) avec compte à rebours
        float cx0 = bl + Ui.dp(16) + 3 * step + Ui.dp(14);
        for (Pickup pk : Pickup.values()) {
            float t = eff[pk.ordinal()];
            if (t <= 0f || pk.duration <= 0f) continue;
            float cw = Ui.dp(82), chh = Ui.dp(32);
            Ui.roundRect(c, cx0, mid - chh / 2f, cx0 + cw, mid + chh / 2f, chh / 2f, pk.bonus ? Ui.OK : Ui.DANGER);
            pk.icon.draw(c, cx0 + chh * 0.55f, mid, chh * 0.62f, Color.WHITE);
            Ui.text(c, (int) Math.ceil(t) + " s", cx0 + cw - Ui.dp(10), mid + Ui.sp(14) * 0.35f, Ui.sp(14),
                    Color.WHITE, true, Paint.Align.RIGHT, 0f);
            cx0 += cw + Ui.dp(8);
        }

        int secs = Math.max(0, (int) Math.ceil(timeLeft));
        boolean urgent = timeLeft <= 10f && phase == Phase.RUN;
        float size = Ui.sp(32);
        if (urgent && !g.progress.reducedFx) size *= 1f + 0.08f * Math.abs((float) Math.sin(anim * 4f));
        Ui.text(c, secs + " s", br, mid + size * 0.35f, size, urgent ? Ui.DANGER : wd.hud, true, Paint.Align.RIGHT, 0f);

        float by = bt - Ui.dp(16), bh2 = Ui.dp(8);
        Ui.roundRect(c, bl, by, br, by + bh2, bh2 / 2f, Ui.alpha(wd.hud, 40));
        float ratio = Ui.clamp(timeLeft / level.time, 0f, 1f);
        if (ratio > 0f) Ui.roundRect(c, bl, by, bl + bw * ratio, by + bh2, bh2 / 2f, urgent ? Ui.DANGER : wd.accent);

        if (urgent && !g.progress.reducedFx) { // liseré rouge qui respire doucement
            int a = (int) (110 + 90 * Math.sin(anim * 3f));
            Ui.R.set(bl, bt, br, bb);
            c.drawRoundRect(Ui.R, Ui.dp(16), Ui.dp(16), Ui.stroke(Ui.alpha(Ui.DANGER, a), Ui.dp(5)));
        }
    }

    private void drawControls(Canvas c, int w, int h) {
        boolean frag = nearIdx >= 0;
        actionBtn.label = frag ? "Casser" : "Saut";
        if (frag) actionBtn.icon = Icons::hammer; else actionBtn.icon = Icons::jump;
        actionBtn.color = frag ? Ui.ORANGE : Ui.BUTTON;

        if (joyZone) { // Joystick : tous les boutons sous le même pouce
            pauseBtn.set(acl, h * 0.05f, acr, h * 0.20f);
            actionBtn.set(acl, h * 0.25f, acr, h * 0.58f);
            blowBtn.set(acl, h * 0.62f, acr, h * 0.95f);
        } else {       // Inclinaison : Pause + Saut à gauche, Souffle à droite
            pauseBtn.set(acl, h * 0.06f, acr, h * 0.28f);
            actionBtn.set(acl, h * 0.36f, acr, h * 0.94f);
            blowBtn.set(rcl, h * 0.36f, rcr, h * 0.94f);
        }

        boolean run = phase == Phase.RUN;
        actionBtn.enabled = run;
        blowBtn.enabled = run;
        actionBtn.fill = 1f - Ui.clamp(jumpCd / JUMP_COOLDOWN, 0f, 1f);
        blowBtn.fill = 1f - Ui.clamp(fanCd / FAN_COOLDOWN, 0f, 1f);

        pauseBtn.draw(c);
        actionBtn.draw(c);
        blowBtn.draw(c);

        if (run && g.sensors.micAvailable()) { // jauge du micro en direct
            float ml = blowBtn.rect.left + Ui.dp(14), mr = blowBtn.rect.right - Ui.dp(14);
            float my = blowBtn.rect.top + Ui.dp(14), mh = Ui.dp(6);
            Ui.roundRect(c, ml, my, mr, my + mh, mh / 2f, Ui.alpha(Color.WHITE, 70));
            float v = Ui.clamp(g.sensors.micLevel() / (g.sensors.micThreshold() * 1.5f), 0f, 1f);
            if (v > 0f) Ui.roundRect(c, ml, my, ml + (mr - ml) * v, my + mh, mh / 2f, v >= 0.66f ? Ui.STAR : Color.WHITE);
            float tick = ml + (mr - ml) * 0.667f;
            c.drawRect(tick - Ui.dp(1), my - Ui.dp(2), tick + Ui.dp(1), my + mh + Ui.dp(2), Ui.fill(Color.WHITE));
        }
    }

    private void drawCountdown(Canvas c, int w, int h) {
        World wd = World.of(level.world);
        Ui.R.set(bl, bt, br, bb);
        c.drawRoundRect(Ui.R, Ui.dp(16), Ui.dp(16), Ui.fill(Ui.alpha(wd.bg, 175)));

        float cx = (bl + br) / 2f;
        int ci = (int) Math.ceil(Math.max(0f, countdown));
        float frac = countdown - (float) Math.floor(countdown);
        float size = bh * 0.42f * (1f + 0.25f * frac);
        Ui.title(c, String.valueOf(Math.max(1, ci)), cx, bt + bh * 0.32f + size * 0.35f, size, wd.hud, Paint.Align.CENTER, 0f);

        float fs = Math.max(Ui.sp(16), bh * 0.045f);
        float ly = bt + bh * 0.58f;
        Ui.title(c, level.name, cx, ly, fs * 1.4f, wd.hud, Paint.Align.CENTER, bw * 0.9f);
        ly += fs * 1.8f;
        if (level.strobe) {
            Ui.text(c, "ATTENTION : lumières clignotantes (réglable dans Réglages > Effets lumineux).",
                    cx, ly, fs, wd.dark ? Color.WHITE : Ui.DANGER, true, Paint.Align.CENTER, bw * 0.94f);
            ly += fs * 1.5f;
            Ui.text(c, "Le plateau s'éteint : mémorise ce que tu vois !", cx, ly, fs, wd.hud, false, Paint.Align.CENTER, bw * 0.94f);
        } else {
            Ui.text(c, resumeMode ? "Reprise dans un instant" : "Prépare-toi !", cx, ly, fs, wd.hud, true, Paint.Align.CENTER, bw * 0.94f);
        }
        ly += fs * 1.5f;
        String hint;
        if (g.progress.controlMode == ControlMode.TILT) {
            hint = g.progress.calibFlat ? "Garde la tablette à plat : c'est la position neutre."
                    : "Tiens l'appareil comme pour jouer : c'est la position neutre.";
        } else {
            hint = "Pose le pouce dans la zone manette : un joystick apparaît.";
        }
        Ui.text(c, hint, cx, ly, fs, wd.hud, false, Paint.Align.CENTER, bw * 0.94f);
        if (!g.sensors.micAvailable()) {
            ly += fs * 1.5f;
            Ui.text(c, "Micro indisponible : utilise le bouton Souffle.", cx, ly, fs,
                    wd.dark ? Color.WHITE : Ui.DANGER, true, Paint.Align.CENTER, bw * 0.94f);
        }
    }

    private void drawToast(Canvas c, int w) {
        if (toastT <= 0f) return;
        float a = Math.min(1f, toastT / 0.4f);
        float size = Math.max(Ui.sp(20), bh * 0.055f);
        float tw = Ui.measure(toastText, size, true);
        float cx = (bl + br) / 2f, base = bt + bh * 0.12f;
        float padX = Ui.dp(18), padY = Ui.dp(10);
        float l = cx - tw / 2f - padX, r = cx + tw / 2f + padX;
        float t = base - size * 0.85f - padY * 0.4f, b = base + size * 0.30f + padY * 0.4f;
        Ui.roundRect(c, l, t, r, b, (b - t) / 2f, Ui.alpha(toastColor, (int) (235 * a)));
        Ui.text(c, toastText, cx, base, size, Ui.alpha(Color.WHITE, (int) (255 * a)), true);
    }

    // ---------- Manette : affichage ----------

    private void drawTouchControl(Canvas c) {
        if (g.progress.controlMode != ControlMode.JOYSTICK) return;
        World wd = World.of(level.world);
        int col = wd.dark ? Color.WHITE : Ui.PRIMARY;
        float jr = stickRadius();

        Ui.R.set(zl, zt, zr, zb);
        c.drawRoundRect(Ui.R, Ui.dp(20), Ui.dp(20), Ui.fill(Ui.alpha(col, 16)));
        c.drawRoundRect(Ui.R, Ui.dp(20), Ui.dp(20), Ui.stroke(Ui.alpha(col, 70), Ui.dp(2)));

        if (ctlId >= 0) {
            float dx = ctlX - ctlBaseX, dy = ctlY - ctlBaseY;
            float d = (float) Math.hypot(dx, dy);
            if (d > jr) { dx *= jr / d; dy *= jr / d; }
            c.drawCircle(ctlBaseX, ctlBaseY, jr, Ui.fill(Ui.alpha(col, 35)));
            c.drawCircle(ctlBaseX, ctlBaseY, jr, Ui.stroke(Ui.alpha(col, 150), Ui.dp(3)));
            c.drawCircle(ctlBaseX + dx, ctlBaseY + dy, jr * 0.38f, Ui.fill(Ui.alpha(col, 190)));
        } else {
            float cx = (zl + zr) / 2f, cy = (zt + zb) / 2f;
            c.drawCircle(cx, cy, jr, Ui.stroke(Ui.alpha(col, 90), Ui.dp(3)));
            c.drawCircle(cx, cy, jr * 0.38f, Ui.fill(Ui.alpha(col, 70)));
            if (phase == Phase.RUN && runT < 6f) {
                Ui.text(c, "Pose ton pouce ici", cx, cy + jr * 1.45f, Math.max(Ui.sp(14), bh * 0.04f), wd.hud, true,
                        Paint.Align.CENTER, (zr - zl) * 0.9f);
            }
        }
    }

    // ---------- Tactile ----------

    @Override
    public void onDown(int id, float px, float py) {
        boolean hitPause = pauseBtn.contains(px, py);
        boolean hitAct = actionBtn.contains(px, py);
        boolean hitBlow = blowBtn.contains(px, py);
        pauseBtn.onDown(px, py);
        actionBtn.onDown(px, py);
        blowBtn.onDown(px, py);
        if (phase == Phase.RUN) {
            if (hitAct && actionBtn.enabled) onAction();
            if (hitBlow && blowBtn.enabled) tryFan();
        }
        // manette flottante : un doigt posé dans la zone de droite (hors boutons) prend le pilotage
        if (!hitPause && !hitAct && !hitBlow && ctlId < 0 && joyZone
                && g.progress.controlMode == ControlMode.JOYSTICK
                && px >= zl && px <= zr && py >= zt && py <= zb) {
            float jr = stickRadius();
            ctlId = id;
            ctlBaseX = ctlX = Ui.clamp(px, zl + jr, zr - jr);
            ctlBaseY = ctlY = Ui.clamp(py, zt + jr, zb - jr);
        }
    }

    @Override
    public void onMove(int id, float px, float py) {
        if (id == ctlId) { ctlX = px; ctlY = py; return; }
        pauseBtn.onMove(px, py);
        actionBtn.onMove(px, py);
        blowBtn.onMove(px, py);
    }

    @Override
    public void onUp(int id, float px, float py) {
        if (id == ctlId) { ctlId = -1; return; }
        actionBtn.onUp(px, py);
        blowBtn.onUp(px, py);
        if (pauseBtn.onUp(px, py)) g.pauseGame();
    }

    @Override
    public void onCancel() {
        ctlId = -1;
        pauseBtn.onCancel();
        actionBtn.onCancel();
        blowBtn.onCancel();
    }
}
