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
import com.sanbong.app.model.SlotStatus;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho lưới chọn khung giờ đặt sân
 */
public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.SlotViewHolder> {

    public interface OnSlotSelectedListener {
        void onSlotSelected(TimeSlot slot);
    }

    private final List<TimeSlot> slots = new ArrayList<>();
    private final OnSlotSelectedListener listener;
    private String selectedSlotId = null;

    public TimeSlotAdapter(OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    public void updateData(List<TimeSlot> newSlots) {
        String previousSelection = selectedSlotId;
        selectedSlotId = null;
        AdapterUpdates.replace(this, slots, newSlots, TimeSlot::getId);
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getId().equals(previousSelection)) notifyItemChanged(i);
        }
    }

    public void setSelectedSlotId(String slotId) {
        String previous = selectedSlotId;
        this.selectedSlotId = slotId;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getId().equals(previous) || slots.get(i).getId().equals(slotId)) notifyItemChanged(i);
        }
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);
        boolean isSelected = slot.getId().equals(selectedSlotId);
        holder.bind(slot, isSelected, v -> {
            if (slot.isAvailable()) {
                setSelectedSlotId(slot.getId());
                listener.onSlotSelected(slot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardSlot;
        private final TextView tvTimeRange;
        private final TextView tvSlotPrice;
        private final TextView tvSlotStatus;
        private final android.widget.ImageView ivSelectedCheck;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSlot = itemView.findViewById(R.id.cardTimeSlot);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            tvSlotPrice = itemView.findViewById(R.id.tvSlotPrice);
            tvSlotStatus = itemView.findViewById(R.id.tvSlotStatus);
            ivSelectedCheck = itemView.findViewById(R.id.ivSelectedCheck);
        }

        void bind(TimeSlot slot, boolean isSelected, View.OnClickListener onClickListener) {
            cardSlot.setSelected(isSelected);
            if (ivSelectedCheck != null) {
                ivSelectedCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            }
            tvTimeRange.setText(slot.getTimeRange());
            tvSlotPrice.setText(CurrencyUtils.formatVnd(slot.getPrice()));
            tvSlotStatus.setText(slot.getStatus().getLabel());

            int bgRes;
            int textRes;
            int strokeRes;

            if (isSelected) {
                bgRes = R.color.slot_selected_bg;
                textRes = R.color.slot_selected_text;
                strokeRes = R.color.slot_selected_bg;
            } else if (slot.getStatus() == SlotStatus.AVAILABLE) {
                bgRes = R.color.slot_available_bg;
                textRes = R.color.slot_available_text;
                strokeRes = R.color.slot_available_stroke;
            } else {
                bgRes = R.color.slot_booked_bg;
                textRes = R.color.slot_booked_text;
                strokeRes = R.color.slot_booked_stroke;
            }

            cardSlot.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), bgRes));
            cardSlot.setStrokeColor(ContextCompat.getColor(itemView.getContext(), strokeRes));
            tvTimeRange.setTextColor(ContextCompat.getColor(itemView.getContext(), textRes));
            tvSlotPrice.setTextColor(ContextCompat.getColor(itemView.getContext(), textRes));
            tvSlotStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), textRes));

            cardSlot.setOnClickListener(slot.isAvailable() ? onClickListener : null);
            cardSlot.setClickable(slot.isAvailable());
            cardSlot.setFocusable(slot.isAvailable());
        }
    }
}
