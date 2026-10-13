package com.sanbong.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.sanbong.app.R;
import com.sanbong.app.model.Booking;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;

/**
 * Màn hình chúc mừng đặt sân thành công và hiển thị mã đặt
 */
public class BookingSuccessActivity extends AppCompatActivity {

    private Booking booking;
    private TextView tvSuccessBookingCode, tvSuccessPitchName, tvSuccessAddress;
    private TextView tvSuccessDate, tvSuccessTime, tvSuccessPrice;
    private MaterialButton btnGoToMyBookings, btnBackToHome;
    private ImageView btnCopyCode, btnToolbarHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_success);

        booking = (Booking) getIntent().getSerializableExtra("booking");
        if (booking == null) {
            finish();
            return;
        }

        initViews();
        bindData();
        setupActions();
    }

    private void initViews() {
        tvSuccessBookingCode = findViewById(R.id.tvSuccessBookingCode);
        tvSuccessPitchName = findViewById(R.id.tvSuccessPitchName);
        tvSuccessAddress = findViewById(R.id.tvSuccessAddress);
        tvSuccessDate = findViewById(R.id.tvSuccessDate);
        tvSuccessTime = findViewById(R.id.tvSuccessTime);
        tvSuccessPrice = findViewById(R.id.tvSuccessPrice);
        btnGoToMyBookings = findViewById(R.id.btnGoToMyBookings);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        btnToolbarHome = findViewById(R.id.btnToolbarHome);
    }

    private void bindData() {
        tvSuccessBookingCode.setText(booking.getBookingCode());
        if (tvSuccessPitchName != null) tvSuccessPitchName.setText(booking.getPitchName());
        if (tvSuccessAddress != null) tvSuccessAddress.setText(booking.getPitchAddress());
        if (tvSuccessDate != null) tvSuccessDate.setText(DateTimeUtils.formatDisplayDate(booking.getDate()));
        if (tvSuccessTime != null) tvSuccessTime.setText(booking.getTimeRange());
        if (tvSuccessPrice != null) tvSuccessPrice.setText(CurrencyUtils.formatVnd(booking.getTotalPrice()));
    }

    private void setupActions() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        if (btnCopyCode != null) {
            btnCopyCode.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("BookingCode", booking.getBookingCode());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Đã sao chép mã: " + booking.getBookingCode(), Toast.LENGTH_SHORT).show();
            });
        }

        if (btnToolbarHome != null) btnToolbarHome.setOnClickListener(v -> navigateToHome());
        btnBackToHome.setOnClickListener(v -> navigateToHome());

        btnGoToMyBookings.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("initial_tab", R.id.nav_my_bookings);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
