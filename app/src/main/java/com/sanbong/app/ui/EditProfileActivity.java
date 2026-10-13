package com.sanbong.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sanbong.app.R;

import java.util.Calendar;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etEmail;
    private TextView tvDistrict, tvBirthdate, tvGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        setupToolbar();
        setupPickers();
        setupActions();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etEditFullName);
        etPhone = findViewById(R.id.etEditPhone);
        etEmail = findViewById(R.id.etEditEmail);
        tvDistrict = findViewById(R.id.tvEditDistrict);
        tvBirthdate = findViewById(R.id.tvEditBirthdate);
        tvGender = findViewById(R.id.tvEditGender);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarEditProfile);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPickers() {
        findViewById(R.id.btnSelectDistrict).setOnClickListener(v -> {
            String[] districts = {"Thanh Xuân, Hà Nội", "Cầu Giấy, Hà Nội", "Nam Từ Liêm, Hà Nội", "Đống Đa, Hà Nội", "Hà Đông, Hà Nội"};
            com.sanbong.app.util.ThemePickerUtils.showOptionPicker(this, "Chọn khu vực", "Lựa chọn khu vực bạn thường xuyên đặt sân",
                    R.drawable.ic_ri_map_pin, districts, tvDistrict.getText().toString(), tvDistrict::setText);
        });

        findViewById(R.id.btnSelectBirthdate).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.set(1998, 4, 15);
            com.sanbong.app.util.ThemePickerUtils.showDatePicker(this, c, (view, year, month, dayOfMonth) ->
                    tvBirthdate.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)));
        });

        findViewById(R.id.btnSelectGender).setOnClickListener(v -> {
            String[] genders = {"Nam", "Nữ", "Khác"};
            com.sanbong.app.util.ThemePickerUtils.showOptionPicker(this, "Chọn giới tính", "Giúp tối ưu hồ sơ cầu thủ của bạn",
                    R.drawable.ic_ri_user_fill, genders, tvGender.getText().toString(), tvGender::setText);
        });

        findViewById(R.id.btnChangeAvatarText).setOnClickListener(v ->
                Toast.makeText(this, "Chọn ảnh đại diện mới từ thư viện", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnEditAvatarPicker).setOnClickListener(v ->
                Toast.makeText(this, "Chụp ảnh đại diện mới", Toast.LENGTH_SHORT).show());
    }

    private void setupActions() {
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ tên và số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đã lưu cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnCancelEditProfile).setOnClickListener(v -> finish());
    }
}
