package com.sanbong.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.BookingMapper;
import com.sanbong.app.data.TimeSlotMapper;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingDetailActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Booking booking;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);
        booking = (Booking) getIntent().getSerializableExtra("booking");
        if (booking == null) {
            Toast.makeText(this, "Không tìm thấy thông tin lịch đặt.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setupToolbar();
        bindDataToViews();
        setupActionButtons();
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuMore).setOnClickListener(v ->
                Toast.makeText(this, "Chia sẻ hoặc lưu vé đặt sân", Toast.LENGTH_SHORT).show());
    }

    private void bindDataToViews() {
        if (booking == null) return;
        com.sanbong.app.model.Pitch pitch = booking.getPitch();
        ((android.widget.ImageView) findViewById(R.id.ivDetailPitchImage))
                .setImageResource(pitch == null ? 0 : pitch.getImageResId());
        ((TextView) findViewById(R.id.tvDetailPitchType)).setText(
                pitch == null ? "Sân bóng" : pitch.getType().getDisplayName());
        ((View) findViewById(R.id.tvDetailPhotoCount).getParent()).setVisibility(View.GONE);
        List<String> amenities = pitch == null ? java.util.Collections.emptyList() : pitch.getAmenities();
        int[] amenityIds = {R.id.tvDetailAmenityOne, R.id.tvDetailAmenityTwo};
        for (int i = 0; i < amenityIds.length; i++) {
            TextView label = findViewById(amenityIds[i]);
            boolean present = amenities != null && i < amenities.size();
            ((View) label.getParent()).setVisibility(present ? View.VISIBLE : View.GONE);
            label.setText(present ? amenities.get(i) : "");
        }
        ((TextView) findViewById(R.id.tvDetailPitchName)).setText(booking.getPitchName());
        ((TextView) findViewById(R.id.tvDetailPitchAddress)).setText(booking.getPitchAddress());
        ((TextView) findViewById(R.id.tvDetailBookingCode)).setText(booking.getBookingCode());
        ((TextView) findViewById(R.id.tvDetailDate)).setText(DateTimeUtils.formatDisplayDate(booking.getDate()));
        ((TextView) findViewById(R.id.tvDetailTime)).setText(booking.getTimeRange());
        ((TextView) findViewById(R.id.tvDetailDuration)).setText(DateTimeUtils.durationLabel(booking.getTimeRange()));
        ((TextView) findViewById(R.id.tvDetailPitchPrice)).setText(CurrencyUtils.formatVnd(booking.getTotalPrice()));
        ((TextView) findViewById(R.id.tvDetailTotalPrice)).setText(CurrencyUtils.formatVnd(booking.getTotalPrice()));
        ((TextView) findViewById(R.id.tvDetailCustomerName)).setText(booking.getCustomerName());
        ((TextView) findViewById(R.id.tvDetailCustomerPhone)).setText(booking.getCustomerPhone());
        String status = booking.getStatus() == BookingStatus.CANCELLED ? "Đã hủy"
                : DateTimeUtils.hasBookingEnded(booking.getDate(), booking.getTimeRange()) ? "Đã hoàn thành" : "✓ Đã xác nhận";
        ((TextView) findViewById(R.id.tvDetailStatusPill)).setText(status);
        ((TextView) findViewById(R.id.tvBadgeStatus)).setText(status);
    }

    private void setupActionButtons() {
        findViewById(R.id.btnCopyBookingCode).setOnClickListener(v -> {
            if (booking == null) return;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("BookingCode", booking.getBookingCode()));
            Toast.makeText(this, "Đã sao chép: " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
        });
        View pitchContact = findViewById(R.id.btnContactPitch);
        if (pitchContact != null) pitchContact.setVisibility(View.GONE);
        View customerCall = findViewById(R.id.btnCallCustomer);
        String customerPhone = booking.getCustomerPhone();
        if (customerCall != null) {
            boolean hasPhone = customerPhone != null && !customerPhone.trim().isEmpty();
            customerCall.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
            if (hasPhone) customerCall.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(customerPhone)))));
        }
        View cancel = findViewById(R.id.btnCancelDetail);
        View reschedule = findViewById(R.id.btnRescheduleSlot);
        if (booking.getStatus() != BookingStatus.CONFIRMED || DateTimeUtils.hasBookingStarted(booking.getDate(), booking.getTimeRange())) {
            if (reschedule != null) reschedule.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
            return;
        }
        if (reschedule != null) reschedule.setOnClickListener(v -> chooseRescheduleDate());
        cancel.setOnClickListener(v -> showCancelDialog());
    }

    private void chooseRescheduleDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        try {
            String[] parts = booking.getDate().split("-");
            calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        } catch (Exception ignored) { }
        DatePickerDialog picker = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            String selectedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            loadSlotsForReschedule(selectedDate);
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        picker.getDatePicker().setMaxDate(DateTimeUtils.getLatestBookableDay().getTimeInMillis());
        picker.show();
    }

    private void loadSlotsForReschedule(String selectedDate) {
        Toast.makeText(this, "Đang tải khung giờ...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                JSONArray rows = ApiClient.getArray(this, "/v1/pitches/" + booking.getPitchId()
                        + "/slots?date=" + selectedDate);
                List<TimeSlot> slots = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    if (row != null) slots.add(TimeSlotMapper.fromJson(row));
                }
                runOnUiThread(() -> showRescheduleDialog(slots, selectedDate));
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this, "Không tải được khung giờ: "
                        + exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showRescheduleDialog(List<TimeSlot> slots, String selectedDate) {
        if (isFinishing()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_reschedule_slot, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        RadioGroup group = view.findViewById(R.id.rgRescheduleSlots);
        RadioButton[] options = {view.findViewById(R.id.rbSlot1), view.findViewById(R.id.rbSlot2),
                view.findViewById(R.id.rbSlot3), view.findViewById(R.id.rbSlot4)};
        List<TimeSlot> available = new ArrayList<>();
        for (TimeSlot slot : slots) {
            if (slot.isAvailable() || slot.getId().equals(booking.getSlotId())) available.add(slot);
        }
        group.removeAllViews();
        int currentSelection = -1;
        for (int i = 0; i < available.size(); i++) {
            TimeSlot slot = available.get(i);
            RadioButton option = i < options.length ? options[i] : new RadioButton(this);
            option.setId(View.generateViewId());
            option.setText(slot.getTimeRange() + "   •   " + CurrencyUtils.formatVnd(slot.getPrice())
                    + (slot.getId().equals(booking.getSlotId()) && selectedDate.equals(booking.getDate()) ? " (Giờ hiện tại)" : " (Còn trống)"));
            option.setTextColor(getColor(R.color.text_primary));
            option.setTextSize(13);
            option.setTypeface(null, android.graphics.Typeface.BOLD);
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.primary_green)));
            option.setPadding(12, 12, 12, 12);
            option.setTag(slot.getId());
            group.addView(option);
            if (slot.getId().equals(booking.getSlotId()) && selectedDate.equals(booking.getDate())) currentSelection = option.getId();
        }
        if (currentSelection != -1) group.check(currentSelection);
        view.findViewById(R.id.btnCancelReschedule).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmReschedule).setOnClickListener(v -> {
            int selectedId = group.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Vui lòng chọn khung giờ", Toast.LENGTH_SHORT).show();
                return;
            }
            String newSlotId = String.valueOf(group.findViewById(selectedId).getTag());
            dialog.dismiss();
            executor.execute(() -> {
                try {
                    JSONObject response = ApiClient.post(this, "/v1/bookings/" + booking.getId()
                            + "/reschedule?slot_id=" + Uri.encode(newSlotId) + "&booking_date=" + Uri.encode(selectedDate), null);
                    Booking updated = BookingMapper.fromJson(response);
                    updated.setPitch(booking.getPitch());
                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        booking = updated;
                        bindDataToViews();
                        Toast.makeText(this, "Đã cập nhật ngày và khung giờ đặt sân", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception exception) {
                    runOnUiThread(() -> Toast.makeText(this, "Không đổi được khung giờ: "
                            + exception.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });
        dialog.show();
    }

    private void showCancelDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ((TextView) view.findViewById(R.id.tvDialogPitchName)).setText(booking.getPitchName());
        ((TextView) view.findViewById(R.id.tvDialogDateTime)).setText(
                DateTimeUtils.formatDisplayDate(booking.getDate()) + " | " + booking.getTimeRange());
        ((TextView) view.findViewById(R.id.tvDialogAddress)).setText(booking.getPitchAddress());
        view.findViewById(R.id.btnKeepBooking).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirmCancel).setOnClickListener(v -> {
            dialog.dismiss();
            executor.execute(() -> {
                try {
                    JSONObject response = ApiClient.post(this, "/v1/bookings/" + booking.getId() + "/cancel", null);
                    Booking updated = BookingMapper.fromJson(response);
                    updated.setPitch(booking.getPitch());
                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        booking = updated;
                        bindDataToViews();
                        setupActionButtons();
                        Toast.makeText(this, "Đã hủy lịch đặt " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception exception) {
                    runOnUiThread(() -> Toast.makeText(this, "Không hủy được lịch: "
                            + exception.getMessage(), Toast.LENGTH_LONG).show());
                }
            });
        });
        dialog.show();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
