package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;
import com.sanbong.app.util.IdGenerator;
import com.sanbong.app.util.ValidationUtils;

/**
 * Màn hình điền thông tin người đặt và xác nhận đặt sân
 */
public class BookingConfirmActivity extends AppCompatActivity {

    private Pitch pitch;
    private TimeSlot slot;

    private TextView tvSummaryPitchName, tvSummaryAddress, tvSummaryTime, tvSummaryTotal;
    private TextInputLayout tilCustomerName, tilCustomerPhone;
    private EditText etCustomerName, etCustomerPhone;
    private MaterialButton btnSubmitBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        pitch = (Pitch) getIntent().getSerializableExtra("pitch");
        slot = (TimeSlot) getIntent().getSerializableExtra("slot");

        if (pitch == null || slot == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        bindSummaryData();
        setupSubmitButton();
    }

    private void initViews() {
        tvSummaryPitchName = findViewById(R.id.tvSummaryPitchName);
        tvSummaryAddress = findViewById(R.id.tvSummaryAddress);
        tvSummaryTime = findViewById(R.id.tvSummaryTime);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        tilCustomerName = findViewById(R.id.tilCustomerName);
        tilCustomerPhone = findViewById(R.id.tilCustomerPhone);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        btnSubmitBooking = findViewById(R.id.btnSubmitBooking);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindSummaryData() {
        android.widget.ImageView ivBanner = findViewById(R.id.ivConfirmBanner);
        if (ivBanner != null && pitch.getImageResId() != 0) {
            ivBanner.setImageResource(pitch.getImageResId());
        }
        tvSummaryPitchName.setText(pitch.getName());
        TextView tvType = findViewById(R.id.tvSummaryPitchType);
        if (tvType != null) tvType.setText(pitch.getType().getDisplayName());
        tvSummaryAddress.setText(pitch.getAddress());
        TextView tvDate = findViewById(R.id.tvSummaryDate);
        if (tvDate != null) tvDate.setText(DateTimeUtils.formatDisplayDate(slot.getDate()));
        tvSummaryTime.setText(slot.getTimeRange() + " · 1,5 giờ");
        String formattedPrice = CurrencyUtils.formatVnd(slot.getPrice());
        TextView tvPitchPrice = findViewById(R.id.tvPitchPrice);
        if (tvPitchPrice != null) tvPitchPrice.setText(formattedPrice);
        tvSummaryTotal.setText(formattedPrice);
        TextView tvBottomTotal = findViewById(R.id.tvBottomTotal);
        if (tvBottomTotal != null) tvBottomTotal.setText(formattedPrice);
    }

    private void setupSubmitButton() {
        btnSubmitBooking.setOnClickListener(v -> processBooking());
    }

    private void processBooking() {
        String name = etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
        String phone = etCustomerPhone.getText() != null ? etCustomerPhone.getText().toString().trim() : "";

        boolean isValid = true;
        if (!ValidationUtils.isValidName(name)) {
            tilCustomerName.setError("Vui lòng nhập họ và tên (tối thiểu 2 ký tự)");
            isValid = false;
        } else {
            tilCustomerName.setError(null);
        }

        if (!ValidationUtils.isValidPhoneNumber(phone)) {
            tilCustomerPhone.setError("Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)");
            isValid = false;
        } else {
            tilCustomerPhone.setError(null);
        }

        if (!isValid) return;

        String bookingCode = IdGenerator.generateBookingCode();
        Booking booking = new Booking(
                bookingCode,
                pitch.getId(),
                pitch.getName(),
                pitch.getAddress(),
                slot.getId(),
                slot.getDate(),
                slot.getTimeRange(),
                slot.getPrice(),
                name,
                phone
        );

        boolean confirmed = BookingRepository.getInstance().confirmBooking(booking);
        if (confirmed) {
            Intent intent = new Intent(this, BookingSuccessActivity.class);
            intent.putExtra("booking", booking);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Khung giờ này vừa có người đặt. Vui lòng chọn khung giờ khác!", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
