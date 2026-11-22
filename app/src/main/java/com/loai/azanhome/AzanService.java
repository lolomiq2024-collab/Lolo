package com.loai.azanhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class AzanService extends Service {

    public static final String ACTION_PLAY =
            "com.loai.azanhome.PLAY";

    public static final String ACTION_STOP =
            "com.loai.azanhome.STOP";

    public static final String EXTRA_NAME = "name";
    private static final String CHANNEL = "azan_playback";

    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private WindowManager windowManager;
    private TextView overlay;

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
        startForeground(7, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null
                ? ACTION_STOP
                : intent.getAction();

        if (ACTION_PLAY.equals(action)) {
            String name = intent.getStringExtra(EXTRA_NAME);
            playAdhan(name == null ? "الأذان" : name);
        } else {
            stopPlayback();
        }

        return START_NOT_STICKY;
    }

    private void playAdhan(String name) {
        requestAudioFocus();
        showOverlay(name);
        stopPlayerOnly();

        try {
            android.content.res.AssetFileDescriptor afd =
                    getAssets().openFd("adhan.mp3");

            player = new MediaPlayer();
            player.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength()
            );

            afd.close();
            player.prepare();

        } catch (Exception ignored) {
            player = null;
        }

        if (player != null) {
            player.setOnCompletionListener(mp -> stopPlayback());
            player.start();
        } else {
            stopPlayback();
        }
    }

    private void requestAudioFocus() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
                    .setAudioAttributes(attributes)
                    .build();

            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            );
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    private void stopPlayerOnly() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
            }

            player.release();
            player = null;
        }
    }

    private void stopPlayback() {
        stopPlayerOnly();
        abandonAudioFocus();
        hideOverlay();
        stopSelf();
    }

    private void showOverlay(String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            return;
        }

        if (overlay != null) {
            return;
        }

        overlay = new TextView(this);
        overlay.setText("حان الآن موعد أذان\n" + name);
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(30);
        overlay.setGravity(Gravity.CENTER);
        overlay.setBackgroundColor(0xEE050A18);

        int windowType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            windowType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        android.graphics.PixelFormat.TRANSLUCENT
                );

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.addView(overlay, params);
        } catch (Exception ignored) {
            overlay = null;
        }
    }

    private void hideOverlay() {
        if (windowManager != null && overlay != null) {
            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {
            }

            overlay = null;
        }
    }

    private Notification createNotification() {
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("Azan Home")
                .setContentText("خدمة الأذان تعمل")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL,
                    "Azan playback",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
