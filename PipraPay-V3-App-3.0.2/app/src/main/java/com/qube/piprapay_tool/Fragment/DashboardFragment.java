package com.qube.piprapay_tool.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.Database.SmsLogDatabase;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.Model.SmsLogEntry;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Service.SmsReceiverService;
import com.qube.piprapay_tool.Utils.PrefManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshDash;
    private TextView tvHeroDevice, tvHeroPing, tvHeroUrl, tvHeroAdmin;
    private TextView chipSmsPerm, chipNotifPerm, chipBatteryPerm;
    private MaterialButton btnFixPermissions;
    private TextView tvTodayAmount, tvTodayCount, tvWeekAmount, tvWeekCount;
    private TextView tvDashServiceStatus;
    private SwitchCompat switchDashService;
    private TextView tvDashStored, tvDashUsed, tvDashQueued, tvDashErrors;
    private View cvStoredCard, cvUsedCard, cvQueuedCard, cvErrorsCard;
    private TextView tvSim1Carrier, tvSim2Carrier;
    private SwitchCompat switchSim1, switchSim2;
    private MaterialButton btnDashPing, btnDashFlushQueue;

    private PrefManager pref;

    public static class DisplaySmsItem {
        public String sender;
        public String simSlot;
        public String message;
        public String reason;
        public String timestamp;
        public String status;
        public String id;

        public DisplaySmsItem(String sender, String simSlot, String message, String reason, String timestamp, String status) {
            this.sender = sender;
            this.simSlot = simSlot;
            this.message = message;
            this.reason = reason;
            this.timestamp = timestamp;
            this.status = status;
            this.id = String.valueOf(System.currentTimeMillis());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        pref = PrefManager.getInstance(requireContext());

        initViews(view);
        renderData();
        checkPermissionsHealth();
        detectCarriers();
        refreshServerData();

        return view;
    }

    private void initViews(View view) {
        swipeRefreshDash = view.findViewById(R.id.swipeRefreshDash);
        tvHeroDevice = view.findViewById(R.id.tvHeroDevice);
        tvHeroPing = view.findViewById(R.id.tvHeroPing);
        tvHeroUrl = view.findViewById(R.id.tvHeroUrl);
        tvHeroAdmin = view.findViewById(R.id.tvHeroAdmin);

        chipSmsPerm = view.findViewById(R.id.chipSmsPerm);
        chipNotifPerm = view.findViewById(R.id.chipNotifPerm);
        chipBatteryPerm = view.findViewById(R.id.chipBatteryPerm);
        btnFixPermissions = view.findViewById(R.id.btnFixPermissions);

        tvTodayAmount = view.findViewById(R.id.tvTodayAmount);
        tvTodayCount = view.findViewById(R.id.tvTodayCount);
        tvWeekAmount = view.findViewById(R.id.tvWeekAmount);
        tvWeekCount = view.findViewById(R.id.tvWeekCount);

        tvDashServiceStatus = view.findViewById(R.id.tvDashServiceStatus);
        switchDashService = view.findViewById(R.id.switchDashService);

        tvDashStored = view.findViewById(R.id.tvDashStored);
        tvDashUsed = view.findViewById(R.id.tvDashUsed);
        tvDashQueued = view.findViewById(R.id.tvDashQueued);
        tvDashErrors = view.findViewById(R.id.tvDashErrors);

        cvStoredCard = (View) tvDashStored.getParent().getParent();
        cvUsedCard = (View) tvDashUsed.getParent().getParent();
        cvQueuedCard = (View) tvDashQueued.getParent().getParent();
        cvErrorsCard = (View) tvDashErrors.getParent().getParent();

        tvSim1Carrier = view.findViewById(R.id.tvSim1Carrier);
        tvSim2Carrier = view.findViewById(R.id.tvSim2Carrier);
        switchSim1 = view.findViewById(R.id.switchSim1);
        switchSim2 = view.findViewById(R.id.switchSim2);
        btnDashPing = view.findViewById(R.id.btnDashPing);
        btnDashFlushQueue = view.findViewById(R.id.btnDashFlushQueue);

        swipeRefreshDash.setColorSchemeResources(R.color.main_color);
        swipeRefreshDash.setOnRefreshListener(this::refreshServerData);

        switchDashService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleService(isChecked);
        });

        cvStoredCard.setOnClickListener(v -> showCategoryDialog("Stored / Forwarded SMS", "stored"));
        cvUsedCard.setOnClickListener(v -> showCategoryDialog("Matched Transactions", "used"));
        cvQueuedCard.setOnClickListener(v -> showCategoryDialog("Offline Queued SMS", "queue"));
        cvErrorsCard.setOnClickListener(v -> showCategoryDialog("Errors & Retries", "error"));

        switchSim1.setChecked(pref.isSim1Enabled());
        switchSim1.setOnCheckedChangeListener((b, checked) -> {
            pref.setSim1Enabled(checked);
            Toast.makeText(getContext(), "SIM 1 Monitoring: " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        switchSim2.setChecked(pref.isSim2Enabled());
        switchSim2.setOnCheckedChangeListener((b, checked) -> {
            pref.setSim2Enabled(checked);
            Toast.makeText(getContext(), "SIM 2 Monitoring: " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        btnDashPing.setOnClickListener(v -> testPing());
        btnDashFlushQueue.setOnClickListener(v -> flushOfflineQueue());
        btnFixPermissions.setOnClickListener(v -> openAppSettings());
    }

    public void renderData() {
        if (!isAdded()) return;

        String deviceName = pref.getDeviceName();
        tvHeroDevice.setText(TextUtils.isEmpty(deviceName) ? Build.MANUFACTURER + " " + Build.MODEL : deviceName);
        tvHeroUrl.setText("Server: " + pref.getBaseUrl());
        tvHeroAdmin.setText("Admin: " + pref.getAdminFullName() + (TextUtils.isEmpty(pref.getAdminEmail()) ? "" : " (" + pref.getAdminEmail() + ")"));

        tvDashStored.setText(String.valueOf(pref.getStoredCount()));
        tvDashUsed.setText(String.valueOf(pref.getUsedCount()));
        tvDashErrors.setText(String.valueOf(pref.getErrorCount()));
        tvDashQueued.setText(String.valueOf(pref.getOfflineSmsQueue().size()));

        // Daily / Weekly stats from DB
        SmsLogDatabase.SummaryStats summary = SmsLogDatabase.getInstance(requireContext()).getSummaryStats();
        tvTodayAmount.setText(String.format(Locale.US, "৳ %.2f", summary.todayAmount));
        tvTodayCount.setText(summary.todayCount + " Transactions");
        tvWeekAmount.setText(String.format(Locale.US, "৳ %.2f", summary.weekAmount));
        tvWeekCount.setText(summary.weekCount + " Transactions");

        boolean isRunning = pref.isServiceRunning();
        switchDashService.setChecked(isRunning);
        if (isRunning) {
            tvDashServiceStatus.setText("Active & listening for payment SMS");
            tvDashServiceStatus.setTextColor(getResources().getColor(R.color.Darkgreen));
        } else {
            tvDashServiceStatus.setText("Paused. SMS won't be forwarded");
            tvDashServiceStatus.setTextColor(getResources().getColor(R.color.redMa));
        }
    }

    private void showCategoryDialog(String title, String type) {
        List<DisplaySmsItem> items = new ArrayList<>();

        if ("queue".equals(type)) {
            List<SmsItem> queue = pref.getOfflineSmsQueue();
            for (SmsItem s : queue) {
                items.add(new DisplaySmsItem(
                        s.getSender(),
                        s.getSimSlot(),
                        s.getMessage(),
                        "Pending offline retry",
                        "",
                        "QUEUED"
                ));
            }
        } else if ("error".equals(type)) {
            // Parse server errors
            try {
                JSONArray arr = new JSONArray(pref.getServerErrorsJson());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    items.add(new DisplaySmsItem(
                            obj.optString("sender", "Unknown"),
                            obj.optString("simslot", "1"),
                            obj.optString("message", ""),
                            "Reason: " + obj.optString("reason", "Rejected by server"),
                            obj.optString("timestamp", ""),
                            "ERROR"
                    ));
                }
            } catch (Exception ignored) {}

            // Also include local failed logs if any
            List<SmsLogEntry> logs = SmsLogDatabase.getInstance(requireContext()).getAllLogs();
            for (SmsLogEntry log : logs) {
                if ("QUEUED".equalsIgnoreCase(log.getStatus()) || "FILTERED".equalsIgnoreCase(log.getStatus()) || log.getStatus().toUpperCase().contains("FAIL")) {
                    boolean alreadyInList = false;
                    for (DisplaySmsItem it : items) {
                        if (it.message.equals(log.getMessage())) {
                            alreadyInList = true;
                            break;
                        }
                    }
                    if (!alreadyInList) {
                        items.add(new DisplaySmsItem(
                                log.getSender(),
                                log.getSimSlot(),
                                log.getMessage(),
                                log.getResponseMessage(),
                                log.getTimestamp(),
                                log.getStatus()
                        ));
                    }
                }
            }
        } else if ("stored".equals(type)) {
            try {
                JSONArray arr = new JSONArray(pref.getServerStoredJson());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    items.add(new DisplaySmsItem(
                            obj.optString("sender", "Unknown"),
                            obj.optString("simslot", "1"),
                            obj.optString("message", ""),
                            "Status: Forwarded to server",
                            obj.optString("timestamp", ""),
                            "STORED"
                    ));
                }
            } catch (Exception ignored) {}
        } else if ("used".equals(type)) {
            try {
                JSONArray arr = new JSONArray(pref.getServerUsedJson());
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    items.add(new DisplaySmsItem(
                            obj.optString("sender", "Unknown"),
                            obj.optString("simslot", "1"),
                            obj.optString("message", ""),
                            "Status: Matched & Paid",
                            obj.optString("timestamp", ""),
                            "MATCHED"
                    ));
                }
            } catch (Exception ignored) {}
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_offline_queue, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextView tvQueueTitle = dialogView.findViewById(R.id.tvQueueTitle);
        TextView tvQueueCountBadge = dialogView.findViewById(R.id.tvQueueCountBadge);
        TextView tvQueueSubtitle = dialogView.findViewById(R.id.tvQueueSubtitle);
        RecyclerView rvQueueList = dialogView.findViewById(R.id.rvQueueList);
        TextView tvEmptyQueue = dialogView.findViewById(R.id.tvEmptyQueue);
        MaterialButton btnRetryAllQueue = dialogView.findViewById(R.id.btnRetryAllQueue);
        MaterialButton btnClearQueue = dialogView.findViewById(R.id.btnClearQueue);

        tvQueueTitle.setText(title);
        tvQueueCountBadge.setText(items.size() + " Total");

        if ("error".equals(type)) {
            tvQueueSubtitle.setText("Failed / unparsed SMS that need review or re-transmission.");
            tvQueueCountBadge.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.redMa));
            tvQueueCountBadge.setTextColor(Color.WHITE);
            btnRetryAllQueue.setText("Retry & Re-transmit All");
            btnClearQueue.setText("Close");
        } else if ("queue".equals(type)) {
            tvQueueSubtitle.setText("SMS stored offline while device was disconnected.");
            btnRetryAllQueue.setText("Flush & Send Now");
            btnClearQueue.setText("Clear Queue");
        } else {
            tvQueueSubtitle.setText("List of processed payment records.");
            btnRetryAllQueue.setVisibility(View.GONE);
            btnClearQueue.setText("Close");
        }

        if (items.isEmpty()) {
            tvEmptyQueue.setVisibility(View.VISIBLE);
            tvEmptyQueue.setText("No records found in " + title + ".");
            rvQueueList.setVisibility(View.GONE);
            btnRetryAllQueue.setEnabled(false);
        } else {
            tvEmptyQueue.setVisibility(View.GONE);
            rvQueueList.setVisibility(View.VISIBLE);
            rvQueueList.setLayoutManager(new LinearLayoutManager(getContext()));
            rvQueueList.setAdapter(new RecyclerView.Adapter<QueueRowHolder>() {
                @NonNull
                @Override
                public QueueRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_row, parent, false);
                    return new QueueRowHolder(v);
                }

                @Override
                public void onBindViewHolder(@NonNull QueueRowHolder holder, int position) {
                    DisplaySmsItem item = items.get(position);
                    holder.tvSender.setText(item.sender);
                    holder.tvSim.setText("SIM " + item.simSlot);
                    holder.tvBody.setText(item.message);
                    holder.tvReason.setText(item.reason);
                    if (!TextUtils.isEmpty(item.timestamp)) {
                        holder.tvTime.setText(item.timestamp);
                        holder.tvTime.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvTime.setVisibility(View.GONE);
                    }

                    if ("ERROR".equalsIgnoreCase(item.status)) {
                        holder.tvReason.setTextColor(getResources().getColor(R.color.redMa));
                    } else if ("QUEUED".equalsIgnoreCase(item.status)) {
                        holder.tvReason.setTextColor(getResources().getColor(R.color.sandyOrange));
                    } else {
                        holder.tvReason.setTextColor(getResources().getColor(R.color.main_color));
                    }
                }

                @Override
                public int getItemCount() {
                    return items.size();
                }
            });
        }

        btnRetryAllQueue.setOnClickListener(v -> {
            dialog.dismiss();
            if ("queue".equals(type)) {
                flushOfflineQueue();
            } else if ("error".equals(type)) {
                retryErrorItems(items);
            }
        });

        btnClearQueue.setOnClickListener(v -> {
            if ("queue".equals(type)) {
                pref.clearOfflineSmsQueue();
                renderData();
                Toast.makeText(getContext(), "Offline queue cleared", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void retryErrorItems(List<DisplaySmsItem> items) {
        if (items.isEmpty()) {
            Toast.makeText(getContext(), "No items to retry.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SmsItem> transmitList = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (DisplaySmsItem it : items) {
            transmitList.add(new SmsItem(
                    String.valueOf(now++),
                    it.sender,
                    it.message,
                    it.simSlot,
                    String.valueOf(System.currentTimeMillis() / 1000)
            ));
        }

        Toast.makeText(getContext(), "Re-transmitting " + transmitList.size() + " messages to server...", Toast.LENGTH_SHORT).show();
        PipraPayApi.getInstance(requireContext()).transmitSms(transmitList, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Re-transmission complete! Refreshing...", Toast.LENGTH_SHORT).show();
                    refreshServerData();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Retry error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private static class QueueRowHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvSim, tvBody, tvReason, tvTime;
        public QueueRowHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvQueueRowSender);
            tvSim = itemView.findViewById(R.id.tvQueueRowSim);
            tvBody = itemView.findViewById(R.id.tvQueueRowBody);
            tvReason = itemView.findViewById(R.id.tvQueueRowReason);
            tvTime = itemView.findViewById(R.id.tvQueueRowTime);
        }
    }

    private void checkPermissionsHealth() {
        Context ctx = getContext();
        if (ctx == null) return;

        boolean smsOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;

        boolean notifOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifOk = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        boolean batteryOk = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            batteryOk = pm != null && pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
        }

        if (smsOk) {
            chipSmsPerm.setText("Granted ✓");
            chipSmsPerm.setTextColor(Color.parseColor("#29a56c"));
            chipSmsPerm.setBackgroundColor(Color.parseColor("#E8F8F0"));
        } else {
            chipSmsPerm.setText("Missing ⚠️");
            chipSmsPerm.setTextColor(Color.parseColor("#DD5050"));
            chipSmsPerm.setBackgroundColor(Color.parseColor("#FDE8E8"));
        }

        if (notifOk) {
            chipNotifPerm.setText("Allowed ✓");
            chipNotifPerm.setTextColor(Color.parseColor("#29a56c"));
            chipNotifPerm.setBackgroundColor(Color.parseColor("#E8F8F0"));
        } else {
            chipNotifPerm.setText("Missing ⚠️");
            chipNotifPerm.setTextColor(Color.parseColor("#DD5050"));
            chipNotifPerm.setBackgroundColor(Color.parseColor("#FDE8E8"));
        }

        if (batteryOk) {
            chipBatteryPerm.setText("Whitelisted ✓");
            chipBatteryPerm.setTextColor(Color.parseColor("#29a56c"));
            chipBatteryPerm.setBackgroundColor(Color.parseColor("#E8F8F0"));
        } else {
            chipBatteryPerm.setText("Whitelist ⚠️");
            chipBatteryPerm.setTextColor(Color.parseColor("#F7941D"));
            chipBatteryPerm.setBackgroundColor(Color.parseColor("#FEF4E8"));
        }

        if (!smsOk || !notifOk || !batteryOk) {
            btnFixPermissions.setVisibility(View.VISIBLE);
        } else {
            btnFixPermissions.setVisibility(View.GONE);
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Please allow permissions in device settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleService(boolean enable) {
        Intent serviceIntent = new Intent(requireContext(), SmsReceiverService.class);
        if (enable) {
            serviceIntent.setAction(SmsReceiverService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent);
            } else {
                requireContext().startService(serviceIntent);
            }
            pref.setServiceRunning(true);
        } else {
            serviceIntent.setAction(SmsReceiverService.ACTION_STOP);
            requireContext().startService(serviceIntent);
            pref.setServiceRunning(false);
        }
        renderData();
    }

    private void refreshServerData() {
        swipeRefreshDash.setRefreshing(true);
        PipraPayApi.getInstance(requireContext()).getAccountInfo(new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    swipeRefreshDash.setRefreshing(false);
                    renderData();
                    Toast.makeText(getContext(), "Server data refreshed!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    swipeRefreshDash.setRefreshing(false);
                    Toast.makeText(getContext(), "Sync Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
        PipraPayApi.getInstance(requireContext()).getWhitelistedSenders(null);
    }

    private void testPing() {
        tvHeroPing.setText("Ping...");
        final long startTime = System.currentTimeMillis();

        PipraPayApi.getInstance(requireContext()).getAccountInfo(new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                long latency = System.currentTimeMillis() - startTime;
                if (isAdded()) {
                    tvHeroPing.setText("Ping: " + latency + "ms");
                    tvHeroPing.setTextColor(getResources().getColor(R.color.main_color));
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    tvHeroPing.setText("Ping: Offline");
                    tvHeroPing.setTextColor(getResources().getColor(R.color.redMa));
                }
            }
        });
    }

    private void flushOfflineQueue() {
        List<SmsItem> queue = pref.getOfflineSmsQueue();
        if (queue.isEmpty()) {
            Toast.makeText(getContext(), "Offline queue is already empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Transmitting " + queue.size() + " queued messages...", Toast.LENGTH_SHORT).show();
        PipraPayApi.getInstance(requireContext()).transmitSms(queue, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                if (isAdded()) {
                    pref.clearOfflineSmsQueue();
                    renderData();
                    Toast.makeText(getContext(), "Queue flushed successfully!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Flush failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void detectCarriers() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager sm = (SubscriptionManager) requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    List<SubscriptionInfo> subList = sm.getActiveSubscriptionInfoList();
                    if (subList != null && !subList.isEmpty()) {
                        for (int i = 0; i < subList.size(); i++) {
                            SubscriptionInfo info = subList.get(i);
                            String carrier = info.getDisplayName() != null ? info.getDisplayName().toString() : "Carrier " + (i + 1);
                            if (info.getSimSlotIndex() == 0) {
                                tvSim1Carrier.setText("SIM 1 (" + carrier + ")");
                            } else if (info.getSimSlotIndex() == 1) {
                                tvSim2Carrier.setText("SIM 2 (" + carrier + ")");
                            }
                        }
                    }
                }
            }
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        renderData();
        checkPermissionsHealth();
    }
}