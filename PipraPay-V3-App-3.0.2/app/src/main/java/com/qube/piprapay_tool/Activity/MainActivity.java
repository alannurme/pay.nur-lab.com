package com.qube.piprapay_tool.Activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.qube.piprapay_tool.Fragment.DashboardFragment;
import com.qube.piprapay_tool.Fragment.SettingsFragment;
import com.qube.piprapay_tool.Fragment.SmsLogsFragment;
import com.qube.piprapay_tool.Fragment.TestToolsFragment;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Service.SmsReceiverService;
import com.qube.piprapay_tool.Utils.PrefManager;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final DashboardFragment dashboardFragment = new DashboardFragment();
    private final SmsLogsFragment smsLogsFragment = new SmsLogsFragment();
    private final TestToolsFragment testToolsFragment = new TestToolsFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefManager pref = PrefManager.getInstance(this);
        if (pref.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
        if (!pref.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                switchFragment(dashboardFragment);
                return true;
            } else if (itemId == R.id.nav_sms_logs) {
                switchFragment(smsLogsFragment);
                return true;
            } else if (itemId == R.id.nav_test_tools) {
                switchFragment(testToolsFragment);
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });

        // Default to Dashboard
        if (savedInstanceState == null) {
            switchFragment(dashboardFragment);
        }

        ensureServiceRunning();
    }

    private void switchFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.flContainer, fragment);
        ft.commit();
    }

    private void ensureServiceRunning() {
        PrefManager pref = PrefManager.getInstance(this);
        if (pref.isServiceRunning()) {
            Intent serviceIntent = new Intent(this, SmsReceiverService.class);
            serviceIntent.setAction(SmsReceiverService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }
}