package com.loai.azanhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
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
    public static final String ACTION_PLAY = "com.loai.azanhome.PLAY";
    public static final String ACTION_STOP = "com.loai.azanhome.STOP";
    public static final String EXTRA_NAME = "name";
    private static final String CHANNEL = "azan_playback";
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private WindowManager wm;
    private TextView overlay;

    @Override public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createChannel();
        startForeground(7, notification());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (ACTION_PLAY.equals(action)) play(intent.getStringExtra(EXTRA_NAME)); else stopPlayback();
        return START_NOT_STICKY;
    }

    private void play(String name) {
        requestFocus();
        showOverlay(name == null ? "الأذان" : name);
        stopPlayerOnly();
        player = MediaPlayer.create(this, getResources().getIdentifier("adhan", "raw", getPackageName()));
        if (player == null) {
            try { player = MediaPlayer.create(this, getAssets().openFd("adhan.mp3")); } catch (Exception ignored) {}
        }
        if (player != null) {
            player.setOnCompletionListener(mp -> stopPlayback());
            player.start();
        } else stopPlayback();
    }

    private void requestFocus() {
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setAudioAttributes(attrs).build();
            audioManager.requestAudioFocus(focusRequest);
        } else audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
    }

    private void abandonFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
        else audioManager.abandonAudioFocus(null);
    }

    private void stopPlayerOnly() { if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; } }
    private void stopPlayback() { stopPlayerOnly(); abandonFocus(); hideOverlay(); stopSelf(); }

    private void showOverlay(String name) {
        if (!Settings.canDrawOverlays(this) || overlay != null) return;
        overlay = new TextView(this); overlay.setText("حان الآن موعد أذان\n" + name); overlay.setTextColor(Color.WHITE); overlay.setTextSize(30); overlay.setGravity(Gravity.CENTER); overlay.setBackgroundColor(0xEE050A18);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, -3);
        try { wm = (WindowManager) getSystemService(WINDOW_SERVICE); wm.addView(overlay, p); } catch (Exception ignored) { overlay = null; }
    }
    private void hideOverlay() { if (wm != null && overlay != null) { try { wm.removeView(overlay); } catch (Exception ignored) {} overlay = null; } }

    private Notification notification() { return new Notification.Builder(this, CHANNEL).setContentTitle("Azan Home").setContentText("خدمة الأذان تعمل").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setOngoing(true).build(); }
    private void createChannel() { if (Build.VERSION.SDK_INT >= 26) ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL, "Azan playback", NotificationManager.IMPORTANCE_LOW)); }
    @Override public void onDestroy() { stopPlayback(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
