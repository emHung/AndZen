package com.sanbong.app.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.ui.PitchDetailActivity;
import com.sanbong.app.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class FavoritePitchAdapter extends RecyclerView.Adapter<FavoritePitchAdapter.ViewHolder> {

    public interface OnFavoriteChangeListener {
        void onFavoriteChanged();
    }

    private final List<Pitch> pitchList = new ArrayList<>();
    private final OnFavoriteChangeListener changeListener;

    public FavoritePitchAdapter(OnFavoriteChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void updateData(List<Pitch> list) {
        pitchList.clear();
        if (list != null) pitchList.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_pitch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pitch pitch = pitchList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(pitch.getName());
        holder.tvRating.setText(pitch.getRating() + " (256)");
        holder.tvAddress.setText(pitch.getDistrict() + ", Hà Nội");
        holder.tvTypeTag.setText(pitch.getType().getDisplayName());
        holder.tvPrice.setText(CurrencyUtils.formatVnd(pitch.getMinPrice()));

        int imgRes = pitch.getImageResId() != 0 ? pitch.getImageResId() : R.drawable.img_pitch_sunset;
        holder.ivImage.setImageResource(imgRes);

        View.OnClickListener openDetail = v -> {
            Intent intent = new Intent(context, PitchDetailActivity.class);
            intent.putExtra("pitch", pitch);
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(openDetail);
        holder.btnBookNow.setOnClickListener(openDetail);

        holder.btnToggle.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < pitchList.size()) {
                Pitch target = pitchList.get(pos);
                BookingRepository.getInstance().toggleFavorite(target.getId());
                Toast.makeText(context, "Đã bỏ lưu: " + target.getName(), Toast.LENGTH_SHORT).show();
                pitchList.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, pitchList.size());
                if (changeListener != null) {
                    changeListener.onFavoriteChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return pitchList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final TextView tvName, tvRating, tvAddress, tvTypeTag, tvPrice;
        final View btnToggle, btnBookNow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivFavPitchImage);
            tvName = itemView.findViewById(R.id.tvFavPitchName);
            tvRating = itemView.findViewById(R.id.tvFavRating);
            tvAddress = itemView.findViewById(R.id.tvFavAddress);
            tvTypeTag = itemView.findViewById(R.id.tvFavTypeTag);
            tvPrice = itemView.findViewById(R.id.tvFavPrice);
            btnToggle = itemView.findViewById(R.id.btnFavToggle);
            btnBookNow = itemView.findViewById(R.id.btnFavBookNow);
        }
    }
}
