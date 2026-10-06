package com.qube.piprapay_tool.Utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.qube.piprapay_tool.R;

public class SoundHelper {
    private static final String TAG = "SoundHelper";

    public static void playSuccessSound(Context context) {
        PrefManager pref = PrefManager.getInstance(context);
        if (pref.isSoundEnabled()) {
            playSound(context, R.raw.pos_success);
        }
        if (pref.isVibrationEnabled()) {
            vibrate(context, 200);
        }
    }

    public static void playFailureSound(Context context) {
        PrefManager pref = PrefManager.getInstance(context);
        if (pref.isSoundEnabled()) {
            playSound(context, R.raw.pos_failed);
        }
        if (pref.isVibrationEnabled()) {
            vibrate(context, 400);
        }
    }

    private static void playSound(Context context, int soundResId) {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(context.getApplicationContext(), soundResId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + e.getMessage());
        }
    }

    private static void vibrate(Context context, long durationMs) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMs);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating: " + e.getMessage());
        }
    }
}