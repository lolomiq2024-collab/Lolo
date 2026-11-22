package com.loai.azanhome;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Azan Home — يعرض الصفحة المدمجة (assets/index.html) بملء الشاشة.
 * By Loai.miqdadi
 */
public class MainActivity extends Activity {

    private static final String START_URL = "file:///android_asset/index.html";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        web = new WebView(this);
        web.setBackgroundColor(0xFF050A18);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                 // حفظ الإعدادات (localStorage)
        s.setMediaPlaybackRequiresUserGesture(false); // تشغيل الصوت/الفيديو بدون لمس
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);  // الطقس والبث من الإنترنت
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                recreate();   // إن انهارت عملية العرض نعيد التشغيل بدل الإغلاق
                return true;
            }
        });

        web.loadUrl(START_URL);
        web.requestFocus();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            immersive();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        immersive();
        if (web != null) {
            web.onResume();
        }
    }

    /** زر الرجوع يُمرَّر للصفحة ولا يغلق التطبيق (وضع كشك). للخروج استخدم زر الرئيسية. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP && web != null) {
                web.evaluateJavascript(
                        "if(document.onkeydown){document.onkeydown({keyCode:27,preventDefault:function(){}});}",
                        null);
            }
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public void onBackPressed() {
        // لا شيء
    }

    private class AndroidBridge {
        @JavascriptInterface public void showAdhanOverlay(String name) { startAzan(name, AzanService.ACTION_PLAY); }
        @JavascriptInterface public void playAdhanAudio() { startAzan("", AzanService.ACTION_PLAY); }
        @JavascriptInterface public void hideAdhanOverlay() { startAzan("", AzanService.ACTION_STOP); }
        @JavascriptInterface public void stopAdhanAudio() { startAzan("", AzanService.ACTION_STOP); }
        @JavascriptInterface public void scheduleAdhans(String json) {
            try {
                JSONObject o = new JSONObject(json);
                String[] keys = {"fajr","dhuhr","asr","maghrib","isha"};
                String[] names = {"الفجر","الظهر","العصر","المغرب","العشاء"};
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                for (int i=0;i<keys.length;i++) {
                    long when = o.optLong(keys[i], 0); if (when <= System.currentTimeMillis()) when += 86400000L;
                    Intent in = new Intent(MainActivity.this, AdhanAlarmReceiver.class).putExtra(AzanService.EXTRA_NAME, names[i]);
                    PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 700+i, in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    if (android.os.Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi); else am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
                }
            } catch (Exception ignored) {}
        }
    }

    private void startAzan(String name, String action) {
        Intent i = new Intent(this, AzanService.class).setAction(action).putExtra(AzanService.EXTRA_NAME, name);
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
