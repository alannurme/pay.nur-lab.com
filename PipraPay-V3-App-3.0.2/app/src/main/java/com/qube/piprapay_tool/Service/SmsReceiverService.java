package com.qube.piprapay_tool.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.qube.piprapay_tool.Activity.MainActivity;
import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Receiver.SmsBroadcastReceiver;
import com.qube.piprapay_tool.Utils.PrefManager;

import org.json.JSONObject;

import java.util.List;

public class SmsReceiverService extends Service {
    private static final String TAG = "SmsReceiverService";
    public static final String CHANNEL_ID = "piprapay_companion_channel";
    public static final int NOTIFICATION_ID = 99881;

    public static final String ACTION_START = "com.qube.piprapay_tool.ACTION_START";
    public static final String ACTION_STOP = "com.qube.piprapay_tool.ACTION_STOP";

    private SmsBroadcastReceiver dynamicReceiver;
    private Handler periodicHandler;
    private Runnable periodicRunnable;
    private static final long PERIODIC_INTERVAL = 5 * 60 * 1000; // 5 minutes

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        registerDynamicSmsReceiver();
        startPeriodicSync();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Log.d(TAG, "Stop action received. Stopping service.");
            PrefManager.getInstance(this).setServiceRunning(false);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        PrefManager.getInstance(this).setServiceRunning(true);
        Notification notification = buildForegroundNotification("PipraPay Companion Active", "Monitoring incoming payments SMS...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // If DATA_SYNC is defined in AndroidManifest, use it, otherwise normal startForeground
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } catch (Exception e) {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "PipraPay SMS Gateway Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps PipraPay Companion active to receive and forward payment SMS.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildForegroundNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void registerDynamicSmsReceiver() {
        if (dynamicReceiver == null) {
            dynamicReceiver = new SmsBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            filter.setPriority(999);
            registerReceiver(dynamicReceiver, filter);
            Log.d(TAG, "Dynamic SMS Receiver registered.");
        }
    }

    private void unregisterDynamicSmsReceiver() {
        if (dynamicReceiver != null) {
            try {
                unregisterReceiver(dynamicReceiver);
                Log.d(TAG, "Dynamic SMS Receiver unregistered.");
            } catch (Exception ignored) {
            }
            dynamicReceiver = null;
        }
    }

    private void startPeriodicSync() {
        periodicHandler = new Handler(Looper.getMainLooper());
        periodicRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    PrefManager pref = PrefManager.getInstance(SmsReceiverService.this);
                    if (pref.isLoggedIn()) {
                        // 1. Refresh sender list
                        PipraPayApi.getInstance(SmsReceiverService.this).getWhitelistedSenders(null);

                        // 2. Transmit any offline queue
                        List<SmsItem> queue = pref.getOfflineSmsQueue();
                        if (!queue.isEmpty()) {
                            PipraPayApi.getInstance(SmsReceiverService.this).transmitSms(queue, new PipraPayApi.ApiCallback<JSONObject>() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    pref.clearOfflineSmsQueue();
                                    Log.d(TAG, "Periodic sync sent offline queue.");
                                }

                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Periodic offline sync error: " + error);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Periodic sync exception", e);
                } finally {
                    periodicHandler.postDelayed(this, PERIODIC_INTERVAL);
                }
            }
        };
        periodicHandler.postDelayed(periodicRunnable, PERIODIC_INTERVAL);
    }

    @Override
    public void onDestroy() {
        PrefManager.getInstance(this).setServiceRunning(false);
        unregisterDynamicSmsReceiver();
        if (periodicHandler != null && periodicRunnable != null) {
            periodicHandler.removeCallbacks(periodicRunnable);
        }
        super.onDestroy();
        Log.d(TAG, "SmsReceiverService destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
