package com.alessandro.falldetector;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import java.io.IOException;

public class RingtonePlayer implements MediaPlayer.OnPreparedListener {
    private static final String TAG = RingtonePlayer.class.getName();

    private MediaPlayer audioPlayer;
    private boolean isPrepared = false;
    private boolean isPlayed = false;

    public RingtonePlayer(Context context, Uri uri) {
        audioPlayer = new MediaPlayer();
        try {
            audioPlayer.setDataSource(context, uri);
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            Log.e(getClass().getName(), e.getMessage());
        }
        audioPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        audioPlayer.setLooping(true);
        audioPlayer.setOnPreparedListener(this);
        audioPlayer.prepareAsync();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        if (isPlayed) {
            audioPlayer.start();
            Log.d(TAG, "[Ringtone starts]");
        }
    }

    /* Ringtone can only be played once. If you need to play it again, create a new one. */
    public void play() {
        if (audioPlayer.isPlaying()) {
            return;
        }

        if (isPrepared) {
            audioPlayer.start();
            Log.d(TAG, "[Ringtone starts]");
        } else {
            isPlayed = true;
        }
    }

    public void stop() {
        /* We don't need to know if the player is playing.
         * In some circumstances the player has to be stopped before getting prepared. */
        if (audioPlayer != null) {
            audioPlayer.stop();
            /* Note: http://developer.android.com/reference/android/media/MediaPlayer.html#release() */
            audioPlayer.release();
            Log.d(TAG, "[Ringtone stops]");
        }
    }
}