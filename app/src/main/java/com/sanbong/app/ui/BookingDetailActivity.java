package com.sanbong.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;

public class BookingDetailActivity extends AppCompatActivity {

    private Booking booking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        loadBookingData();
        setupToolbar();
        bindDataToViews();
        setupActionButtons();
    }

    private void loadBookingData() {
        booking = (Booking) getIntent().getSerializableExtra("booking");
        if (booking == null) {
            String code = getIntent().getStringExtra("booking_code");
            if (code != null) booking = BookingRepository.getInstance().getBookingByCode(code);
        }
        if (booking == null) {
            java.util.List<Booking> list = BookingRepository.getInstance().getMyBookings();
            if (!list.isEmpty()) booking = list.get(0);
        }
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuMore).setOnClickListener(v ->
                Toast.makeText(this, "Chia sẻ hoặc lưu vé đặt sân", Toast.LENGTH_SHORT).show());
    }

    private void bindDataToViews() {
        if (booking == null) return;
        ((TextView) findViewById(R.id.tvDetailPitchName)).setText(booking.getPitchName());
        ((TextView) findViewById(R.id.tvDetailPitchAddress)).setText(booking.getPitchAddress());
        ((TextView) findViewById(R.id.tvDetailBookingCode)).setText(booking.getBookingCode());
        ((TextView) findViewById(R.id.tvDetailDate)).setText(DateTimeUtils.formatDisplayDate(booking.getDate()));
        ((TextView) findViewById(R.id.tvDetailTime)).setText(booking.getTimeRange());
        ((TextView) findViewById(R.id.tvDetailPitchPrice)).setText(CurrencyUtils.formatVnd(booking.getTotalPrice()));
        ((TextView) findViewById(R.id.tvDetailTotalPrice)).setText(CurrencyUtils.formatVnd(booking.getTotalPrice()));

        boolean isConfirmed = (booking.getStatus() == BookingStatus.CONFIRMED);
        String statusText = isConfirmed ? "✓ Đã xác nhận" : "Đã hủy";
        ((TextView) findViewById(R.id.tvDetailStatusPill)).setText(statusText);
        ((TextView) findViewById(R.id.tvBadgeStatus)).setText(statusText);
    }

    private void setupActionButtons() {
        findViewById(R.id.btnCopyBookingCode).setOnClickListener(v -> {
            if (booking == null) return;
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("BookingCode", booking.getBookingCode()));
            Toast.makeText(this, "Đã sao chép: " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnContactPitch).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0912345678"))));
        findViewById(R.id.btnCallCustomer).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0912345678"))));

        View btnCancel = findViewById(R.id.btnCancelDetail);
        View btnReschedule = findViewById(R.id.btnRescheduleSlot);
        if (booking != null && booking.getStatus() != BookingStatus.CONFIRMED) {
            if (btnReschedule != null) btnReschedule.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        } else {
            if (btnReschedule != null) btnReschedule.setOnClickListener(v -> showRescheduleDialog());
            btnCancel.setOnClickListener(v -> showCancelDialog());
        }
    }

    private void showRescheduleDialog() {
        if (booking == null) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reschedule_slot, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.RadioGroup rg = view.findViewById(R.id.rgRescheduleSlots);
        android.widget.RadioButton[] options = {
                view.findViewById(R.id.rbSlot1), view.findViewById(R.id.rbSlot2),
                view.findViewById(R.id.rbSlot3), view.findViewById(R.id.rbSlot4)
        };
        java.util.List<TimeSlot> available = new java.util.ArrayList<>();
        for (TimeSlot slot : BookingRepository.getInstance().getSlots(booking.getPitchId(), booking.getDate())) {
            if (slot.isAvailable() || slot.getId().equals(booking.getSlotId())) available.add(slot);
        }
        int visibleCount = Math.min(options.length, available.size());
        for (int i = 0; i < options.length; i++) {
            if (i >= visibleCount) {
                options[i].setVisibility(View.GONE);
                continue;
            }
            TimeSlot slot = available.get(i);
            options[i].setVisibility(View.VISIBLE);
            options[i].setText(slot.getTimeRange() + "   •   " +
                    com.sanbong.app.util.CurrencyUtils.formatVnd(slot.getPrice()) +
                    (slot.getId().equals(booking.getSlotId()) ? " (Giờ hiện tại)" : " (Còn trống)"));
            options[i].setTag(slot.getId());
            options[i].setChecked(slot.getId().equals(booking.getSlotId()));
        }
        view.findViewById(R.id.btnCancelReschedule).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmReschedule).setOnClickListener(v -> {
            int selectedId = rg.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn khung giờ", Toast.LENGTH_SHORT).show();
                return;
            }
            String newSlotId = String.valueOf(view.findViewById(selectedId).getTag());
            boolean updated = BookingRepository.getInstance().rescheduleBooking(booking.getBookingCode(), newSlotId);
            if (!updated) {
                Toast.makeText(this, "Khung giờ vừa chọn không còn trống", Toast.LENGTH_LONG).show();
                dialog.dismiss();
                return;
            }
            booking = BookingRepository.getInstance().getBookingByCode(booking.getBookingCode());
            bindDataToViews();
            Toast.makeText(this, "Đã cập nhật khung giờ", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showCancelDialog() {
        if (booking == null) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ((TextView) view.findViewById(R.id.tvDialogPitchName)).setText(booking.getPitchName());
        ((TextView) view.findViewById(R.id.tvDialogDateTime)).setText(DateTimeUtils.formatDisplayDate(booking.getDate()) + " | " + booking.getTimeRange());
        ((TextView) view.findViewById(R.id.tvDialogAddress)).setText(booking.getPitchAddress());

        view.findViewById(R.id.btnKeepBooking).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmCancel).setOnClickListener(v -> {
            dialog.dismiss();
            boolean cancelled = BookingRepository.getInstance().cancelBooking(booking.getBookingCode());
            if (cancelled) {
                Toast.makeText(this, "Đã hủy lịch đặt " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Lịch này đã được hủy hoặc không còn tồn tại", Toast.LENGTH_LONG).show();
            }
        });
        dialog.show();
    }
}
