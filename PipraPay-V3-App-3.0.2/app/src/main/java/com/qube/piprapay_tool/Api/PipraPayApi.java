package com.qube.piprapay_tool.Api;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.Utils.PrefManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipraPayApi {
    private static final String TAG = "PipraPayApi";
    private static PipraPayApi instance;
    private final RequestQueue requestQueue;
    private final Context context;

    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }

    private PipraPayApi(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    public static synchronized PipraPayApi getInstance(Context context) {
        if (instance == null) {
            instance = new PipraPayApi(context);
        }
        return instance;
    }

    private String normalizeUrl(String baseUrl) {
        if (baseUrl == null) return "";
        baseUrl = baseUrl.trim();
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl;
    }

    public void login(String baseUrl, String otp, String deviceName, ApiCallback<JSONObject> callback) {
        String url = normalizeUrl(baseUrl);
        PrefManager pref = PrefManager.getInstance(context);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.optBoolean("status", false) || json.has("token")) {
                            String token = json.optString("token", "");
                            pref.setBaseUrl(baseUrl);
                            pref.setToken(token);
                            pref.setDeviceName(deviceName);
                            pref.setServiceRunning(true);
                            if (callback != null) callback.onSuccess(json);
                        } else {
                            String msg = json.optString("message", "Pairing rejected by server");
                            if (callback != null) callback.onError(msg);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Login JSON parse error: " + response, e);
                        if (callback != null) callback.onError("Invalid server response: " + response);
                    }
                },
                error -> {
                    String msg = error.getMessage() != null ? error.getMessage() : "Network error. Please check server URL.";
                    if (callback != null) callback.onError(msg);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action-companion", "login");
                params.put("onetimepassword", otp);
                params.put("name", deviceName);
                params.put("model", Build.MODEL);
                params.put("android_level", String.valueOf(Build.VERSION.SDK_INT));
                params.put("app_version", "3.0.2");
                return params;
            }
        };

        postRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    public void getAccountInfo(ApiCallback<JSONObject> callback) {
        PrefManager pref = PrefManager.getInstance(context);
        getAccountInfo(pref.getBaseUrl(), pref.getToken(), callback);
    }

    public void getAccountInfo(String baseUrl, String token, ApiCallback<JSONObject> callback) {
        String url = normalizeUrl(baseUrl);
        PrefManager pref = PrefManager.getInstance(context);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.has("fullname")) {
                            pref.setAdminFullName(json.optString("fullname"));
                        }
                        if (json.has("email")) {
                            pref.setAdminEmail(json.optString("email"));
                        }
                        if (json.has("stored_count")) {
                            pref.setStoredCount(json.optInt("stored_count", 0));
                        }
                        if (json.has("used_count")) {
                            pref.setUsedCount(json.optInt("used_count", 0));
                        }
                        if (json.has("error_count")) {
                            pref.setErrorCount(json.optInt("error_count", 0));
                        }
                        if (json.has("error")) {
                            JSONArray errArr = json.optJSONArray("error");
                            pref.setServerErrorsJson(errArr != null ? errArr.toString() : "[]");
                        }
                        if (json.has("stored")) {
                            JSONArray storedArr = json.optJSONArray("stored");
                            pref.setServerStoredJson(storedArr != null ? storedArr.toString() : "[]");
                        }
                        if (json.has("used")) {
                            JSONArray usedArr = json.optJSONArray("used");
                            pref.setServerUsedJson(usedArr != null ? usedArr.toString() : "[]");
                        }
                        if (callback != null) callback.onSuccess(json);
                    } catch (Exception e) {
                        if (callback != null) callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    if (callback != null) callback.onError(error.getMessage() != null ? error.getMessage() : "Network error");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action-companion", "account-information");
                params.put("token", token);
                return params;
            }
        };

        postRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    public void getWhitelistedSenders(ApiCallback<JSONObject> callback) {
        PrefManager pref = PrefManager.getInstance(context);
        getWhitelistedSenders(pref.getBaseUrl(), pref.getToken(), callback);
    }

    public void getWhitelistedSenders(String baseUrl, String token, ApiCallback<JSONObject> callback) {
        String url = normalizeUrl(baseUrl);
        PrefManager pref = PrefManager.getInstance(context);

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.has("senders")) {
                            JSONArray arr = json.getJSONArray("senders");
                            List<String> senders = new ArrayList<>();
                            for (int i = 0; i < arr.length(); i++) {
                                senders.add(arr.getString(i));
                            }
                            pref.setWhitelistedSenders(senders);
                        }
                        if (callback != null) callback.onSuccess(json);
                    } catch (Exception e) {
                        if (callback != null) callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    if (callback != null) callback.onError(error.getMessage() != null ? error.getMessage() : "Network error");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action-companion", "sms-transmit-sender");
                params.put("token", token);
                return params;
            }
        };

        postRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }

    public void transmitSms(List<SmsItem> items, ApiCallback<JSONObject> callback) {
        PrefManager pref = PrefManager.getInstance(context);
        transmitSms(pref.getBaseUrl(), pref.getToken(), items, callback);
    }

    public void transmitSms(String baseUrl, String token, List<SmsItem> items, ApiCallback<JSONObject> callback) {
        String url = normalizeUrl(baseUrl);
        JSONArray arr = new JSONArray();
        for (SmsItem item : items) {
            arr.put(item.toJsonObject());
        }
        final String smsListJson = arr.toString();

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (callback != null) callback.onSuccess(json);
                    } catch (Exception e) {
                        Log.e(TAG, "Transmit SMS parse error: " + response, e);
                        if (callback != null) callback.onError("Parse error: " + response);
                    }
                },
                error -> {
                    if (callback != null) callback.onError(error.getMessage() != null ? error.getMessage() : "Network error");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action-companion", "sms-transmit-bulk");
                params.put("token", token);
                params.put("sms_list", smsListJson);
                return params;
            }
        };

        postRequest.setRetryPolicy(new DefaultRetryPolicy(20000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(postRequest);
    }
}