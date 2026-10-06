package com.qube.piprapay_tool.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.qube.piprapay_tool.Model.SmsItem;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrefManager {
    private static final String PREF_NAME = "piprapay_v3_prefs";
    private static PrefManager instance;
    private final SharedPreferences prefs;

    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_TOKEN = "session_token";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_ADMIN_NAME = "admin_name";
    private static final String KEY_ADMIN_EMAIL = "admin_email";
    private static final String KEY_STORED_COUNT = "stored_count";
    private static final String KEY_USED_COUNT = "used_count";
    private static final String KEY_ERROR_COUNT = "error_count";
    private static final String KEY_SERVICE_RUNNING = "service_running";
    private static final String KEY_WHITELISTED_SENDERS = "whitelisted_senders";
    private static final String KEY_OFFLINE_SMS_QUEUE = "offline_sms_queue";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";

    private static final String KEY_SERVER_ERRORS = "server_errors_json";
    private static final String KEY_SERVER_STORED = "server_stored_json";
    private static final String KEY_SERVER_USED = "server_used_json";

    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_SIM1_ENABLED = "sim1_enabled";
    private static final String KEY_SIM2_ENABLED = "sim2_enabled";
    private static final String KEY_AUTO_START_ENABLED = "auto_start_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_SYNC_INTERVAL = "sync_interval_minutes";

    private PrefManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefManager(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        String token = getToken();
        String baseUrl = getBaseUrl();
        return token != null && !token.trim().isEmpty() && baseUrl != null && !baseUrl.trim().isEmpty();
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, "");
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null) {
            baseUrl = baseUrl.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
        }
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public void setToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getDeviceName() {
        return prefs.getString(KEY_DEVICE_NAME, android.os.Build.MODEL);
    }

    public void setDeviceName(String deviceName) {
        prefs.edit().putString(KEY_DEVICE_NAME, deviceName).apply();
    }

    public String getAdminFullName() {
        return prefs.getString(KEY_ADMIN_NAME, "Administrator");
    }

    public void setAdminFullName(String adminName) {
        prefs.edit().putString(KEY_ADMIN_NAME, adminName).apply();
    }

    public String getAdminEmail() {
        return prefs.getString(KEY_ADMIN_EMAIL, "");
    }

    public void setAdminEmail(String adminEmail) {
        prefs.edit().putString(KEY_ADMIN_EMAIL, adminEmail).apply();
    }

    public int getStoredCount() {
        return prefs.getInt(KEY_STORED_COUNT, 0);
    }

    public void setStoredCount(int count) {
        prefs.edit().putInt(KEY_STORED_COUNT, count).apply();
    }

    public int getUsedCount() {
        return prefs.getInt(KEY_USED_COUNT, 0);
    }

    public void setUsedCount(int count) {
        prefs.edit().putInt(KEY_USED_COUNT, count).apply();
    }

    public int getErrorCount() {
        return prefs.getInt(KEY_ERROR_COUNT, 0);
    }

    public void setErrorCount(int count) {
        prefs.edit().putInt(KEY_ERROR_COUNT, count).apply();
    }

    public String getServerErrorsJson() {
        return prefs.getString(KEY_SERVER_ERRORS, "[]");
    }

    public void setServerErrorsJson(String json) {
        prefs.edit().putString(KEY_SERVER_ERRORS, json).apply();
    }

    public String getServerStoredJson() {
        return prefs.getString(KEY_SERVER_STORED, "[]");
    }

    public void setServerStoredJson(String json) {
        prefs.edit().putString(KEY_SERVER_STORED, json).apply();
    }

    public String getServerUsedJson() {
        return prefs.getString(KEY_SERVER_USED, "[]");
    }

    public void setServerUsedJson(String json) {
        prefs.edit().putString(KEY_SERVER_USED, json).apply();
    }

    public boolean isServiceRunning() {
        return prefs.getBoolean(KEY_SERVICE_RUNNING, true);
    }

    public void setServiceRunning(boolean running) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply();
    }

    public String getLastSyncTime() {
        return prefs.getString(KEY_LAST_SYNC_TIME, "--");
    }

    public void setLastSyncTime(String time) {
        prefs.edit().putString(KEY_LAST_SYNC_TIME, time).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    public boolean isSim1Enabled() {
        return prefs.getBoolean(KEY_SIM1_ENABLED, true);
    }

    public void setSim1Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SIM1_ENABLED, enabled).apply();
    }

    public boolean isSim2Enabled() {
        return prefs.getBoolean(KEY_SIM2_ENABLED, true);
    }

    public void setSim2Enabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SIM2_ENABLED, enabled).apply();
    }

    public boolean isAutoStartEnabled() {
        return prefs.getBoolean(KEY_AUTO_START_ENABLED, true);
    }

    public void setAutoStartEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_START_ENABLED, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean darkMode) {
        prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
    }

    public int getSyncIntervalMinutes() {
        return prefs.getInt(KEY_SYNC_INTERVAL, 5);
    }

    public void setSyncIntervalMinutes(int minutes) {
        prefs.edit().putInt(KEY_SYNC_INTERVAL, minutes).apply();
    }

    public Set<String> getWhitelistedSenders() {
        Set<String> set = prefs.getStringSet(KEY_WHITELISTED_SENDERS, null);
        if (set == null || set.isEmpty()) {
            Set<String> defaults = new HashSet<>();
            defaults.add("bkash");
            defaults.add("nagad");
            defaults.add("16216");
            defaults.add("16167");
            defaults.add("rocket");
            defaults.add("upay");
            defaults.add("cellfin");
            defaults.add("ibbl");
            return defaults;
        }
        return set;
    }

    public void setWhitelistedSenders(List<String> senders) {
        Set<String> set = new HashSet<>();
        if (senders != null) {
            for (String s : senders) {
                if (s != null && !s.trim().isEmpty()) {
                    set.add(s.trim().toLowerCase());
                }
            }
        }
        prefs.edit().putStringSet(KEY_WHITELISTED_SENDERS, set).apply();
    }

    public boolean isSenderAllowed(String sender) {
        if (sender == null || sender.trim().isEmpty()) return false;
        String clean = sender.trim().toLowerCase();
        Set<String> list = getWhitelistedSenders();
        for (String w : list) {
            if (clean.contains(w) || w.contains(clean)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addOfflineSms(SmsItem item) {
        List<SmsItem> queue = getOfflineSmsQueue();
        queue.add(item);
        saveOfflineQueue(queue);
    }

    public synchronized List<SmsItem> getOfflineSmsQueue() {
        List<SmsItem> list = new ArrayList<>();
        String raw = prefs.getString(KEY_OFFLINE_SMS_QUEUE, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(SmsItem.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveOfflineQueue(List<SmsItem> list) {
        JSONArray arr = new JSONArray();
        if (list != null) {
            for (SmsItem item : list) {
                arr.put(item.toJsonObject());
            }
        }
        prefs.edit().putString(KEY_OFFLINE_SMS_QUEUE, arr.toString()).apply();
    }

    public synchronized void clearOfflineSmsQueue() {
        prefs.edit().putString(KEY_OFFLINE_SMS_QUEUE, "[]").apply();
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_ADMIN_NAME)
                .remove(KEY_ADMIN_EMAIL)
                .remove(KEY_STORED_COUNT)
                .remove(KEY_USED_COUNT)
                .remove(KEY_ERROR_COUNT)
                .remove(KEY_SERVER_ERRORS)
                .remove(KEY_SERVER_STORED)
                .remove(KEY_SERVER_USED)
                .putBoolean(KEY_SERVICE_RUNNING, false)
                .apply();
    }
}