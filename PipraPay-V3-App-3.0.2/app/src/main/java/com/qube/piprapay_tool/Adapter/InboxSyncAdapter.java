package com.qube.piprapay_tool.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.qube.piprapay_tool.Model.SmsItem;
import com.qube.piprapay_tool.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InboxSyncAdapter extends RecyclerView.Adapter<InboxSyncAdapter.ViewHolder> {

    private final Context context;
    private List<SmsItem> items = new ArrayList<>();
    private final Set<Integer> selectedPositions = new HashSet<>();

    public InboxSyncAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<SmsItem> list) {
        this.items = list != null ? list : new ArrayList<>();
        selectedPositions.clear();
        for (int i = 0; i < this.items.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
    }

    public void selectAll(boolean selectAll) {
        selectedPositions.clear();
        if (selectAll) {
            for (int i = 0; i < items.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
    }

    public List<SmsItem> getSelectedItems() {
        List<SmsItem> selected = new ArrayList<>();
        for (int pos : selectedPositions) {
            if (pos >= 0 && pos < items.size()) {
                selected.add(items.get(pos));
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_inbox_select, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmsItem item = items.get(position);
        holder.tvInboxSender.setText(item.getSender());
        holder.tvInboxBody.setText(item.getMessage());

        try {
            long millis = Long.parseLong(item.getTimestamp()) * 1000L;
            holder.tvInboxDate.setText(new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date(millis)));
        } catch (Exception e) {
            holder.tvInboxDate.setText(item.getTimestamp());
        }

        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedPositions.contains(position));

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(position);
            } else {
                selectedPositions.remove(position);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            boolean current = holder.cbSelect.isChecked();
            holder.cbSelect.setChecked(!current);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvInboxSender;
        TextView tvInboxDate;
        TextView tvInboxBody;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvInboxSender = itemView.findViewById(R.id.tvInboxSender);
            tvInboxDate = itemView.findViewById(R.id.tvInboxDate);
            tvInboxBody = itemView.findViewById(R.id.tvInboxBody);
        }
    }
}