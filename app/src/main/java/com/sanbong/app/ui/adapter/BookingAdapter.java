package com.sanbong.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.sanbong.app.R;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách các lịch đặt sân của người dùng
 */
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnCancelClickListener {
        void onCancelBooking(Booking booking);
    }

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    private final List<Booking> bookings = new ArrayList<>();
    private final OnCancelClickListener cancelListener;
    private OnBookingClickListener itemClickListener;

    public BookingAdapter(OnCancelClickListener cancelListener) {
        this.cancelListener = cancelListener;
    }

    public void setOnBookingClickListener(OnBookingClickListener listener) {
        this.itemClickListener = listener;
    }

    public void updateData(List<Booking> newBookings) {
        bookings.clear();
        if (newBookings != null) {
            bookings.addAll(newBookings);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        holder.bind(booking, cancelListener, itemClickListener);
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onBookingClick(booking);
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBookingCode, tvBookingStatus, tvPitchName, tvBookingAddress;
        private final TextView tvBookingDate, tvBookingTime, tvCustomerInfo, tvTotalPrice;
        private final MaterialButton btnCancelBooking, btnDetailsBooking;
        private final ImageView ivBookingBanner;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingCode = itemView.findViewById(R.id.tvBookingCode);
            tvBookingStatus = itemView.findViewById(R.id.tvBookingStatus);
            tvPitchName = itemView.findViewById(R.id.tvPitchName);
            tvBookingAddress = itemView.findViewById(R.id.tvBookingAddress);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvBookingTime = itemView.findViewById(R.id.tvBookingTime);
            tvCustomerInfo = itemView.findViewById(R.id.tvCustomerInfo);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            btnCancelBooking = itemView.findViewById(R.id.btnCancelBooking);
            btnDetailsBooking = itemView.findViewById(R.id.btnDetailsBooking);
            ivBookingBanner = itemView.findViewById(R.id.ivBookingBanner);
        }

        void bind(Booking booking, OnCancelClickListener cancelListener, OnBookingClickListener itemClickListener) {
            tvBookingCode.setText("Mã: " + booking.getBookingCode());
            tvPitchName.setText(booking.getPitchName());
            ((TextView) itemView.findViewById(R.id.tvBookingType)).setText(
                    booking.getPitch() == null ? "Sân bóng" : booking.getPitch().getType().getDisplayName());
            ivBookingBanner.setImageResource(booking.getPitch() == null ? 0 : booking.getPitch().getImageResId());
            if (tvBookingAddress != null) tvBookingAddress.setText(booking.getPitchAddress());
            if (tvBookingDate != null) tvBookingDate.setText(DateTimeUtils.formatDisplayDate(booking.getDate()));
            if (tvBookingTime != null) tvBookingTime.setText(booking.getTimeRange());
            tvCustomerInfo.setText(booking.getCustomerPhone());
            tvTotalPrice.setText(CurrencyUtils.formatVnd(booking.getTotalPrice()));

            boolean isConfirmed = (booking.getStatus() == BookingStatus.CONFIRMED);
            tvBookingStatus.setText(isConfirmed && DateTimeUtils.hasBookingEnded(booking.getDate(), booking.getTimeRange())
                    ? "Đã hoàn thành" : booking.getStatus().getTitle());
            int statusColor = isConfirmed ? R.color.primary_green : R.color.text_muted;
            tvBookingStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColor));

            btnCancelBooking.setVisibility(isConfirmed && !DateTimeUtils.hasBookingStarted(booking.getDate(), booking.getTimeRange()) ? View.VISIBLE : View.GONE);
            btnCancelBooking.setOnClickListener(v -> cancelListener.onCancelBooking(booking));

            if (btnDetailsBooking != null) {
                btnDetailsBooking.setOnClickListener(v -> {
                    if (itemClickListener != null) itemClickListener.onBookingClick(booking);
                });
            }
        }
    }
}
