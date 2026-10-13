package com.sanbong.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanbong.app.R;
import com.sanbong.app.model.NotificationItem;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> items;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<NotificationItem> items, OnNotificationClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvTime.setText(item.getTime());
        holder.ivIcon.setImageResource(item.getIconRes());
        holder.vUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

        holder.itemView.setOnClickListener(v -> {
            item.setRead(true);
            notifyItemChanged(position);
            if (listener != null) listener.onNotificationClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvBody;
        final TextView tvTime;
        final View vUnreadDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvBody = itemView.findViewById(R.id.tvNotifBody);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            vUnreadDot = itemView.findViewById(R.id.vUnreadDot);
        }
    }
}
