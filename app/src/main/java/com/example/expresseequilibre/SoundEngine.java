package com.example.expresseequilibre;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Moteur audio : les sons et musiques sont synthétisés au premier lancement (en arrière-plan),
 * écrits dans le cache de l'appli, puis joués avec SoundPool (effets) et MediaPlayer (musique).
 */
final class SoundEngine {
    private static final String DIR = "snd1";
    private static final int THEMES = 4;

    private final Context ctx;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SoundPool pool;
    private final int[] sfxId = new int[Sfx.values().length];
    private final Set<Integer> loaded = Collections.synchronizedSet(new HashSet<Integer>());
    private final File[] musicFile = new File[THEMES];
    private volatile int readyMask;
    private volatile boolean released;

    private MediaPlayer player;
    private int playingTheme = -1, wantedTheme = -1;
    private float wantedVol = 0.5f;
    private boolean muted, appPaused;

    SoundEngine(Context context) {
        ctx = context.getApplicationContext();
        pool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((sp, id, status) -> { if (status == 0) loaded.add(id); });
        new Thread(this::generate, "sound-gen").start();
    }

    private void generate() {
        try {
            File dir = new File(ctx.getCacheDir(), DIR);
            dir.mkdirs();
            for (Sfx s : Sfx.values()) {
                if (released) return;
                File f = new File(dir, "sfx_" + s.name() + ".wav");
                if (!f.exists() || f.length() == 0) writeWav(f, SoundSynth.sfx(s));
                sfxId[s.ordinal()] = pool.load(f.getPath(), 1);
            }
            for (int i = 0; i < THEMES; i++) {
                if (released) return;
                File f = new File(dir, "music_" + i + ".wav");
                if (!f.exists() || f.length() == 0) writeWav(f, SoundSynth.music(i));
                musicFile[i] = f;
                readyMask |= 1 << i;
                main.post(this::applyMusic);
            }
        } catch (Exception ignored) {
        }
    }

    private static void writeWav(File f, short[] pcm) throws Exception {
        int dataLen = pcm.length * 2;
        ByteBuffer bb = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes("US-ASCII")).putInt(36 + dataLen).put("WAVE".getBytes("US-ASCII"));
        bb.put("fmt ".getBytes("US-ASCII")).putInt(16).putShort((short) 1).putShort((short) 1)
                .putInt(SoundSynth.RATE).putInt(SoundSynth.RATE * 2).putShort((short) 2).putShort((short) 16);
        bb.put("data".getBytes("US-ASCII")).putInt(dataLen);
        for (short s : pcm) bb.putShort(s);
        File tmp = new File(f.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(bb.array()); }
        tmp.renameTo(f);
    }

    // ---------- Effets ----------

    void play(Sfx s, float rate) {
        if (muted || released) return;
        int id = sfxId[s.ordinal()];
        if (id == 0 || !loaded.contains(id)) return;
        float v = s == Sfx.TICK ? 0.35f : 0.8f;
        pool.play(id, v, v, 1, 0, Math.max(0.5f, Math.min(2f, rate)));
    }

    // ---------- Musique ----------

    /** theme : 0..3, ou -1 pour le silence. */
    void setMusic(int theme, float volume) {
        wantedTheme = theme;
        wantedVol = volume;
        applyMusic();
    }

    void setMuted(boolean m) {
        muted = m;
        applyMusic();
    }

    void onAppPause() {
        appPaused = true;
        applyMusic();
    }

    void onAppResume() {
        appPaused = false;
        applyMusic();
    }

    private void applyMusic() {
        if (released) return;
        if (wantedTheme < 0) { stopPlayer(); return; }
        boolean ok = !muted && !appPaused && ((readyMask >> wantedTheme) & 1) == 1;
        if (!ok) {
            if (player != null) {
                try { if (player.isPlaying()) player.pause(); } catch (Exception ignored) { }
            }
            return;
        }
        if (player != null && playingTheme == wantedTheme) {
            try {
                player.setVolume(wantedVol, wantedVol);
                if (!player.isPlaying()) player.start();
            } catch (Exception e) {
                stopPlayer();
            }
            return;
        }
        stopPlayer();
        try {
            MediaPlayer mp = new MediaPlayer();
            player = mp;
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setDataSource(musicFile[wantedTheme].getPath());
            mp.setLooping(true);
            mp.prepare();
            mp.setVolume(wantedVol, wantedVol);
            mp.start();
            playingTheme = wantedTheme;
        } catch (Exception e) {
            stopPlayer();
        }
    }

    private void stopPlayer() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            player.release();
            player = null;
        }
        playingTheme = -1;
    }

    void release() {
        released = true;
        stopPlayer();
        pool.release();
    }
}