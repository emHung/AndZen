package com.sanbong.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import com.sanbong.app.util.DateTimeUtils;
import com.sanbong.app.ui.adapter.BookingAdapter;

import java.util.List;

/**
 * Màn hình quản lý các lịch đặt của người dùng
 */
public class MyBookingsFragment extends Fragment implements BookingAdapter.OnCancelClickListener {

    private RecyclerView rvBookings;
    private View layoutEmptyBookings;
    private BookingAdapter bookingAdapter;

    private int selectedTab = 0; // 0: Sắp tới, 1: Đã hoàn thành, 2: Đã hủy
    private View tabUpcoming, tabCompleted, tabCancelled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_bookings, container, false);
        initViews(view);
        setupRecyclerView();
        setupTabs(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBookings();
    }

    private void initViews(View view) {
        rvBookings = view.findViewById(R.id.rvBookings);
        layoutEmptyBookings = view.findViewById(R.id.layoutEmptyBookings);
        View btnFind = view.findViewById(R.id.btnEmptyFindPitch);
        if (btnFind != null) {
            btnFind.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_pitches);
                }
            });
        }
        View btnNotify = view.findViewById(R.id.btnBookingNotification);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v ->
                    startActivity(new android.content.Intent(getContext(), NotificationActivity.class)));
        }
    }

    private void setupTabs(View view) {
        tabUpcoming = view.findViewById(R.id.tabUpcoming);
        tabCompleted = view.findViewById(R.id.tabCompleted);
        tabCancelled = view.findViewById(R.id.tabCancelled);

        if (tabUpcoming != null) tabUpcoming.setOnClickListener(v -> switchTab(0));
        if (tabCompleted != null) tabCompleted.setOnClickListener(v -> switchTab(1));
        if (tabCancelled != null) tabCancelled.setOnClickListener(v -> switchTab(2));
    }

    private void switchTab(int tab) {
        selectedTab = tab;
        loadBookings();
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingAdapter(this);
        bookingAdapter.setOnBookingClickListener(booking -> {
            android.content.Intent intent = new android.content.Intent(getContext(), BookingDetailActivity.class);
            intent.putExtra("booking", booking);
            startActivity(intent);
        });
        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBookings.setAdapter(bookingAdapter);
    }

    private void loadBookings() {
        List<Booking> all = BookingRepository.getInstance().getMyBookings();
        java.util.List<Booking> filtered = new java.util.ArrayList<>();
        String today = DateTimeUtils.getNextDays(1).get(0);
        for (Booking b : all) {
            int dateCompare = b.getDate().compareTo(today);
            if (selectedTab == 0 && b.getStatus() == BookingStatus.CONFIRMED && dateCompare >= 0) {
                filtered.add(b);
            } else if (selectedTab == 1 && b.getStatus() == BookingStatus.CONFIRMED && dateCompare < 0) {
                filtered.add(b);
            } else if (selectedTab == 2 && b.getStatus() == BookingStatus.CANCELLED) {
                filtered.add(b);
            }
        }
        bookingAdapter.updateData(filtered);
        boolean isEmpty = filtered.isEmpty();
        if (layoutEmptyBookings != null) layoutEmptyBookings.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvBookings != null) rvBookings.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCancelBooking(Booking booking) {
        showCancelConfirmDialog(booking);
    }

    private void showCancelConfirmDialog(Booking booking) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cancel_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvName = view.findViewById(R.id.tvDialogPitchName);
        TextView tvDateTime = view.findViewById(R.id.tvDialogDateTime);
        TextView tvAddress = view.findViewById(R.id.tvDialogAddress);
        View btnKeep = view.findViewById(R.id.btnKeepBooking);
        View btnConfirm = view.findViewById(R.id.btnConfirmCancel);

        tvName.setText(booking.getPitchName());
        tvDateTime.setText("📅 " + com.sanbong.app.util.DateTimeUtils.formatDisplayDate(booking.getDate()) + " | " + booking.getTimeRange());
        tvAddress.setText("📍 " + booking.getPitchAddress());

        btnKeep.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            performCancel(booking);
        });

        dialog.show();
    }

    private void performCancel(Booking booking) {
        boolean success = BookingRepository.getInstance().cancelBooking(booking.getBookingCode());
        if (success) {
            Toast.makeText(getContext(), "Đã hủy lịch đặt " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
            loadBookings();
        }
    }
}
