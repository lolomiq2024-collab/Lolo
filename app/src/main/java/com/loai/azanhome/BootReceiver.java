package com.loai.azanhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Context appContext = context.getApplicationContext();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent launchIntent = new Intent(
                            appContext,
                            MainActivity.class
                    );

                    launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    appContext.startActivity(launchIntent);

                } catch (Exception ignored) {
                    // بعض أجهزة Android TV تمنع فتح Activity بعد الإقلاع
                }
            }, 10000);
        }
    }
}
