package com.sanbong.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sanbong.app.R;
import com.sanbong.app.util.DateTimeUtils;
import com.sanbong.app.data.ApiClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho thanh chọn ngày cuộn ngang
 */
public class DateChipAdapter extends RecyclerView.Adapter<DateChipAdapter.DateViewHolder> {
    public void close() { executor.shutdownNow(); }

    public interface OnDateSelectedListener {
        void onDateSelected(String isoDate);
    }

    private final String pitchId;
    private final List<String> dates = new ArrayList<>();
    private final OnDateSelectedListener listener;
    private int selectedPosition = 0;
    private final java.util.Map<String, Integer> slotCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    public DateChipAdapter(String pitchId, List<String> dateList, OnDateSelectedListener listener) {
        this.pitchId = pitchId;
        if (dateList != null) {
            this.dates.addAll(dateList);
        }
        this.listener = listener;
    }

    public void setSelectedDate(String isoDate) {
        int idx = dates.indexOf(isoDate);
        if (idx != -1) {
            int prev = selectedPosition;
            selectedPosition = idx;
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
        }
    }

    public void updateDates(List<String> newDates) {
        dates.clear();
        if (newDates != null) dates.addAll(newDates);
        selectedPosition = 0;
        notifyDataSetChanged();
    }

    public void setSlotCount(String isoDate, int count) {
        slotCounts.put(isoDate, Math.max(0, count));
        int position = dates.indexOf(isoDate);
        if (position >= 0) notifyItemChanged(position);
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date_chip, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        String isoDate = dates.get(position);
        holder.tvDateStatus.setTag(isoDate);
        boolean isSelected = (position == selectedPosition);
        Integer cachedCount = slotCounts.get(isoDate);
        int availableSlots = cachedCount == null ? -1 : cachedCount;
        if (cachedCount == null) {
            slotCounts.put(isoDate, -1);
            holder.tvDateStatus.setText("Đang tải...");
            holder.tvDateStatus.setTag(isoDate);
            executor.execute(() -> {
                try {
                    org.json.JSONArray rows = ApiClient.getArray(holder.itemView.getContext(),
                            "/v1/pitches/" + pitchId + "/slots?date=" + isoDate);
                    int count = 0;
                    for (int i = 0; i < rows.length(); i++) {
                        org.json.JSONObject row = rows.optJSONObject(i);
                        if (row != null && "available".equals(row.optString("status"))) count++;
                    }
                    slotCounts.put(isoDate, count);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && isoDate.equals(holder.tvDateStatus.getTag())) notifyItemChanged(pos);
                    });
                } catch (Exception ignored) {
                    slotCounts.put(isoDate, -2);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && isoDate.equals(holder.tvDateStatus.getTag())) notifyItemChanged(pos);
                    });
                }
            });
        }
        holder.bind(isoDate, availableSlots, isSelected, v -> {
            if (slotCounts.getOrDefault(isoDate, -1) == -2) {
                slotCounts.remove(isoDate);
                int retryPosition = holder.getBindingAdapterPosition();
                if (retryPosition != RecyclerView.NO_POSITION) notifyItemChanged(retryPosition);
            }
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onDateSelected(isoDate);
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardDateChip;
        private final TextView tvDateLabel;
        private final TextView tvDateStatus;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDateChip = itemView.findViewById(R.id.cardDateChip);
            tvDateLabel = itemView.findViewById(R.id.tvDateLabel);
            tvDateStatus = itemView.findViewById(R.id.tvDateStatus);
        }

        void bind(String isoDate, int availableSlots, boolean isSelected, View.OnClickListener onClickListener) {
            tvDateLabel.setText(DateTimeUtils.formatDisplayDate(isoDate));

            if (tvDateStatus != null) {
                if (availableSlots == -2) {
                    tvDateStatus.setText("Không tải được");
                    tvDateStatus.setTextColor(isSelected ? 0xFFC8E6C9 : 0xFF68776D);
                } else if (availableSlots < 0) {
                    tvDateStatus.setText("Đang tải...");
                    tvDateStatus.setTextColor(isSelected ? 0xFFC8E6C9 : 0xFF68776D);
                } else if (availableSlots > 0) {
                    tvDateStatus.setText("Còn " + availableSlots + " khung");
                    tvDateStatus.setTextColor(isSelected ? 0xFFC8E6C9 : 0xFF2E7D32);
                } else {
                    tvDateStatus.setText("Hết sân");
                    tvDateStatus.setTextColor(isSelected ? 0xFFFFCDD2 : 0xFFD32F2F);
                }
            }

            int contextBg = isSelected ? R.color.brand_700 : R.color.surface;
            int textColor = isSelected ? R.color.surface : R.color.text_primary;
            int strokeColor = isSelected ? R.color.brand_700 : (availableSlots > 0 ? R.color.border : R.color.border);

            cardDateChip.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), contextBg));
            cardDateChip.setStrokeColor(ContextCompat.getColor(itemView.getContext(), strokeColor));
            tvDateLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));

            cardDateChip.setOnClickListener(onClickListener);
        }
    }
}
