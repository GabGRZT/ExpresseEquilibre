package com.example.expresseequilibre.game;

import com.example.expresseequilibre.data.Progress;
import com.example.expresseequilibre.ui.Icons;
import com.example.expresseequilibre.ui.Ui;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;


/** Dessine le plateau de jeu et tout ce qu'il contient (en coordonnées logiques, mises à l'échelle de l'écran). */
public final class BoardRenderer {
    private final GameWorld world;
    private final TouchLayout layout;
    private final Progress progress;
    private final Path clip = new Path();

    public BoardRenderer(GameWorld world, TouchLayout layout, Progress progress) {
        this.world = world;
        this.layout = layout;
        this.progress = progress;
    }

    public void draw(Canvas c) {
        Level level = world.level;
        World theme = World.of(level.world);
        Effects fx = world.effects;
        RectF board = layout.board;
        float rad = Ui.dp(16), px = 1f / layout.scale; // px = taille d'un pixel écran en unités logiques

        c.drawColor(theme.bg);
        Ui.roundRect(c, board.left, board.top + Ui.dp(4), board.right, board.bottom + Ui.dp(4), rad,
                Ui.alpha(theme.dark ? Color.BLACK : Ui.PRIMARY, 60));

        float shakeX = 0f, shakeY = 0f;
        if (fx.shake > 0f && !progress.reducedFx) {
            shakeX = (float) Math.sin(fx.time * 70f) * fx.shake * Ui.dp(6);
            shakeY = (float) Math.cos(fx.time * 83f) * fx.shake * Ui.dp(6);
        }
        c.save();
        c.translate(board.left + shakeX, board.top + shakeY);
        c.scale(layout.scale, layout.scale);
        Ui.R.set(0, 0, Level.W, Level.H);
        clip.reset();
        clip.addRoundRect(Ui.R, rad * px, rad * px, Path.Direction.CW);
        c.clipPath(clip);

        drawBackground(c, theme, px);
        drawWalls(c, theme);
        drawHoles(c, theme);
        drawExit(c, px);
        drawStars(c);
        drawPickups(c);
        fx.draw(c, px);
        if (world.fanLeft > 0f) drawWind(c, px);

        float visibility = world.strobeVisibility(); // niveau stroboscopique : le plateau s'éteint, la bille reste visible
        if (visibility < 0.999f) {
            c.drawRect(0, 0, Level.W, Level.H, Ui.fill(Ui.alpha(Color.rgb(4, 4, 18), (int) ((1f - visibility) * 228))));
        }
        drawBall(c, theme, px);
        if (fx.flash > 0f) {
            int alpha = (int) ((progress.reducedFx ? 40 : 110) * fx.flash);
            c.drawRect(0, 0, Level.W, Level.H, Ui.fill(Ui.alpha(Ui.DANGER, alpha)));
        }
        c.restore();

        Ui.outline(c, board, rad, Ui.alpha(theme.dark ? theme.rim : Ui.PRIMARY, 150), Ui.dp(3));
    }

    private void drawBackground(Canvas c, World theme, float px) {
        Paint paint = Ui.fill(Color.WHITE);
        paint.setShader(theme.boardShader());
        c.drawRect(0, 0, Level.W, Level.H, paint);
        paint.setShader(null);

        Paint grid = Ui.stroke(theme.grid, px * 1.5f);
        for (float gx = 100; gx < Level.W; gx += 100) c.drawLine(gx, 0, gx, Level.H, grid);
        for (float gy = 100; gy < Level.H; gy += 100) c.drawLine(0, gy, Level.W, gy, grid);

        if (world.level.world == 1) { // reflets de glace
            Paint shine = Ui.stroke(Ui.alpha(Color.WHITE, 45), 46f);
            for (int i = 0; i < 7; i++) c.drawLine(i * 260f, -20f, i * 260f - 360f, Level.H + 20f, shine);
        }
    }

    private void drawWalls(Canvas c, World theme) {
        Level level = world.level;
        for (int i = 0; i < level.walls.length; i++) {
            if (world.wallBroken[i]) continue;
            Wall wall = level.walls[i];
            float wx = wall.x;
            if (wall.fragile && i == world.nearWall) wx += (float) Math.sin(world.effects.time * 40f) * 2.5f; // tremble : "secoue-moi"
            c.drawRect(wx + 8, wall.y + 10, wx + wall.w + 8, wall.y + wall.h + 10, Ui.fill(Ui.alpha(Color.BLACK, 45)));
            c.drawRect(wx, wall.y, wx + wall.w, wall.y + wall.h, Ui.fill(wall.fragile ? Ui.FRAGILE : theme.wall));
            c.drawRect(wx, wall.y, wx + wall.w, wall.y + Math.min(8f, wall.h), Ui.fill(wall.fragile ? Ui.FRAGILE_TOP : theme.wallTop));
            if (wall.fragile) drawCracks(c, wx, wall);
        }
    }

    /** Fissure en zigzag le long du mur fragile. */
    private void drawCracks(Canvas c, float wx, Wall wall) {
        boolean horizontal = wall.w > wall.h;
        int segments = 7;
        Path p = Ui.PATH;
        p.reset();
        for (int k = 0; k <= segments; k++) {
            float t = k / (float) segments;
            float offset = (k % 2 == 0 ? -1f : 1f) * (horizontal ? wall.h : wall.w) * 0.28f;
            float cx = horizontal ? wx + wall.w * t : wx + wall.w / 2f + offset;
            float cy = horizontal ? wall.y + wall.h / 2f + offset : wall.y + wall.h * t;
            if (k == 0) p.moveTo(cx, cy); else p.lineTo(cx, cy);
        }
        c.drawPath(p, Ui.stroke(Color.rgb(90, 60, 35), 4f));
    }

    private void drawHoles(Canvas c, World theme) {
        for (Hole hole : world.level.holes) {
            c.drawCircle(hole.x, hole.y, hole.r + 6f, Ui.fill(Ui.alpha(Color.BLACK, 40)));
            c.drawCircle(hole.x, hole.y, hole.r, Ui.fill(Ui.HOLE));
            c.drawCircle(hole.x, hole.y, hole.r * 0.78f, Ui.fill(Color.rgb(16, 19, 32)));
            c.drawCircle(hole.x, hole.y, hole.r * 0.5f, Ui.fill(Color.rgb(6, 8, 16)));
            c.drawCircle(hole.x, hole.y, hole.r - 2f, Ui.stroke(theme.rim, 5f));
        }
    }

    private void drawExit(Canvas c, float px) {
        Level level = world.level;
        float er = Level.EXIT_R;
        float pulse = 1f + 0.06f * (float) Math.sin(world.effects.time * 4f);
        c.drawCircle(level.exitX, level.exitY, er * pulse, Ui.fill(Ui.alpha(Ui.OK, 70)));
        c.drawCircle(level.exitX, level.exitY, er * 0.8f, Ui.fill(Ui.OK));
        c.drawCircle(level.exitX, level.exitY, er * 0.55f, Ui.stroke(Color.WHITE, Ui.dp(3) * px));
        Ui.text(c, "SORTIE", level.exitX, level.exitY + er + Ui.sp(16) * px, Ui.sp(13) * px, Ui.OK, true);
    }

    private void drawStars(Canvas c) {
        float time = world.effects.time;
        for (int i = 0; i < world.stars.length; i++) {
            Star star = world.stars[i];
            if (star.collected) continue;
            Icons.rating(c, star.x, star.y, GameWorld.STAR_R * (1f + 0.08f * (float) Math.sin(time * 3f + i)), true);
            // petit scintillement en haut à droite
            int alpha = (int) (140 + 110 * Math.sin(time * 5f + i * 2f));
            float tx = star.x + GameWorld.STAR_R * 0.95f, ty = star.y - GameWorld.STAR_R * 0.95f;
            Paint twinkle = Ui.stroke(Ui.alpha(Color.WHITE, alpha), 3f);
            c.drawLine(tx - 9, ty, tx + 9, ty, twinkle);
            c.drawLine(tx, ty - 9, tx, ty + 9, twinkle);
        }
    }

    private void drawPickups(Canvas c) {
        for (int i = 0; i < world.pickupGot.length; i++) {
            if (world.pickupGot[i]) continue;
            PickupSpot spot = world.level.pickups[i];
            Pickup pickup = spot.type;
            float rr = GameWorld.PICK_R * (1f + 0.07f * (float) Math.sin(world.effects.time * 3f + i));
            int color = pickup == Pickup.MYSTERY ? Ui.PURPLE : (pickup.bonus ? Ui.OK : Ui.DANGER);
            int edge = Ui.alpha(Color.WHITE, 220);
            if (pickup.bonus) { // bonus : rond
                c.drawCircle(spot.x, spot.y, rr, Ui.fill(color));
                c.drawCircle(spot.x, spot.y, rr, Ui.stroke(edge, 3f));
            } else {            // malus : losange (forme + couleur, pour le daltonisme)
                c.save();
                c.rotate(45f, spot.x, spot.y);
                Ui.R.set(spot.x - rr * 0.82f, spot.y - rr * 0.82f, spot.x + rr * 0.82f, spot.y + rr * 0.82f);
                c.drawRoundRect(Ui.R, 8f, 8f, Ui.fill(color));
                c.drawRoundRect(Ui.R, 8f, 8f, Ui.stroke(edge, 3f));
                c.restore();
            }
            pickup.icon.draw(c, spot.x, spot.y, GameWorld.PICK_R * 1.15f, Color.WHITE);
        }
    }

    /** Traits qui défilent dans la direction du vent. */
    private void drawWind(Canvas c, float px) {
        float len = 140f, diag = (float) Math.hypot(Level.W, Level.H);
        float perpX = -world.windDy, perpY = world.windDx;
        int alpha = (int) (170 * Math.min(1f, world.fanLeft / 0.25f));
        Paint paint = Ui.stroke(Ui.alpha(Color.rgb(80, 150, 215), alpha), Ui.dp(3) * px);
        for (int i = 0; i < 13; i++) {
            float t = (world.effects.time * 1.6f + i * 0.37f) % 1f;
            float offset = (i - 6) * 170f;
            float cx = Level.W / 2f + world.windDx * (t - 0.5f) * diag * 1.1f + perpX * offset;
            float cy = Level.H / 2f + world.windDy * (t - 0.5f) * diag * 1.1f + perpY * offset;
            c.drawLine(cx - world.windDx * len / 2f, cy - world.windDy * len / 2f,
                    cx + world.windDx * len / 2f, cy + world.windDy * len / 2f, paint);
        }
    }

    private void drawBall(Canvas c, World theme, float px) {
        float r = world.radius();
        boolean running = world.isRunning();
        if (running && Math.hypot(world.vx, world.vy) > 80f) world.trail.draw(c, r, Ui.BALL);

        float jump = world.jumpHeight();
        float shadowR = r * (0.95f - 0.25f * jump);
        Ui.R.set(world.x - shadowR, world.y + r * 0.55f, world.x + shadowR, world.y + r * 0.55f + r * 0.45f);
        c.drawOval(Ui.R, Ui.fill(Ui.alpha(Color.BLACK, (int) (70 - 35 * jump))));

        float bx = world.x, by = world.y - jump * r * 1.6f, br = r * (1f + 0.3f * jump);
        if (world.level.strobe) c.drawCircle(bx, by, br * 1.9f, Ui.fill(Ui.alpha(Ui.BALL, 55))); // halo : visible dans le noir
        Ui.ball(c, bx, by, br);

        if (running && world.tiltMag >= 0.08f) drawDirectionArrow(c, theme, px, bx, by, br, r);
        if (running && world.nearWall >= 0) { // indice : "secoue pour casser"
            Icons.shakeAnim(c, world.x, world.y - r * 2.9f, r * 2.6f, theme.hud,
                    (float) Math.sin(world.effects.time * 14f) * r * 0.12f);
        }
    }

    /** Flèche devant la bille : direction et force de l'intention du joueur. */
    private void drawDirectionArrow(Canvas c, World theme, float px, float bx, float by, float ballR, float r) {
        float m = (float) Math.hypot(world.tiltX, world.tiltY);
        float nx = world.tiltX / m, ny = world.tiltY / m;
        float sx = bx + nx * ballR * 1.35f, sy = by + ny * ballR * 1.35f;
        float len = r * (1.1f + 2.2f * world.tiltMag);
        float ex = sx + nx * len, ey = sy + ny * len;
        int color = Ui.alpha(theme.dark ? Color.WHITE : Ui.PRIMARY, 215);
        c.drawLine(sx, sy, ex, ey, Ui.stroke(color, Ui.dp(4) * px));

        float head = r * 0.7f, qx = -ny, qy = nx;
        Path path = Ui.PATH;
        path.reset();
        path.moveTo(ex + nx * head, ey + ny * head);
        path.lineTo(ex + qx * head * 0.7f, ey + qy * head * 0.7f);
        path.lineTo(ex - qx * head * 0.7f, ey - qy * head * 0.7f);
        path.close();
        c.drawPath(path, Ui.fill(color));
    }
}
