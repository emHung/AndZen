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

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho thanh chọn ngày cuộn ngang
 */
public class DateChipAdapter extends RecyclerView.Adapter<DateChipAdapter.DateViewHolder> {

    public interface OnDateSelectedListener {
        void onDateSelected(String isoDate);
    }

    private final String pitchId;
    private final List<String> dates = new ArrayList<>();
    private final OnDateSelectedListener listener;
    private int selectedPosition = 0;

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
        boolean isSelected = (position == selectedPosition);
        int availableSlots = com.sanbong.app.data.BookingRepository.getInstance()
                .getAvailableSlotsCount(pitchId, isoDate);
        holder.bind(isoDate, availableSlots, isSelected, v -> {
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
                if (availableSlots > 0) {
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
