package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;
import com.sanbong.app.util.ValidationUtils;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService bookingExecutor = Executors.newSingleThreadExecutor();

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
        tvSummaryTime.setText(slot.getTimeRange());
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
        if (!((android.widget.CheckBox) findViewById(R.id.cbConfirmTerms)).isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý điều khoản trước khi đặt sân.", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioGroup paymentOptions = findViewById(R.id.rgPaymentMethod);
        String paymentMethod = paymentOptions.getCheckedRadioButtonId() == R.id.rbPaymentQr ? "qr" : "cash";
        JSONObject payload = new JSONObject();
        try {
            payload.put("pitch_id", pitch.getId());
            payload.put("slot_id", slot.getId());
            payload.put("booking_date", slot.getDate());
            payload.put("customer_name", name);
            payload.put("customer_phone", phone);
            payload.put("payment_method", paymentMethod);
        } catch (Exception exception) {
            Toast.makeText(this, "Không tạo được yêu cầu đặt sân.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitBooking.setEnabled(false);
        bookingExecutor.execute(() -> {
            try {
                JSONObject response = ApiClient.post(this, "/v1/bookings", payload);
                Booking booking = new Booking(
                        response.optString("id"), response.optString("booking_code"),
                        response.optString("pitch_id", pitch.getId()),
                        response.optString("pitch_name", pitch.getName()),
                        response.optString("pitch_address", pitch.getAddress()),
                        response.optString("slot_id", slot.getId()),
                        response.optString("booking_date", slot.getDate()),
                        response.optString("time_range", slot.getTimeRange()),
                        response.optLong("total_price", slot.getPrice()), name, phone,
                        response.optString("payment_method", paymentMethod),
                        response.optString("payment_status", paymentMethod.equals("qr") ? "qr_due" : "cash_due"),
                        com.sanbong.app.model.BookingStatus.CONFIRMED
                );
                booking.setPitch(pitch);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    Intent intent = new Intent(this, BookingSuccessActivity.class);
                    intent.putExtra("booking", booking);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    btnSubmitBooking.setEnabled(true);
                    Toast.makeText(this, exception.getMessage() == null
                            ? "Không đặt được sân. Hãy thử lại." : exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        bookingExecutor.shutdownNow();
        super.onDestroy();
    }
}
