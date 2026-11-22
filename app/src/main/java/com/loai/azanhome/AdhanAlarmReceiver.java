package com.loai.azanhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AdhanAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, AzanService.class).setAction(AzanService.ACTION_PLAY).putExtra(AzanService.EXTRA_NAME, intent.getStringExtra(AzanService.EXTRA_NAME));
        if (android.os.Build.VERSION.SDK_INT >= 26) context.startForegroundService(service); else context.startService(service);
    }
}
