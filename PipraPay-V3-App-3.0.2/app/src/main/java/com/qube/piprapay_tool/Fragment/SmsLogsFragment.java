package com.qube.piprapay_tool.Fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.qube.piprapay_tool.Adapter.SmsLogAdapter;
import com.qube.piprapay_tool.Database.SmsLogDatabase;
import com.qube.piprapay_tool.Model.SmsLogEntry;
import com.qube.piprapay_tool.R;

import java.util.ArrayList;
import java.util.List;

public class SmsLogsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLogs;
    private RecyclerView rvSmsLogs;
    private LinearLayout llEmptyLogs;
    private EditText etSearchLogs;
    private TextView btnClearLogs;

    private SmsLogAdapter adapter;
    private List<SmsLogEntry> allLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sms_logs, container, false);

        initViews(view);
        loadLogs();

        return view;
    }

    private void initViews(View view) {
        swipeRefreshLogs = view.findViewById(R.id.swipeRefreshLogs);
        rvSmsLogs = view.findViewById(R.id.rvSmsLogs);
        llEmptyLogs = view.findViewById(R.id.llEmptyLogs);
        etSearchLogs = view.findViewById(R.id.etSearchLogs);
        btnClearLogs = view.findViewById(R.id.btnClearLogs);

        adapter = new SmsLogAdapter(requireContext());
        rvSmsLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSmsLogs.setAdapter(adapter);

        swipeRefreshLogs.setColorSchemeResources(R.color.main_color);
        swipeRefreshLogs.setOnRefreshListener(this::loadLogs);

        btnClearLogs.setOnClickListener(v -> confirmClearLogs());

        etSearchLogs.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLogs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void loadLogs() {
        if (!isAdded()) return;

        swipeRefreshLogs.setRefreshing(true);
        allLogs = SmsLogDatabase.getInstance(requireContext()).getAllLogs();
        adapter.setData(allLogs);
        swipeRefreshLogs.setRefreshing(false);

        llEmptyLogs.setVisibility(allLogs.isEmpty() ? View.VISIBLE : View.GONE);
        rvSmsLogs.setVisibility(allLogs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void filterLogs(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setData(allLogs);
            return;
        }

        String lower = query.trim().toLowerCase();
        List<SmsLogEntry> filtered = new ArrayList<>();
        for (SmsLogEntry entry : allLogs) {
            if (entry.getSender().toLowerCase().contains(lower) ||
                entry.getMessage().toLowerCase().contains(lower) ||
                entry.getStatus().toLowerCase().contains(lower)) {
                filtered.add(entry);
            }
        }
        adapter.setData(filtered);
    }

    private void confirmClearLogs() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear SMS Logs")
                .setMessage("Are you sure you want to delete all local SMS history? This will not affect the server.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    SmsLogDatabase.getInstance(requireContext()).clearLogs();
                    loadLogs();
                    Toast.makeText(getContext(), "Logs cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLogs();
    }
}