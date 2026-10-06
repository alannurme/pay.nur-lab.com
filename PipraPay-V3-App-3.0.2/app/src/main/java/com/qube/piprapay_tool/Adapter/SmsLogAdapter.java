package com.qube.piprapay_tool.Adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.qube.piprapay_tool.Model.SmsLogEntry;
import com.qube.piprapay_tool.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.ViewHolder> {

    private final Context context;
    private List<SmsLogEntry> list = new ArrayList<>();

    public SmsLogAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<SmsLogEntry> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_sms_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsLogEntry item = list.get(position);
        String sender = item.getSender();
        String message = item.getMessage();
        String status = item.getStatus();

        holder.tvSenderName.setText(sender);
        holder.tvMessageSnippet.setText(message);
        holder.tvTimestamp.setText(item.getTimestamp());
        holder.tvSimBadge.setText("SIM " + item.getSimSlot());

        // Brand colors and avatar
        String cleanSender = sender.toLowerCase();
        if (cleanSender.contains("bkash")) {
            holder.tvSenderBadge.setText("bK");
            holder.tvSenderBadge.setTextColor(Color.parseColor("#E2136E"));
            holder.tvSenderBadge.setBackgroundColor(Color.parseColor("#FDE8F1"));
        } else if (cleanSender.contains("nagad")) {
            holder.tvSenderBadge.setText("NG");
            holder.tvSenderBadge.setTextColor(Color.parseColor("#F7941D"));
            holder.tvSenderBadge.setBackgroundColor(Color.parseColor("#FEF4E8"));
        } else if (cleanSender.contains("rocket") || cleanSender.contains("16216")) {
            holder.tvSenderBadge.setText("RK");
            holder.tvSenderBadge.setTextColor(Color.parseColor("#8C3494"));
            holder.tvSenderBadge.setBackgroundColor(Color.parseColor("#F6EBF8"));
        } else if (cleanSender.contains("upay")) {
            holder.tvSenderBadge.setText("UP");
            holder.tvSenderBadge.setTextColor(Color.parseColor("#1B365D"));
            holder.tvSenderBadge.setBackgroundColor(Color.parseColor("#FFF8D6"));
        } else {
            holder.tvSenderBadge.setText(sender.length() > 2 ? sender.substring(0, 2).toUpperCase() : sender.toUpperCase());
            holder.tvSenderBadge.setTextColor(Color.parseColor("#29a56c"));
            holder.tvSenderBadge.setBackgroundColor(Color.parseColor("#E8F8F0"));
        }

        // Status badge styling
        if ("SENT".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("● Forwarded");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#29a56c"));
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#E8F8F0"));
        } else if ("QUEUED".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("● Queued");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#F7941D"));
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF4E8"));
        } else if ("FILTERED".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText("● Ignored");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#78828A"));
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#F4F6FA"));
        } else {
            holder.tvStatusBadge.setText("● Failed");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#DD5050"));
            holder.tvStatusBadge.setBackgroundColor(Color.parseColor("#FDE8E8"));
        }

        holder.itemView.setOnClickListener(v -> showDetailDialog(item));
    }

    private void showDetailDialog(SmsLogEntry item) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sms_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        TextView tvDetailSender = dialogView.findViewById(R.id.tvDetailSender);
        TextView tvDetailSim = dialogView.findViewById(R.id.tvDetailSim);
        TextView tvDetailTime = dialogView.findViewById(R.id.tvDetailTime);
        TextView tvDetailMessage = dialogView.findViewById(R.id.tvDetailMessage);
        TextView tvDetailStatus = dialogView.findViewById(R.id.tvDetailStatus);
        MaterialButton btnCopyTrxId = dialogView.findViewById(R.id.btnCopyTrxId);
        MaterialButton btnCopyFullMessage = dialogView.findViewById(R.id.btnCopyFullMessage);

        tvDetailSender.setText(item.getSender() + " Message");
        tvDetailSim.setText("SIM " + item.getSimSlot());
        tvDetailTime.setText("Timestamp: " + item.getTimestamp());
        tvDetailMessage.setText(item.getMessage());
        tvDetailStatus.setText(item.getResponseMessage());

        String trxId = extractTrxId(item.getMessage());

        btnCopyTrxId.setOnClickListener(v -> {
            if (!trxId.isEmpty()) {
                copyToClipboard("TrxID", trxId);
                Toast.makeText(context, "TrxID copied: " + trxId, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "No TrxID found in this message", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyFullMessage.setOnClickListener(v -> {
            copyToClipboard("SMS Message", item.getMessage());
            Toast.makeText(context, "Full SMS copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private String extractTrxId(String msg) {
        if (msg == null) return "";
        // Match TrxID or Transaction ID patterns
        Pattern pattern = Pattern.compile("(?i)(?:TrxID|TxnId|Transaction ID|Trx ID|Trans ID)[:\\s]+([A-Z0-9]+)");
        Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderBadge;
        TextView tvSenderName;
        TextView tvSimBadge;
        TextView tvTimestamp;
        TextView tvStatusBadge;
        TextView tvMessageSnippet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderBadge = itemView.findViewById(R.id.tvSenderBadge);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvSimBadge = itemView.findViewById(R.id.tvSimBadge);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvMessageSnippet = itemView.findViewById(R.id.tvMessageSnippet);
        }
    }
}