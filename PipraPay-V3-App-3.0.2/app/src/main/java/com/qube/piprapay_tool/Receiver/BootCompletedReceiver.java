package com.qube.piprapay_tool.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.qube.piprapay_tool.Service.SmsReceiverService;
import com.qube.piprapay_tool.Utils.PrefManager;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            Log.d(TAG, "Boot completed event received.");
            PrefManager pref = PrefManager.getInstance(context);
            if (pref.isLoggedIn() && pref.isServiceRunning() && pref.isAutoStartEnabled()) {
                Intent serviceIntent = new Intent(context, SmsReceiverService.class);
                serviceIntent.setAction(SmsReceiverService.ACTION_START);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                Log.d(TAG, "PipraPay Companion foreground service started on boot.");
            }
        }
    }
}