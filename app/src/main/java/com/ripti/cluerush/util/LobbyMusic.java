package com.ripti.cluerush.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import com.ripti.cluerush.R;


public class LobbyMusic {

    private static final float VOLUME = 0.5f;

    private final Context context;
    private MediaPlayer player;

    public LobbyMusic(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        try {
            if (player == null) {
                player = MediaPlayer.create(context, R.raw.lobby_loop);
                if (player == null) {
                    return;
                }
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                player.setLooping(true);
                player.setVolume(VOLUME, VOLUME);
            }
            if (!player.isPlaying()) {
                player.start();
            }
        } catch (Exception ignored) {
            // Music is optional; never crash the lobby because of it.
        }
    }

    public void pause() {
        try {
            if (player != null && player.isPlaying()) {
                player.pause();
            }
        } catch (Exception ignored) {
        }
    }

    public void release() {
        try {
            if (player != null) {
                player.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
    }
}