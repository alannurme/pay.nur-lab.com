package com.qube.piprapay_tool.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.qube.piprapay_tool.Adapter.InboxSyncAdapter;
import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.Database.SmsLogDatabase;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.Model.SmsLogEntry;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Utils.InboxSmsScanner;
import com.qube.piprapay_tool.Utils.PrefManager;
import com.qube.piprapay_tool.Utils.SoundHelper;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InboxSyncActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvScanCount;
    private MaterialButton btnFilter24h;
    private MaterialButton btnFilter3d;
    private MaterialButton btnFilter7d;
    private CheckBox cbSelectAll;
    private RecyclerView rvInboxSms;
    private ProgressBar pbScanning;
    private TextView tvEmptyInbox;
    private MaterialButton btnPushSelected;

    private InboxSyncAdapter adapter;
    private long selectedTimeRangeMillis = 24 * 60 * 60 * 1000L; // default 24h

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox_sync);

        initViews();
        performScan();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvScanCount = findViewById(R.id.tvScanCount);
        btnFilter24h = findViewById(R.id.btnFilter24h);
        btnFilter3d = findViewById(R.id.btnFilter3d);
        btnFilter7d = findViewById(R.id.btnFilter7d);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        rvInboxSms = findViewById(R.id.rvInboxSms);
        pbScanning = findViewById(R.id.pbScanning);
        tvEmptyInbox = findViewById(R.id.tvEmptyInbox);
        btnPushSelected = findViewById(R.id.btnPushSelected);

        adapter = new InboxSyncAdapter(this);
        rvInboxSms.setLayoutManager(new LinearLayoutManager(this));
        rvInboxSms.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnFilter24h.setOnClickListener(v -> {
            selectedTimeRangeMillis = 24 * 60 * 60 * 1000L;
            highlightFilterButton(btnFilter24h);
            performScan();
        });

        btnFilter3d.setOnClickListener(v -> {
            selectedTimeRangeMillis = 3 * 24 * 60 * 60 * 1000L;
            highlightFilterButton(btnFilter3d);
            performScan();
        });

        btnFilter7d.setOnClickListener(v -> {
            selectedTimeRangeMillis = 7 * 24 * 60 * 60 * 1000L;
            highlightFilterButton(btnFilter7d);
            performScan();
        });

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });

        btnPushSelected.setOnClickListener(v -> pushSelectedSms());
    }

    private void highlightFilterButton(MaterialButton selected) {
        int activeBg = getResources().getColor(R.color.main_color);
        int inactiveBg = getResources().getColor(R.color.greyF6Ma);
        int activeText = getResources().getColor(R.color.white);
        int inactiveText = getResources().getColor(R.color.black1F);

        btnFilter24h.setBackgroundColor(inactiveBg);
        btnFilter24h.setTextColor(inactiveText);
        btnFilter3d.setBackgroundColor(inactiveBg);
        btnFilter3d.setTextColor(inactiveText);
        btnFilter7d.setBackgroundColor(inactiveBg);
        btnFilter7d.setTextColor(inactiveText);

        selected.setBackgroundColor(activeBg);
        selected.setTextColor(activeText);
    }

    private void performScan() {
        pbScanning.setVisibility(View.VISIBLE);
        tvEmptyInbox.setVisibility(View.GONE);
        rvInboxSms.setVisibility(View.GONE);

        new Thread(() -> {
            long since = System.currentTimeMillis() - selectedTimeRangeMillis;
            List<SmsItem> found = InboxSmsScanner.scanInbox(this, since, 200);

            runOnUiThread(() -> {
                pbScanning.setVisibility(View.GONE);
                adapter.setItems(found);
                tvScanCount.setText("Found " + found.size() + " matching payment messages");

                if (found.isEmpty()) {
                    tvEmptyInbox.setVisibility(View.VISIBLE);
                    rvInboxSms.setVisibility(View.GONE);
                    btnPushSelected.setEnabled(false);
                } else {
                    tvEmptyInbox.setVisibility(View.GONE);
                    rvInboxSms.setVisibility(View.VISIBLE);
                    btnPushSelected.setEnabled(true);
                    btnPushSelected.setText("Sync " + found.size() + " Messages to Server");
                }
            });
        }).start();
    }

    private void pushSelectedSms() {
        List<SmsItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select at least one message to sync.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPushSelected.setEnabled(false);
        btnPushSelected.setText("Syncing " + selected.size() + " Messages...");

        PipraPayApi.getInstance(this).transmitSms(selected, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                btnPushSelected.setEnabled(true);
                btnPushSelected.setText("Sync Complete!");

                // Save to local logs
                for (SmsItem item : selected) {
                    long millis = Long.parseLong(item.getTimestamp()) * 1000L;
                    String formatted = new SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault()).format(new Date(millis));
                    SmsLogEntry log = new SmsLogEntry(item.getSender(), item.getMessage(), item.getSimSlot(), formatted, "SENT", "Imported from Inbox");
                    SmsLogDatabase.getInstance(InboxSyncActivity.this).insertLog(log);
                }

                PrefManager pref = PrefManager.getInstance(InboxSyncActivity.this);
                pref.setStoredCount(pref.getStoredCount() + selected.size());

                SoundHelper.playSuccessSound(InboxSyncActivity.this);
                Toast.makeText(InboxSyncActivity.this, "Successfully synced " + selected.size() + " SMS to PipraPay Server!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                btnPushSelected.setEnabled(true);
                btnPushSelected.setText("Sync Failed. Retry");

                SoundHelper.playFailureSound(InboxSyncActivity.this);
                Toast.makeText(InboxSyncActivity.this, "Server Sync Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}