package com.qube.piprapay_tool.Fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.qube.piprapay_tool.Api.PipraPayApi;
import com.qube.piprapay_tool.Database.SmsLogDatabase;
import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.Model.SmsLogEntry;
import com.qube.piprapay_tool.R;
import com.qube.piprapay_tool.Utils.PrefManager;
import com.qube.piprapay_tool.Utils.SoundHelper;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class TestToolsFragment extends Fragment {

    private EditText etTestSender;
    private RadioGroup rgTestSim;
    private RadioButton rbSim1;
    private RadioButton rbSim2;
    private EditText etTestMessage;
    private MaterialButton btnPresetBkash;
    private MaterialButton btnPresetNagad;
    private MaterialButton btnPresetRocket;
    private MaterialButton btnGenerateNewTrx;
    private MaterialButton btnSendTestSms;
    private CardView cvTestResponse;
    private TextView tvTestResponse;

    private PrefManager pref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_tools, container, false);
        pref = PrefManager.getInstance(requireContext());

        initViews(view);
        setBkashPreset();

        return view;
    }

    private void initViews(View view) {
        etTestSender = view.findViewById(R.id.etTestSender);
        rgTestSim = view.findViewById(R.id.rgTestSim);
        rbSim1 = view.findViewById(R.id.rbSim1);
        rbSim2 = view.findViewById(R.id.rbSim2);
        etTestMessage = view.findViewById(R.id.etTestMessage);
        btnPresetBkash = view.findViewById(R.id.btnPresetBkash);
        btnPresetNagad = view.findViewById(R.id.btnPresetNagad);
        btnPresetRocket = view.findViewById(R.id.btnPresetRocket);
        btnGenerateNewTrx = view.findViewById(R.id.btnGenerateNewTrx);
        btnSendTestSms = view.findViewById(R.id.btnSendTestSms);
        cvTestResponse = view.findViewById(R.id.cvTestResponse);
        tvTestResponse = view.findViewById(R.id.tvTestResponse);

        btnPresetBkash.setOnClickListener(v -> setBkashPreset());
        btnPresetNagad.setOnClickListener(v -> setNagadPreset());
        btnPresetRocket.setOnClickListener(v -> setRocketPreset());
        btnGenerateNewTrx.setOnClickListener(v -> generateNewTrxInText());
        btnSendTestSms.setOnClickListener(v -> sendTestSms());
    }

    private String generateTrxId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("9K");
        Random rnd = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void setBkashPreset() {
        etTestSender.setText("bKash");
        String trx = generateTrxId();
        etTestMessage.setText("You have received Tk 500.00 from 01712345678. Ref: 10293847. Fee Tk 0.00. Balance Tk 15,230.00. TrxID " + trx + " at " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(new Date()));
    }

    private void setNagadPreset() {
        etTestSender.setText("NAGAD");
        String trx = generateTrxId();
        etTestMessage.setText("Merchant Payment Received. Amount: Tk 1,000.00. Sender: 01812345678. TxnID: " + trx + ". Balance: Tk 24,500.00. Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(new Date()));
    }

    private void setRocketPreset() {
        etTestSender.setText("16216");
        String trx = generateTrxId();
        etTestMessage.setText("Tk 300.00 received from 01912345678. A/C 019123456789. TxnID: " + trx + ". Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(new Date()) + ".");
    }

    private void generateNewTrxInText() {
        String current = etTestMessage.getText().toString();
        String newTrx = generateTrxId();
        if (current.contains("TrxID")) {
            current = current.replaceAll("(?i)TrxID\\s+[A-Z0-9]+", "TrxID " + newTrx);
        } else if (current.contains("TxnID")) {
            current = current.replaceAll("(?i)TxnID[:\\s]+[A-Z0-9]+", "TxnID: " + newTrx);
        } else {
            current += " TrxID " + newTrx;
        }
        etTestMessage.setText(current);
        Toast.makeText(getContext(), "Generated new TrxID: " + newTrx, Toast.LENGTH_SHORT).show();
    }

    private void sendTestSms() {
        String sender = etTestSender.getText().toString().trim();
        String message = etTestMessage.getText().toString().trim();
        String simSlot = rbSim2.isChecked() ? "2" : "1";

        if (TextUtils.isEmpty(sender)) {
            etTestSender.setError("Enter sender name");
            return;
        }
        if (TextUtils.isEmpty(message)) {
            etTestMessage.setError("Enter SMS body");
            return;
        }

        btnSendTestSms.setEnabled(false);
        btnSendTestSms.setText("Transmitting to Server...");

        long timestampMillis = System.currentTimeMillis();
        String id = String.valueOf(timestampMillis);
        String timestampSec = String.valueOf(timestampMillis / 1000);
        String formattedTime = new SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault()).format(new Date(timestampMillis));

        SmsItem item = new SmsItem(id, sender, message, simSlot, timestampSec);
        List<SmsItem> list = new ArrayList<>();
        list.add(item);

        SmsLogEntry logEntry = new SmsLogEntry(sender, message, simSlot, formattedTime, "SENT", "Test Simulation");
        SmsLogDatabase.getInstance(requireContext()).insertLog(logEntry);

        PipraPayApi.getInstance(requireContext()).transmitSms(list, new PipraPayApi.ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                if (!isAdded()) return;
                btnSendTestSms.setEnabled(true);
                btnSendTestSms.setText("Transmit Test SMS to Server");

                cvTestResponse.setVisibility(View.VISIBLE);
                try {
                    tvTestResponse.setText(response.toString(2));
                } catch (Exception e) {
                    tvTestResponse.setText(response.toString());
                }

                SoundHelper.playSuccessSound(requireContext());
                Toast.makeText(getContext(), "Simulation Success! Check PipraPay Admin Panel.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                btnSendTestSms.setEnabled(true);
                btnSendTestSms.setText("Transmit Test SMS to Server");

                cvTestResponse.setVisibility(View.VISIBLE);
                tvTestResponse.setText("ERROR: " + errorMessage);

                SoundHelper.playFailureSound(requireContext());
                Toast.makeText(getContext(), "Server Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}