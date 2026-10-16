package com.sanbong.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
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
    private final java.util.Set<String> favoriteIds = new java.util.HashSet<>();
    private final java.util.concurrent.ExecutorService favoriteExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

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

    public void setFavoriteIds(java.util.Set<String> ids) {
        favoriteIds.clear();
        if (ids != null) favoriteIds.addAll(ids);
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
        holder.bind(pitch, listener, favoriteIds, favoriteExecutor);
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

        void bind(Pitch pitch, OnPitchClickListener listener, java.util.Set<String> favoriteIds,
                  java.util.concurrent.ExecutorService favoriteExecutor) {
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
                tvAvailableSlots.setTag(pitch.getId());
                tvAvailableSlots.setText("Đang tải khung giờ...");
                favoriteExecutor.execute(() -> {
                    try {
                        org.json.JSONArray slots = ApiClient.getArray(itemView.getContext(),
                                "/v1/pitches/" + pitch.getId() + "/slots?date=" + today);
                        int count = 0;
                        for (int i = 0; i < slots.length(); i++)
                            if ("available".equals(slots.optJSONObject(i).optString("status"))) count++;
                        String label = count == 0 ? "Hết giờ trống" : count + " khung giờ còn trống";
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            if (pitch.getId().equals(tvAvailableSlots.getTag())) tvAvailableSlots.setText(label);
                        });
                    } catch (Exception exception) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            if (pitch.getId().equals(tvAvailableSlots.getTag())) tvAvailableSlots.setText("Mở sân để xem khung giờ");
                        });
                    }
                });
            }

            if (ivFavoritePitch != null) {
                boolean isFav = favoriteIds.contains(pitch.getId());
                ivFavoritePitch.setImageResource(isFav ? R.drawable.ic_ri_heart_fill : R.drawable.ic_ri_heart);
                ivFavoritePitch.setColorFilter(isFav ? 0xFFE53935 : 0xFFFFFFFF);
                ivFavoritePitch.setOnClickListener(v -> {
                    boolean newFav = !favoriteIds.contains(pitch.getId());
                    favoriteExecutor.execute(() -> {
                        try {
                            ApiClient.put(v.getContext(), "/v1/favorites/" + pitch.getId() + "?enabled=" + newFav, new org.json.JSONObject());
                            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
                            main.post(() -> {
                                if (newFav) favoriteIds.add(pitch.getId()); else favoriteIds.remove(pitch.getId());
                                ivFavoritePitch.setImageResource(newFav ? R.drawable.ic_ri_heart_fill : R.drawable.ic_ri_heart);
                                ivFavoritePitch.setColorFilter(newFav ? 0xFFE53935 : 0xFFFFFFFF);
                                String msg = newFav ? "Đã thêm vào yêu thích: " : "Đã bỏ lưu: ";
                                android.widget.Toast.makeText(v.getContext(), msg + pitch.getName(), android.widget.Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception exception) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                    android.widget.Toast.makeText(v.getContext(), exception.getMessage(), android.widget.Toast.LENGTH_LONG).show());
                        }
                    });
                });
            }

            itemView.setOnClickListener(v -> listener.onPitchClick(pitch));
            btnViewDetails.setOnClickListener(v -> listener.onPitchClick(pitch));
        }

    }
}
