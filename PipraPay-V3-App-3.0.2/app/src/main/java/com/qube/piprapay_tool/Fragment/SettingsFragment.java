package com.qube.piprapay_tool.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
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
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.qube.piprapay_tool.Activity.InboxSyncActivity;
import com.qube.piprapay_tool.Activity.LoginActivity;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Service.SmsReceiverService;
import com.qube.piprapay_tool.Utils.AppUpdater;
import com.qube.piprapay_tool.Utils.PrefManager;

import java.util.Set;

public class SettingsFragment extends Fragment {

    private TextView tvAppVersion;
    private MaterialButton btnCheckAppUpdate;
    private MaterialButton btnOpenInboxSync;
    private SwitchCompat switchSound;
    private SwitchCompat switchVibration;
    private SwitchCompat switchAutoStart;
    private TextView tvSettingsSenders;
    private MaterialButton btnSettingsBatteryOpt;
    private MaterialButton btnSettingsDisconnect;

    private PrefManager pref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        pref = PrefManager.getInstance(requireContext());

        initViews(view);
        renderSettings();

        return view;
    }

    private void initViews(View view) {
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
        btnCheckAppUpdate = view.findViewById(R.id.btnCheckAppUpdate);
        btnOpenInboxSync = view.findViewById(R.id.btnOpenInboxSync);
        switchSound = view.findViewById(R.id.switchSound);
        switchVibration = view.findViewById(R.id.switchVibration);
        switchAutoStart = view.findViewById(R.id.switchAutoStart);
        tvSettingsSenders = view.findViewById(R.id.tvSettingsSenders);
        btnSettingsBatteryOpt = view.findViewById(R.id.btnSettingsBatteryOpt);
        btnSettingsDisconnect = view.findViewById(R.id.btnSettingsDisconnect);

        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            tvAppVersion.setText("Current Version: v" + pInfo.versionName + " (Build " + pInfo.versionCode + ")");
        } catch (Exception ignored) {}

        btnCheckAppUpdate.setOnClickListener(v -> {
            btnCheckAppUpdate.setEnabled(false);
            btnCheckAppUpdate.setText("Checking for update...");
            AppUpdater.checkForUpdates(requireContext(), true, new AppUpdater.UpdateCheckCallback() {
                @Override
                public void onNoUpdate() {
                    if (isAdded()) {
                        btnCheckAppUpdate.setEnabled(true);
                        btnCheckAppUpdate.setText("Check for App Updates (1-Click)");
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        btnCheckAppUpdate.setEnabled(true);
                        btnCheckAppUpdate.setText("Check for App Updates (1-Click)");
                        Toast.makeText(getContext(), "Update Check Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        btnOpenInboxSync.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), InboxSyncActivity.class));
        });

        switchSound.setOnCheckedChangeListener((b, checked) -> {
            pref.setSoundEnabled(checked);
            Toast.makeText(getContext(), "Payment Chime: " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        switchVibration.setOnCheckedChangeListener((b, checked) -> {
            pref.setVibrationEnabled(checked);
            Toast.makeText(getContext(), "Vibration: " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        switchAutoStart.setOnCheckedChangeListener((b, checked) -> {
            pref.setAutoStartEnabled(checked);
            Toast.makeText(getContext(), "Auto-Start: " + (checked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        btnSettingsBatteryOpt.setOnClickListener(v -> requestDisableBatteryOptimization());
        btnSettingsDisconnect.setOnClickListener(v -> confirmDisconnect());
    }

    private void renderSettings() {
        switchSound.setChecked(pref.isSoundEnabled());
        switchVibration.setChecked(pref.isVibrationEnabled());
        switchAutoStart.setChecked(pref.isAutoStartEnabled());

        Set<String> senders = pref.getWhitelistedSenders();
        if (senders != null && !senders.isEmpty()) {
            tvSettingsSenders.setText(TextUtils.join(", ", senders));
        }
    }

    @SuppressLint("BatteryLife")
    private void requestDisableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(requireContext().getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Battery optimization is already disabled for PipraPay!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(getContext(), "Please disable battery optimization from Settings.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void confirmDisconnect() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Disconnect Device")
                .setMessage("Are you sure you want to unpair this device from PipraPay server? Incoming SMS will stop syncing.")
                .setPositiveButton("Disconnect", (dialog, which) -> {
                    Intent serviceIntent = new Intent(requireContext(), SmsReceiverService.class);
                    serviceIntent.setAction(SmsReceiverService.ACTION_STOP);
                    requireContext().startService(serviceIntent);

                    pref.clearSession();
                    startActivity(new Intent(requireActivity(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}