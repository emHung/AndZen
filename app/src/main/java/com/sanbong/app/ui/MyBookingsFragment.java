package com.sanbong.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.BookingMapper;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import com.sanbong.app.util.DateTimeUtils;
import com.sanbong.app.ui.adapter.BookingAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/**
 * Màn hình quản lý các lịch đặt của người dùng
 */
public class MyBookingsFragment extends Fragment implements BookingAdapter.OnCancelClickListener {

    private RecyclerView rvBookings;
    private View layoutEmptyBookings;
    private BookingAdapter bookingAdapter;

    private int selectedTab = 0; // 0: Sắp tới, 1: Đã hoàn thành, 2: Đã hủy
    private View tabUpcoming, tabCompleted, tabCancelled;
    private TextView tvUpcomingCount;
    private final ExecutorService bookingExecutor = Executors.newSingleThreadExecutor();

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
        tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);

        if (tabUpcoming != null) tabUpcoming.setOnClickListener(v -> switchTab(0));
        if (tabCompleted != null) tabCompleted.setOnClickListener(v -> switchTab(1));
        if (tabCancelled != null) tabCancelled.setOnClickListener(v -> switchTab(2));
        renderSelectedTab();
    }

    private void switchTab(int tab) {
        selectedTab = tab;
        renderSelectedTab();
        loadBookings();
    }

    private void renderSelectedTab() {
        styleTab(tabUpcoming, selectedTab == 0);
        styleTab(tabCompleted, selectedTab == 1);
        styleTab(tabCancelled, selectedTab == 2);
    }

    private void styleTab(View tab, boolean selected) {
        if (tab == null || getContext() == null) return;

        int backgroundColor = getResources().getColor(
                selected ? R.color.slot_available_bg : R.color.surface, requireContext().getTheme());
        int textColor = getResources().getColor(
                selected ? R.color.primary_green : R.color.text_secondary, requireContext().getTheme());
        tab.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));

        if (tab instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) tab;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView && child.getId() != R.id.tvUpcomingCount) {
                    TextView label = (TextView) child;
                    label.setTextColor(textColor);
                    label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                }
            }
        } else if (tab instanceof TextView) {
            TextView label = (TextView) tab;
            label.setTextColor(textColor);
            label.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
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
        bookingExecutor.execute(() -> {
          try {
            JSONArray remoteBookings = ApiClient.getArray(requireContext(), "/v1/bookings");
            List<Booking> all = new java.util.ArrayList<>();
            for (int i = 0; i < remoteBookings.length(); i++) {
                org.json.JSONObject row = remoteBookings.optJSONObject(i);
                if (row != null) all.add(BookingMapper.fromJson(row));
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> displayBookings(all));
          } catch (Exception exception) {
            if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Không tải được lịch đặt: " + exception.getMessage(), Toast.LENGTH_LONG).show());
          }
        });
    }

    private void displayBookings(List<Booking> all) {
        java.util.List<Booking> filtered = new java.util.ArrayList<>();
        int upcomingCount = 0;
        for (Booking booking : all) {
            if (booking.getStatus() == BookingStatus.CONFIRMED && !DateTimeUtils.hasBookingEnded(booking.getDate(), booking.getTimeRange())) upcomingCount++;
        }
        if (tvUpcomingCount != null) tvUpcomingCount.setText(String.valueOf(upcomingCount));
        for (Booking b : all) {
            boolean ended = DateTimeUtils.hasBookingEnded(b.getDate(), b.getTimeRange());
            if (selectedTab == 0 && b.getStatus() == BookingStatus.CONFIRMED && !ended) {
                filtered.add(b);
            } else if (selectedTab == 1 && b.getStatus() == BookingStatus.CONFIRMED && ended) {
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
        bookingExecutor.execute(() -> {
            try {
                ApiClient.post(requireContext(), "/v1/bookings/" + booking.getId() + "/cancel", new org.json.JSONObject());
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã hủy lịch đặt " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
                    loadBookings();
                });
            } catch (Exception exception) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroy() {
        bookingExecutor.shutdownNow();
        super.onDestroy();
    }
}
