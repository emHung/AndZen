package com.sanbong.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách các sân bóng
 */
public class PitchAdapter extends RecyclerView.Adapter<PitchAdapter.PitchViewHolder> {

    public interface OnPitchClickListener {
        void onPitchClick(Pitch pitch);
    }

    private final List<Pitch> pitchList = new ArrayList<>();
    private final OnPitchClickListener listener;

    public PitchAdapter(OnPitchClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Pitch> newPitches) {
        pitchList.clear();
        if (newPitches != null) {
            pitchList.addAll(newPitches);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PitchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pitch_card, parent, false);
        return new PitchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PitchViewHolder holder, int position) {
        Pitch pitch = pitchList.get(position);
        holder.bind(pitch, listener);
    }

    @Override
    public int getItemCount() {
        return pitchList.size();
    }

    static class PitchViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPitchName;
        private final TextView tvRating;
        private final TextView tvAddress;
        private final TextView tvPitchType;
        private final TextView tvAmenities;
        private final TextView tvPrice;
        private final TextView tvAvailableSlots;
        private final android.widget.ImageView ivPitchImage;
        private final android.widget.ImageView ivFavoritePitch;
        private final MaterialButton btnViewDetails;

        PitchViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPitchImage = itemView.findViewById(R.id.ivPitchImage);
            ivFavoritePitch = itemView.findViewById(R.id.ivFavorite);
            tvPitchName = itemView.findViewById(R.id.tvPitchName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvPitchType = itemView.findViewById(R.id.tvPitchType);
            tvAmenities = itemView.findViewById(R.id.tvAmenities);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvAvailableSlots = itemView.findViewById(R.id.tvAvailableSlots);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }

        void bind(Pitch pitch, OnPitchClickListener listener) {
            if (ivPitchImage != null && pitch.getImageResId() != 0) {
                ivPitchImage.setImageResource(pitch.getImageResId());
            }
            tvPitchName.setText(pitch.getName());
            tvRating.setText(String.valueOf(pitch.getRating()));
            tvAddress.setText(pitch.getAddress());
            tvPitchType.setText(pitch.getType().getDisplayName());
            tvAmenities.setText(String.join(" • ", pitch.getAmenities()));
            tvPrice.setText(CurrencyUtils.formatVnd(pitch.getMinPrice()) + " / trận");

            if (tvAvailableSlots != null) {
                String today = com.sanbong.app.util.DateTimeUtils.getNextDays(1).get(0);
                tvAvailableSlots.setText(BookingRepository.getInstance().getAvailabilityText(pitch.getId(), today));
            }

            if (ivFavoritePitch != null) {
                boolean isFav = BookingRepository.getInstance().isFavorite(pitch.getId());
                ivFavoritePitch.setImageResource(isFav ? R.drawable.ic_ri_heart_fill : R.drawable.ic_ri_heart);
                ivFavoritePitch.setColorFilter(isFav ? 0xFFE53935 : 0xFFFFFFFF);
                ivFavoritePitch.setOnClickListener(v -> {
                    boolean newFav = BookingRepository.getInstance().toggleFavorite(pitch.getId());
                    ivFavoritePitch.setImageResource(newFav ? R.drawable.ic_ri_heart_fill : R.drawable.ic_ri_heart);
                    ivFavoritePitch.setColorFilter(newFav ? 0xFFE53935 : 0xFFFFFFFF);
                    String msg = newFav ? "Đã thêm vào yêu thích: " : "Đã bỏ lưu: ";
                    android.widget.Toast.makeText(v.getContext(), msg + pitch.getName(), android.widget.Toast.LENGTH_SHORT).show();
                });
            }

            itemView.setOnClickListener(v -> listener.onPitchClick(pitch));
            btnViewDetails.setOnClickListener(v -> listener.onPitchClick(pitch));
        }
    }
}
