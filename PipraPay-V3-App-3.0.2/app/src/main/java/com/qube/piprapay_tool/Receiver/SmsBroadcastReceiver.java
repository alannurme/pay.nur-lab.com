package com.qube.piprapay_tool.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.Database.SmsLogDatabase;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.Model.SmsLogEntry;
import com.qube.piprapay_tool.Utils.PrefManager;
import com.qube.piprapay_tool.Utils.SoundHelper;
import com.qube.piprapay_tool.Worker.SmsSyncWorker;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())
                && !"android.provider.Telephony.SMS_DELIVER".equals(intent.getAction())) {
            return;
        }

        PrefManager pref = PrefManager.getInstance(context);
        if (!pref.isLoggedIn() || !pref.isServiceRunning()) {
            Log.d(TAG, "PipraPay Companion is not paired or service is disabled.");
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;

        String format = bundle.getString("format");
        StringBuilder fullMessage = new StringBuilder();
        String sender = "";
        long timestampMillis = System.currentTimeMillis();

        for (Object pdu : pdus) {
            SmsMessage sms;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) pdu);
            }
            if (sms != null) {
                sender = sms.getOriginatingAddress();
                fullMessage.append(sms.getDisplayMessageBody());
                timestampMillis = sms.getTimestampMillis();
            }
        }

        if (sender == null || sender.trim().isEmpty()) {
            return;
        }

        String simSlot = detectSimSlot(bundle);

        // Check if SIM is enabled in settings
        if ("1".equals(simSlot) && !pref.isSim1Enabled()) {
            Log.d(TAG, "SIM 1 monitoring is disabled in settings. Skipping.");
            return;
        }
        if ("2".equals(simSlot) && !pref.isSim2Enabled()) {
            Log.d(TAG, "SIM 2 monitoring is disabled in settings. Skipping.");
            return;
        }

        String id = String.valueOf(timestampMillis);
        String timestampSec = String.valueOf(timestampMillis / 1000);
        String formattedTime = new SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault()).format(new Date(timestampMillis));

        final SmsItem smsItem = new SmsItem(id, sender, fullMessage.toString(), simSlot, timestampSec);
        Log.i(TAG, "Captured incoming SMS from: " + sender + " on SIM: " + simSlot);

        // Check sender filter
        boolean isWhitelisted = pref.isSenderAllowed(sender);
        String initialStatus = isWhitelisted ? "QUEUED" : "FILTERED";

        final SmsLogEntry logEntry = new SmsLogEntry(
                sender,
                fullMessage.toString(),
                simSlot,
                formattedTime,
                initialStatus,
                isWhitelisted ? "Processing forwarding..." : "Ignored: Non-whitelisted sender"
        );

        final long dbId = SmsLogDatabase.getInstance(context).insertLog(logEntry);

        if (!isWhitelisted) {
            Log.d(TAG, "Sender " + sender + " is not in whitelist. Skipping transmission.");
            return;
        }

        List<SmsItem> items = new ArrayList<>();
        items.add(smsItem);

        // Transmit to PipraPay V3 Server
        PipraPayApi.getInstance(context).transmitSms(items, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "SMS successfully delivered to PipraPay V3 server: " + response.toString());
                pref.setStoredCount(pref.getStoredCount() + 1);
                pref.setLastSyncTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

                logEntry.setStatus("SENT");
                logEntry.setResponseMessage("Matched & Delivered: " + response.optString("message", "Success"));
                SmsLogDatabase.getInstance(context).insertLog(logEntry);

                // Play pleasant payment chime & vibration
                SoundHelper.playSuccessSound(context);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Failed to deliver SMS online (" + errorMessage + "). Queuing offline.");
                pref.setErrorCount(pref.getErrorCount() + 1);
                pref.addOfflineSms(smsItem);
                scheduleOfflineSync(context);

                logEntry.setStatus("QUEUED");
                logEntry.setResponseMessage("Queued offline: " + errorMessage);
                SmsLogDatabase.getInstance(context).insertLog(logEntry);

                SoundHelper.playFailureSound(context);
            }
        });
    }

    private String detectSimSlot(Bundle bundle) {
        int slot = 1;
        if (bundle.containsKey("slot")) {
            slot = bundle.getInt("slot", 0) + 1;
        } else if (bundle.containsKey("simSlot")) {
            slot = bundle.getInt("simSlot", 0) + 1;
        } else if (bundle.containsKey("simId")) {
            slot = bundle.getInt("simId", 0) + 1;
        } else if (bundle.containsKey("subscription")) {
            int subId = bundle.getInt("subscription", -1);
            if (subId > 1) {
                slot = 2;
            }
        }
        return String.valueOf(slot);
    }

    private void scheduleOfflineSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SmsSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(syncRequest);
    }
}