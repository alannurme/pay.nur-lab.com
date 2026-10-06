package com.qube.piprapay_tool.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Service.SmsReceiverService;
import com.qube.piprapay_tool.Utils.PrefManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 1001;

    private EditText etServerUrl;
    private EditText etOtp;
    private EditText etDeviceName;
    private MaterialButton btnScanQr;
    private MaterialButton btnConnect;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    String scannedContent = result.getContents().trim();
                    parseAndPopulateQr(scannedContent);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply Theme
        PrefManager pref = PrefManager.getInstance(this);
        if (pref.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        // If already logged in, go directly to MainActivity
        if (pref.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        checkAndRequestPermissions();
    }

    private void initViews() {
        etServerUrl = findViewById(R.id.etServerUrl);
        etOtp = findViewById(R.id.etOtp);
        etDeviceName = findViewById(R.id.etDeviceName);
        btnScanQr = findViewById(R.id.btnScanQr);
        btnConnect = findViewById(R.id.btnConnect);
        progressBar = findViewById(R.id.progressBar);

        // Pre-fill if previously used
        PrefManager pref = PrefManager.getInstance(this);
        if (!TextUtils.isEmpty(pref.getBaseUrl())) {
            etServerUrl.setText(pref.getBaseUrl());
        }
        if (!TextUtils.isEmpty(pref.getDeviceName())) {
            etDeviceName.setText(pref.getDeviceName());
        }

        btnScanQr.setOnClickListener(v -> launchQrScanner());
        btnConnect.setOnClickListener(v -> performLogin());
    }

    private void launchQrScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQ_CODE);
            return;
        }

        ScanOptions options = new ScanOptions();
        options.setPrompt("Align the QR code inside the frame");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setBarcodeImageEnabled(false);
        options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity.class);
        barcodeLauncher.launch(options);
    }

    private void parseAndPopulateQr(String raw) {
        // PipraPay V3 QR Code format: baseUrl----otp
        if (raw.contains("----")) {
            String[] parts = raw.split("----");
            if (parts.length >= 2) {
                etServerUrl.setText(parts[0].trim());
                etOtp.setText(parts[1].trim());
                Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show();
                performLogin();
                return;
            }
        }
        
        // If it's a URL or OTP alone
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            etServerUrl.setText(raw);
        } else {
            etOtp.setText(raw);
        }
    }

    private void performLogin() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String deviceName = etDeviceName.getText().toString().trim();

        if (TextUtils.isEmpty(serverUrl)) {
            etServerUrl.setError("Please enter PipraPay server URL");
            etServerUrl.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(otp)) {
            etOtp.setError("Please enter One Time Password (OTP)");
            etOtp.requestFocus();
            return;
        }

        setLoading(true);

        PipraPayApi.getInstance(this).login(serverUrl, otp, deviceName, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Pairing successful! Welcome to PipraPay V3.", Toast.LENGTH_LONG).show();

                // Start Foreground Service
                startForegroundGatewayService();

                // Navigate to Dashboard
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Pairing Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startForegroundGatewayService() {
        Intent serviceIntent = new Intent(this, SmsReceiverService.class);
        serviceIntent.setAction(SmsReceiverService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnConnect.setEnabled(!loading);
        btnScanQr.setEnabled(!loading);
    }

    private void checkAndRequestPermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.RECEIVE_SMS);
        requiredPermissions.add(Manifest.permission.READ_SMS);
        requiredPermissions.add(Manifest.permission.READ_PHONE_STATE);
        requiredPermissions.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "SMS & Camera permissions are required for PipraPay to function properly.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
