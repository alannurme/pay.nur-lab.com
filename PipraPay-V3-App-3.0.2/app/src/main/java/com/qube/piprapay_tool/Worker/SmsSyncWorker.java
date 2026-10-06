package com.qube.piprapay_tool.Worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.Utils.PrefManager;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmsSyncWorker extends Worker {
    private static final String TAG = "SmsSyncWorker";

    public SmsSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PrefManager pref = PrefManager.getInstance(context);

        if (!pref.isLoggedIn()) {
            Log.d(TAG, "Device not logged in. Skipping sync.");
            return Result.failure();
        }

        List<SmsItem> queue = pref.getOfflineSmsQueue();
        if (queue == null || queue.isEmpty()) {
            Log.d(TAG, "Offline SMS queue is empty.");
            return Result.success();
        }

        Log.d(TAG, "Attempting to sync " + queue.size() + " queued SMS messages...");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isSuccess = new AtomicBoolean(false);

        PipraPayApi.getInstance(context).transmitSms(queue, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "Queued SMS sync success: " + response.toString());
                pref.clearOfflineSmsQueue();
                isSuccess.set(true);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Queued SMS sync failed: " + error);
                isSuccess.set(false);
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while waiting for sync", e);
            return Result.retry();
        }

        if (isSuccess.get()) {
            return Result.success();
        } else {
            return Result.retry();
        }
    }
}
