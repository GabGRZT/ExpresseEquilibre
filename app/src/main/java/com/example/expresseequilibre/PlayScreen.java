package com.example.expresseequilibre;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ToneGenerator;

/** Écran de jeu (paysage) : plateau large au centre, Saut à gauche, Souffle à droite. */
final class PlayScreen extends Screen {

    private enum Phase { COUNTDOWN, RUN }

    // ---------- Réglages de gameplay (vitesses en "hauteurs de plateau" par seconde) ----------
    private static final float GAME_TIME = 60f;
    private static final float COUNTDOWN_TIME = 3f;
    private static final float JUMP_AIR = 0.9f;
    private static final float JUMP_COOLDOWN = 1.6f;
    private static final float FAN_TIME = 1.0f;
    private static final float FAN_COOLDOWN = 3.5f;
    private static final float FALL_PENALTY = 3f;
    private static final float TILT_ACC = 2.0f;
    private static final float FRICTION = 2.5f;
    private static final float MAX_SPEED = 0.9f;
    private static final float FAN_ACC = 1.8f;
    private static final float BOUNCE = 0.35f;

    // ---------- Niveau : x en fraction de la largeur, y en fraction de la hauteur ----------
    // murs : gauche, haut, droite, bas
    private static final float[][] WALLS = {
            {0.300f, 0.00f, 0.325f, 0.68f},   // passage en bas (bouché par le trou 1)
            {0.620f, 0.32f, 0.645f, 1.00f}};  // passage en haut (bouché par le trou 2)
    // trous : x, y, rayon (en hauteurs), direction de réapparition (dx, dy)
    private static final float[][] HOLES = {
            {0.3125f, 0.84f, 0.15f, -1f, 0f},
            {0.6325f, 0.16f, 0.15f, -1f, 0f},
            {0.470f, 0.50f, 0.09f, 0f, 1f}};
    private static final float[][] STARS = {{0.10f, 0.62f}, {0.45f, 0.30f}, {0.78f, 0.28f}};
    private static final float START_X = 0.07f, START_Y = 0.20f;
    private static final float EXIT_X = 0.90f, EXIT_Y = 0.74f, EXIT_R = 0.09f;

    // ---------- Boutons ----------
    private final UiButton pauseBtn = new UiButton("Pause", Ui.SLATE, Icons::pause);
    private final UiButton jumpBtn = new UiButton("Saut", Ui.BUTTON, Icons::jump);
    private final UiButton blowBtn = new UiButton("Souffle", Ui.TEAL, Icons::wind);

    // ---------- État ----------
    private Phase phase = Phase.COUNTDOWN;
    private float countdown = COUNTDOWN_TIME;
    private int lastCi;
    private boolean resumeMode;
    private float timeLeft = GAME_TIME;
    private final boolean[] got = new boolean[STARS.length];
    private int starCount;

    private float x, y, vx, vy;
    private float airLeft, jumpCd, fanLeft, fanCd;
    private float tiltX, tiltY, tiltMag;
    private float windDx = 1f, windDy;
    private float anim, flash;

    private String toastText = "";
    private int toastColor = Ui.OK;
    private float toastT;

    private static final float RING_LIFE = 0.5f;
    private final float[] ringX = new float[6], ringY = new float[6], ringLife = new float[6];
    private final int[] ringColor = new int[6];
    private int ringNext;

    // ---------- Géométrie (pixels) ----------
    private boolean laidOut;
    private long layoutKey;
    private float bl, bt, br, bb, bw, bh, u, ballR, starR;
    private float lcl, lcr, rcl, rcr; // colonnes de boutons gauche / droite
    private final float[][] wallPx = new float[WALLS.length][4];
    private final float[][] holePx = new float[HOLES.length][3];
    private final float[][] respawnPx = new float[HOLES.length][2];
    private final float[][] starPx = new float[STARS.length][2];
    private float exitX, exitY, exitR;
    private final Path clip = new Path();

    PlayScreen(GameView g) {
        super(g);
        jumpBtn.fillVertical = true;
        blowBtn.fillVertical = true;
    }

    int starCount() { return starCount; }

    float timeLeft() { return timeLeft; }

    // ---------- Démarrage / reprise ----------

    void reset() {
        timeLeft = GAME_TIME;
        starCount = 0;
        for (int i = 0; i < got.length; i++) got[i] = false;
        airLeft = jumpCd = fanLeft = fanCd = 0f;
        flash = 0f;
        toastT = 0f;
        for (int i = 0; i < ringLife.length; i++) ringLife[i] = 0f;
        if (laidOut) placeBallAtStart();
        beginCountdown(false);
    }

    void beginCountdown(boolean resume) {
        phase = Phase.COUNTDOWN;
        countdown = COUNTDOWN_TIME;
        lastCi = 0;
        resumeMode = resume;
        vx = vy = 0f;
    }

    private void placeBallAtStart() {
        x = bl + START_X * bw;
        y = bt + START_Y * bh;
        vx = vy = 0f;
    }

    // ---------- Mise en page ----------

    private void layout(int w, int h) {
        int il = g.insetLeft, ir = g.insetRight, it = g.insetTop;
        long key = ((((long) w * 31 + h) * 31 + il) * 31 + ir) * 31 + it;
        if (laidOut && key == layoutKey) return;
        layoutKey = key;

        float fx = 0f, fy = 0f;
        if (laidOut) { fx = (x - bl) / bw; fy = (y - bt) / bh; }

        float margin = Ui.dp(10);
        float colW = Math.max(Ui.dp(70), (w - il - ir) * 0.12f);
        lcl = il + margin;
        lcr = lcl + colW;
        rcr = w - ir - margin;
        rcl = rcr - colW;

        bl = lcr + margin;
        br = rcl - margin;
        bt = Math.max(h * 0.15f, it + h * 0.10f);
        bb = h * 0.96f;
        bw = br - bl;
        bh = bb - bt;

        // Fixe la taille de référence u au minimum des deux dimensions
        // pour préserver le ratio 1:1 du plateau sans l'étirer ni l'écraser
        u = Math.min(bw, bh);
        ballR = u * 0.045f;
        starR = u * 0.04f;

        for (int i = 0; i < WALLS.length; i++) {
            wallPx[i][0] = bl + WALLS[i][0] * bw;
            wallPx[i][1] = bt + WALLS[i][1] * bh;
            wallPx[i][2] = bl + WALLS[i][2] * bw;
            wallPx[i][3] = bt + WALLS[i][3] * bh;
        }
        for (int i = 0; i < HOLES.length; i++) {
            holePx[i][0] = bl + HOLES[i][0] * bw;
            holePx[i][1] = bt + HOLES[i][1] * bh;
            holePx[i][2] = HOLES[i][2] * u;
            float dist = holePx[i][2] + ballR * 2.2f;
            respawnPx[i][0] = holePx[i][0] + HOLES[i][3] * dist;
            respawnPx[i][1] = holePx[i][1] + HOLES[i][4] * dist;
        }
        for (int i = 0; i < STARS.length; i++) {
            starPx[i][0] = bl + STARS[i][0] * bw;
            starPx[i][1] = bt + STARS[i][1] * bh;
        }
        exitX = bl + EXIT_X * bw;
        exitY = bt + EXIT_Y * bh;
        exitR = EXIT_R * u;

        if (laidOut) { x = bl + fx * bw; y = bt + fy * bh; }
        else { placeBallAtStart(); laidOut = true; }
    }

    // ---------- Mise à jour ----------

    @Override
    void update(float dt) {
        int w = g.getWidth(), h = g.getHeight();
        if (w <= 0 || h <= 0) return;
        layout(w, h);

        anim += dt;
        flash = Math.max(0f, flash - dt * 2.5f);
        toastT = Math.max(0f, toastT - dt);
        for (int i = 0; i < ringLife.length; i++) ringLife[i] = Math.max(0f, ringLife[i] - dt);

        g.sensors.update(dt);
        boolean shake = g.sensors.consumeShake();
        boolean blow = g.sensors.consumeBlow();

        if (phase == Phase.COUNTDOWN) { updateCountdown(dt); return; }

        if (shake) tryJump();
        if (blow) tryFan();

        jumpCd = Math.max(0f, jumpCd - dt);
        fanCd = Math.max(0f, fanCd - dt);
        if (airLeft > 0f) airLeft = Math.max(0f, airLeft - dt);

        timeLeft -= dt;
        if (timeLeft <= 0f) { timeLeft = 0f; finish(false); return; }

        tiltX = g.sensors.tiltX();
        tiltY = g.sensors.tiltY();
        tiltMag = Math.min(1f, (float) Math.hypot(tiltX, tiltY));
        float acc = u * TILT_ACC;
        vx += tiltX * acc * dt;
        vy += tiltY * acc * dt;

        if (fanLeft > 0f) {
            fanLeft = Math.max(0f, fanLeft - dt);
            updateWindDir();
            vx += windDx * u * FAN_ACC * dt;
            vy += windDy * u * FAN_ACC * dt;
        }

        float damp = Math.max(0f, 1f - FRICTION * dt);
        vx *= damp;
        vy *= damp;
        float sp = (float) Math.hypot(vx, vy), max = u * MAX_SPEED;
        if (sp > max) { vx *= max / sp; vy *= max / sp; }

        x += vx * dt;
        y += vy * dt;
        collideBoard();
        for (float[] wall : wallPx) collideRect(wall[0], wall[1], wall[2], wall[3]);

        // Trous : sans effet quand la bille est en l'air.
        if (airLeft <= 0f) {
            for (int i = 0; i < holePx.length; i++) {
                float dx = x - holePx[i][0], dy = y - holePx[i][1];
                if (dx * dx + dy * dy < holePx[i][2] * holePx[i][2]) {
                    fall(i);
                    if (timeLeft <= 0f) { timeLeft = 0f; finish(false); return; }
                    break;
                }
            }
        }

        for (int i = 0; i < starPx.length; i++) {
            if (got[i]) continue;
            float dx = x - starPx[i][0], dy = y - starPx[i][1];
            float rr = ballR + starR;
            if (dx * dx + dy * dy < rr * rr) {
                got[i] = true;
                starCount++;
                addRing(starPx[i][0], starPx[i][1], Ui.STAR);
                toast("Étoile " + starCount + "/3", Ui.OK);
                g.feedback.vibrate(30);
                g.feedback.tone(ToneGenerator.TONE_PROP_ACK, 120);
            }
        }

        float ex = x - exitX, ey = y - exitY;
        if (ex * ex + ey * ey < exitR * exitR) finish(true);
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
            g.sensors.calibrate();       // position tenue = position neutre
            toast("GO !", Ui.OK);
            g.feedback.vibrate(40);
            g.feedback.tone(ToneGenerator.TONE_PROP_BEEP2, 200);
        }
    }

    private void tryJump() {
        if (phase != Phase.RUN || airLeft > 0f || jumpCd > 0f) return;
        airLeft = JUMP_AIR;
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

    /** Le vent pousse dans la direction où le joueur incline ; au repos, vers la sortie. */
    private void updateWindDir() {
        float m = (float) Math.hypot(tiltX, tiltY);
        if (m > 0.25f) { windDx = tiltX / m; windDy = tiltY / m; return; }
        float dx = exitX - x, dy = exitY - y;
        float d = (float) Math.max(1e-3, Math.hypot(dx, dy));
        windDx = dx / d;
        windDy = dy / d;
    }

    private void fall(int i) {
        timeLeft -= FALL_PENALTY;
        x = respawnPx[i][0];
        y = respawnPx[i][1];
        vx = vy = 0f;
        airLeft = 0f;
        jumpCd = 0f;
        flash = 1f;
        toast("Dans le trou !  -3 s", Ui.DANGER);
        g.feedback.vibrate(90);
        g.feedback.tone(ToneGenerator.TONE_PROP_NACK, 200);
    }

    private void finish(boolean win) {
        int score = starCount * 100 + (win ? 200 + (int) (timeLeft * 5f) : 0);
        g.feedback.vibrate(win ? 200 : 120);
        g.feedback.tone(win ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_NACK, 300);
        g.finishGame(win, starCount, score, (int) timeLeft);
    }

    private void collideBoard() {
        float minX = bl + ballR, maxX = br - ballR, minY = bt + ballR, maxY = bb - ballR;
        if (x < minX) { x = minX; vx = Math.abs(vx) * BOUNCE; }
        else if (x > maxX) { x = maxX; vx = -Math.abs(vx) * BOUNCE; }
        if (y < minY) { y = minY; vy = Math.abs(vy) * BOUNCE; }
        else if (y > maxY) { y = maxY; vy = -Math.abs(vy) * BOUNCE; }
    }

    /** Collision cercle / rectangle avec rebond amorti. */
    private void collideRect(float l, float t, float r, float b) {
        float cx = Math.max(l, Math.min(x, r));
        float cy = Math.max(t, Math.min(y, b));
        float dx = x - cx, dy = y - cy;
        float d2 = dx * dx + dy * dy;
        if (d2 >= ballR * ballR) return;

        float nx, ny;
        if (d2 > 1e-4f) {
            float d = (float) Math.sqrt(d2);
            nx = dx / d;
            ny = dy / d;
            x = cx + nx * ballR;
            y = cy + ny * ballR;
        } else {
            float pl = x - l, pr = r - x, pt = y - t, pb = b - y;
            float m = Math.min(Math.min(pl, pr), Math.min(pt, pb));
            if (m == pl) { nx = -1f; ny = 0f; x = l - ballR; }
            else if (m == pr) { nx = 1f; ny = 0f; x = r + ballR; }
            else if (m == pt) { nx = 0f; ny = -1f; y = t - ballR; }
            else { nx = 0f; ny = 1f; y = b + ballR; }
        }
        float vn = vx * nx + vy * ny;
        if (vn < 0f) {
            vx -= (1f + BOUNCE) * vn * nx;
            vy -= (1f + BOUNCE) * vn * ny;
        }
    }

    private void toast(String s, int color) {
        toastText = s;
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

    // ---------- Dessin ----------

    @Override
    void draw(Canvas c, int w, int h) {
        layout(w, h);
        drawWorld(c, w, h);
        drawHud(c, w, h);
        drawControls(c, w, h);
        if (phase == Phase.COUNTDOWN) drawCountdown(c, w, h);
        drawToast(c, w);
    }

    /** Plateau + objets. Utilisé aussi par l'écran de pause (en fond). */
    void drawWorld(Canvas c, int w, int h) {
        layout(w, h);
        c.drawColor(Ui.BG);

        float rad = Ui.dp(16);
        Ui.roundRect(c, bl, bt + Ui.dp(4), br, bb + Ui.dp(4), rad, Ui.alpha(Ui.PRIMARY, 60));

        Ui.R.set(bl, bt, br, bb);
        clip.reset();
        clip.addRoundRect(Ui.R, rad, rad, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        c.drawRect(bl, bt, br, bb, Ui.fill(Ui.BOARD));

        for (float[] wl : wallPx) c.drawRect(wl[0], wl[1], wl[2], wl[3], Ui.fill(Ui.WALL));

        for (float[] hl : holePx) {
            c.drawCircle(hl[0], hl[1], hl[2], Ui.fill(Ui.HOLE));
            c.drawCircle(hl[0], hl[1], hl[2] - Ui.dp(1.5f), Ui.stroke(Color.rgb(70, 82, 110), Ui.dp(3)));
            c.drawCircle(hl[0], hl[1], hl[2] * 0.68f, Ui.fill(Color.rgb(12, 15, 26)));
        }

        float pulse = 1f + 0.06f * (float) Math.sin(anim * 4f);
        c.drawCircle(exitX, exitY, exitR * pulse, Ui.fill(Ui.alpha(Ui.OK, 70)));
        c.drawCircle(exitX, exitY, exitR * 0.8f, Ui.fill(Ui.OK));
        c.drawCircle(exitX, exitY, exitR * 0.55f, Ui.stroke(Color.WHITE, Ui.dp(3)));
        Ui.text(c, "SORTIE", exitX, exitY + exitR + Ui.sp(16), Ui.sp(13), Ui.OK, true);

        for (int i = 0; i < starPx.length; i++) {
            if (got[i]) continue;
            float k = 1f + 0.08f * (float) Math.sin(anim * 3f + i);
            Icons.star(c, starPx[i][0], starPx[i][1], starR * k, Ui.STAR, true);
            Icons.star(c, starPx[i][0], starPx[i][1], starR * k, Color.rgb(200, 130, 0), false);
        }

        for (int i = 0; i < ringLife.length; i++) {
            if (ringLife[i] <= 0f) continue;
            float p = 1f - ringLife[i] / RING_LIFE;
            c.drawCircle(ringX[i], ringY[i], ballR + p * ballR * 2.6f,
                    Ui.stroke(Ui.alpha(ringColor[i], (int) (220 * (1f - p))), Ui.dp(4)));
        }

        if (fanLeft > 0f) drawWind(c);
        drawBall(c);

        if (flash > 0f) c.drawRect(bl, bt, br, bb, Ui.fill(Ui.alpha(Ui.DANGER, (int) (110 * flash))));
        c.restore();

        Ui.R.set(bl, bt, br, bb);
        c.drawRoundRect(Ui.R, rad, rad, Ui.stroke(Ui.alpha(Ui.PRIMARY, 140), Ui.dp(3)));
    }

    private void drawWind(Canvas c) {
        float len = u * 0.16f;
        float diag = (float) Math.hypot(bw, bh);
        float px = -windDy, py = windDx;
        int a = (int) (170 * Math.min(1f, fanLeft / 0.25f));
        Paint p = Ui.stroke(Ui.alpha(Color.rgb(80, 150, 215), a), Ui.dp(3));
        for (int i = 0; i < 13; i++) {
            float t = (anim * 1.6f + i * 0.37f) % 1f;
            float off = (i - 6) * u * 0.2f;
            float cx = bl + bw / 2f + windDx * (t - 0.5f) * diag * 1.1f + px * off;
            float cy = bt + bh / 2f + windDy * (t - 0.5f) * diag * 1.1f + py * off;
            c.drawLine(cx - windDx * len / 2f, cy - windDy * len / 2f,
                    cx + windDx * len / 2f, cy + windDy * len / 2f, p);
        }
    }

    private void drawBall(Canvas c) {
        float h = 0f;
        if (airLeft > 0f) {
            float p = 1f - airLeft / JUMP_AIR;
            h = (float) Math.sin(p * Math.PI);
        }
        float sr = ballR * (0.95f - 0.25f * h);
        Ui.R.set(x - sr, y + ballR * 0.55f, x + sr, y + ballR * 0.55f + ballR * 0.45f);
        c.drawOval(Ui.R, Ui.fill(Ui.alpha(Color.BLACK, (int) (70 - 35 * h))));

        float bx = x, by = y - h * ballR * 1.6f, br2 = ballR * (1f + 0.3f * h);
        Ui.ball(c, bx, by, br2);

        // flèche de direction détectée
        if (phase == Phase.RUN && tiltMag >= 0.08f) {
            float m = (float) Math.hypot(tiltX, tiltY);
            float nx = tiltX / m, ny = tiltY / m;
            float sx = bx + nx * br2 * 1.35f, sy = by + ny * br2 * 1.35f;
            float len = ballR * (1.1f + 2.2f * tiltMag);
            float ex = sx + nx * len, ey = sy + ny * len;
            int col = Ui.alpha(Ui.PRIMARY, 215);
            c.drawLine(sx, sy, ex, ey, Ui.stroke(col, Ui.dp(4)));
            float hl = ballR * 0.7f, px = -ny, py = nx;
            Path path = Ui.PATH;
            path.reset();
            path.moveTo(ex + nx * hl, ey + ny * hl);
            path.lineTo(ex + px * hl * 0.7f, ey + py * hl * 0.7f);
            path.lineTo(ex - px * hl * 0.7f, ey - py * hl * 0.7f);
            path.close();
            c.drawPath(path, Ui.fill(col));
        }
    }

    private void drawHud(Canvas c, int w, int h) {
        float top = g.insetTop;
        float mid = top + (bt - top) * 0.42f;

        float sr = Ui.dp(14), step = Ui.dp(36);
        for (int i = 0; i < 3; i++) {
            float sx = bl + Ui.dp(16) + i * step;
            Icons.star(c, sx, mid, sr, i < starCount ? Ui.STAR : Ui.STAR_EMPTY, true);
            if (i < starCount) Icons.star(c, sx, mid, sr, Color.rgb(200, 130, 0), false);
        }

        int secs = Math.max(0, (int) Math.ceil(timeLeft));
        boolean urgent = timeLeft <= 10f && phase == Phase.RUN;
        float size = Ui.sp(32) * (urgent ? 1f + 0.08f * Math.abs((float) Math.sin(anim * 6f)) : 1f);
        Ui.text(c, secs + " s", br, mid + size * 0.35f, size, urgent ? Ui.DANGER : Ui.PRIMARY, true, Paint.Align.RIGHT, 0f);

        float by = bt - Ui.dp(16), bh2 = Ui.dp(8);
        Ui.roundRect(c, bl, by, br, by + bh2, bh2 / 2f, Ui.alpha(Ui.PRIMARY, 40));
        float ratio = Ui.clamp(timeLeft / GAME_TIME, 0f, 1f);
        if (ratio > 0f) Ui.roundRect(c, bl, by, bl + bw * ratio, by + bh2, bh2 / 2f, urgent ? Ui.DANGER : Ui.BUTTON);
    }

    private void drawControls(Canvas c, int w, int h) {
        // Colonne gauche : Pause (haut, rarement utilisée) + Saut (grand, sous le pouce gauche).
        pauseBtn.set(lcl, h * 0.06f, lcr, h * 0.28f);
        jumpBtn.set(lcl, h * 0.36f, lcr, h * 0.94f);
        // Colonne droite : Souffle (sous le pouce droit).
        blowBtn.set(rcl, h * 0.36f, rcr, h * 0.94f);

        boolean run = phase == Phase.RUN;
        jumpBtn.enabled = run;
        blowBtn.enabled = run;
        jumpBtn.fill = 1f - Ui.clamp(jumpCd / JUMP_COOLDOWN, 0f, 1f);
        blowBtn.fill = 1f - Ui.clamp(fanCd / FAN_COOLDOWN, 0f, 1f);

        pauseBtn.draw(c);
        jumpBtn.draw(c);
        blowBtn.draw(c);

        // Jauge de souffle en direct : le joueur voit ce que le micro "entend".
        if (run && g.sensors.micAvailable()) {
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
        Ui.R.set(bl, bt, br, bb);
        c.drawRoundRect(Ui.R, Ui.dp(16), Ui.dp(16), Ui.fill(Ui.alpha(Ui.BG, 175)));

        float cx = (bl + br) / 2f;
        int ci = (int) Math.ceil(Math.max(0f, countdown));
        float frac = countdown - (float) Math.floor(countdown);
        float size = bh * 0.5f * (1f + 0.25f * frac);
        Ui.text(c, String.valueOf(Math.max(1, ci)), cx, bt + bh * 0.36f + size * 0.35f, size, Ui.PRIMARY, true);

        float fs = Math.max(Ui.sp(17), bh * 0.05f);
        float ly = bt + bh * 0.66f;
        Ui.text(c, resumeMode ? "Reprise dans un instant" : "Prépare-toi !", cx, ly, fs * 1.25f, Ui.PRIMARY, true, Paint.Align.CENTER, bw * 0.9f);
        Ui.text(c, "Tiens le téléphone comme pour jouer : cette position devient la position neutre.",
                cx, ly + fs * 1.8f, fs, Ui.TEXT, false, Paint.Align.CENTER, bw * 0.92f);
        if (!g.sensors.micAvailable()) {
            Ui.text(c, "Micro indisponible : utilise le bouton Souffle.", cx, ly + fs * 3.2f, fs, Ui.DANGER, true, Paint.Align.CENTER, bw * 0.92f);
        }
    }

    private void drawToast(Canvas c, int w) {
        if (toastT <= 0f) return;
        float a = Math.min(1f, toastT / 0.4f);
        float size = Math.max(Ui.sp(20), bh * 0.06f);
        float tw = Ui.measure(toastText, size, true);
        float cx = (bl + br) / 2f, base = bt + bh * 0.12f;
        float padX = Ui.dp(18), padY = Ui.dp(10);
        float l = cx - tw / 2f - padX, r = cx + tw / 2f + padX;
        float t = base - size * 0.85f - padY * 0.4f, b = base + size * 0.30f + padY * 0.4f;
        Ui.roundRect(c, l, t, r, b, (b - t) / 2f, Ui.alpha(toastColor, (int) (235 * a)));
        Ui.text(c, toastText, cx, base, size, Ui.alpha(Color.WHITE, (int) (255 * a)), true);
    }

    // ---------- Tactile ----------

    @Override
    void onDown(float px, float py) {
        pauseBtn.onDown(px, py);
        jumpBtn.onDown(px, py);
        blowBtn.onDown(px, py);
        // Actions de jeu dès l'appui (latence minimale) ; Pause au relâchement (anti-erreur).
        if (phase == Phase.RUN) {
            if (jumpBtn.pressed) tryJump();
            if (blowBtn.pressed) tryFan();
        }
    }

    @Override
    void onMove(float px, float py) {
        pauseBtn.onMove(px, py);
        jumpBtn.onMove(px, py);
        blowBtn.onMove(px, py);
    }

    @Override
    void onUp(float px, float py) {
        jumpBtn.onUp(px, py);
        blowBtn.onUp(px, py);
        if (pauseBtn.onUp(px, py)) g.pauseGame();
    }

    @Override
    void onCancel() {
        pauseBtn.onCancel();
        jumpBtn.onCancel();
        blowBtn.onCancel();
    }
}