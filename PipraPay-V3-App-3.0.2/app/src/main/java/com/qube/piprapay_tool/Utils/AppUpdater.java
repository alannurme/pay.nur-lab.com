package com.qube.piprapay_tool.Utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdater {
    private static final String TAG = "AppUpdater";
    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/samsusiyam/PipraPay-V3-App/releases/latest";

    public interface UpdateCheckCallback {
        void onNoUpdate();
        void onError(String error);
    }

    public static void checkForUpdates(Context context, boolean showToastIfNoUpdate, UpdateCheckCallback callback) {
        StringRequest req = new StringRequest(Request.Method.GET, GITHUB_API_LATEST,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        String latestTag = json.optString("tag_name", "").replace("v", "").trim();
                        String releaseNotes = json.optString("body", "Bug fixes and performance improvements.");
                        String currentVersion = getCurrentVersionName(context);

                        String downloadUrl = "";
                        JSONArray assets = json.optJSONArray("assets");
                        if (assets != null && assets.length() > 0) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                String name = asset.optString("name", "");
                                if (name.endsWith(".apk")) {
                                    downloadUrl = asset.optString("browser_download_url", "");
                                    break;
                                }
                            }
                        }

                        if (isNewerVersion(currentVersion, latestTag) && !downloadUrl.isEmpty()) {
                            final String finalDownloadUrl = downloadUrl;
                            final String finalTag = latestTag;
                            new Handler(Looper.getMainLooper()).post(() ->
                                    showUpdateDialog(context, finalTag, releaseNotes, finalDownloadUrl));
                        } else {
                            if (showToastIfNoUpdate) {
                                Toast.makeText(context, "You are using the latest version (v" + currentVersion + ")", Toast.LENGTH_SHORT).show();
                            }
                            if (callback != null) callback.onNoUpdate();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Update check error", e);
                        if (callback != null) callback.onError(e.getMessage());
                    }
                },
                error -> {
                    if (callback != null) callback.onError(error.getMessage() != null ? error.getMessage() : "Network error");
                });

        Volley.newRequestQueue(context).add(req);
    }

    private static String getCurrentVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName.replace("v", "").trim() : "3.0.0";
        } catch (Exception e) {
            return "3.0.0";
        }
    }

    private static boolean isNewerVersion(String current, String latest) {
        if (latest == null || latest.isEmpty()) return false;
        try {
            String[] curParts = current.split("\\.");
            String[] latParts = latest.split("\\.");
            int len = Math.max(curParts.length, latParts.length);
            for (int i = 0; i < len; i++) {
                int c = i < curParts.length ? Integer.parseInt(curParts[i]) : 0;
                int l = i < latParts.length ? Integer.parseInt(latParts[i]) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
        } catch (Exception e) {
            return !current.equalsIgnoreCase(latest);
        }
        return false;
    }

    private static void showUpdateDialog(Context context, String newVersion, String notes, String downloadUrl) {
        new AlertDialog.Builder(context)
                .setTitle("New Update Available! (v" + newVersion + ")")
                .setMessage("A new version of PipraPay Companion is available.\n\nRelease Notes:\n" + notes)
                .setPositiveButton("Update Now", (dialog, which) -> downloadAndInstallApk(context, downloadUrl))
                .setNegativeButton("Later", null)
                .setCancelable(false)
                .show();
    }

    private static void downloadAndInstallApk(Context context, String downloadUrl) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Downloading Update");
        progressDialog.setMessage("Please wait while downloading the new APK...");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned HTTP " + connection.getResponseCode());
                }

                int fileLength = connection.getContentLength();
                File outputDir = new File(context.getExternalCacheDir(), "updates");
                if (!outputDir.exists()) outputDir.mkdirs();
                File outputFile = new File(outputDir, "PipraPay-Update.apk");
                if (outputFile.exists()) outputFile.delete();

                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(outputFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        new Handler(Looper.getMainLooper()).post(() -> progressDialog.setProgress(progress));
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    progressDialog.dismiss();
                    installApk(context, outputFile);
                });

            } catch (Exception e) {
                Log.e(TAG, "Download APK failed", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    public static void installApk(Context context, File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Install failed", e);
            Toast.makeText(context, "Installation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}