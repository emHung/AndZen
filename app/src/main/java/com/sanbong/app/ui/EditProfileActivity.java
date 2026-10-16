package com.sanbong.app.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Calendar;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_AVATAR_REQUEST = 701;

    private EditText etFullName, etPhone, etEmail;
    private TextView tvDistrict, tvBirthdate, tvGender;
    private ImageView ivAvatar;
    private final ExecutorService profileExecutor = Executors.newSingleThreadExecutor();
    private File selectedAvatar;
    private String originalEmail = "";

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
        ivAvatar = findViewById(R.id.ivEditAvatar);
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
            String[] current = tvBirthdate.getText().toString().split("/");
            if (current.length == 3) {
                try { c.set(Integer.parseInt(current[2]), Integer.parseInt(current[1]) - 1, Integer.parseInt(current[0])); }
                catch (NumberFormatException ignored) { }
            }
            com.sanbong.app.util.ThemePickerUtils.showDatePicker(this, c, null, Calendar.getInstance(), (view, year, month, dayOfMonth) ->
                    tvBirthdate.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)));
        });

        findViewById(R.id.btnSelectGender).setOnClickListener(v -> {
            String[] genders = {"Nam", "Nữ", "Khác"};
            com.sanbong.app.util.ThemePickerUtils.showOptionPicker(this, "Chọn giới tính", "Giúp tối ưu hồ sơ cầu thủ của bạn",
                    R.drawable.ic_ri_user_fill, genders, tvGender.getText().toString(), tvGender::setText);
        });

        findViewById(R.id.btnChangeAvatarText).setOnClickListener(v -> openAvatarPicker());
        findViewById(R.id.btnEditAvatarPicker).setOnClickListener(v -> openAvatarPicker());
    }

    private void setupActions() {
        findViewById(R.id.btnSaveProfile).setEnabled(false);
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            if (name.length() < 2) {
                Toast.makeText(this, "Họ tên cần có ít nhất 2 ký tự.", Toast.LENGTH_SHORT).show();
                return;
            }
            String phone = etPhone.getText().toString().trim();
            if (!phone.isEmpty() && !phone.matches("0[0-9]{9}")) {
                Toast.makeText(this, "Số điện thoại cần đủ 10 chữ số và bắt đầu bằng 0.", Toast.LENGTH_SHORT).show();
                return;
            }
            saveProfile();
        });

        findViewById(R.id.btnCancelEditProfile).setOnClickListener(v -> finish());
        loadProfile();
    }

    private void loadProfile() {
        profileExecutor.execute(() -> {
            try {
                JSONObject profile = ApiClient.get(this, "/v1/me");
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    etFullName.setText(profile.optString("full_name", ""));
                    etPhone.setText(profile.isNull("phone") ? "" : profile.optString("phone", ""));
                    originalEmail = editableValue(profile, "email", "");
                    etEmail.setText(originalEmail);
                    tvDistrict.setText(editableValue(profile, "district", "Chọn khu vực"));
                    tvGender.setText(editableValue(profile, "gender", "Chọn giới tính"));
                    tvBirthdate.setText("Chọn ngày sinh");
                    String birthdate = profile.optString("birthdate", "");
                    if (!birthdate.isEmpty() && !"null".equals(birthdate)) {
                        String[] parts = birthdate.split("-");
                        if (parts.length == 3) tvBirthdate.setText(parts[2] + "/" + parts[1] + "/" + parts[0]);
                    }
                    String avatarUrl = profile.optString("avatar_url", "");
                    if (!avatarUrl.isEmpty() && !"null".equals(avatarUrl)) loadAvatar(avatarUrl);
                    findViewById(R.id.btnSaveProfile).setEnabled(true);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void saveProfile() {
        String requestedEmail = etEmail.getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(requestedEmail).matches()) {
            Toast.makeText(this, "Email chưa đúng định dạng.", Toast.LENGTH_SHORT).show();
            return;
        }
        findViewById(R.id.btnSaveProfile).setEnabled(false);
        profileExecutor.execute(() -> {
            try {
                if (selectedAvatar != null) {
                    ApiClient.uploadAvatar(this, selectedAvatar, "image/jpeg");
                }
                JSONObject payload = new JSONObject();
                payload.put("full_name", etFullName.getText().toString().trim());
                String phone = etPhone.getText().toString().trim();
                payload.put("phone", phone.isEmpty() ? JSONObject.NULL : phone);
                String district = tvDistrict.getText().toString();
                payload.put("district", district.startsWith("Chọn ") ? JSONObject.NULL : district);
                String gender = tvGender.getText().toString();
                payload.put("gender", gender.startsWith("Chọn ") ? JSONObject.NULL : gender);
                String displayedDate = tvBirthdate.getText().toString();
                if (displayedDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    String[] date = displayedDate.split("/");
                    payload.put("birthdate", date[2] + "-" + date[1] + "-" + date[0]);
                } else payload.put("birthdate", JSONObject.NULL);
                ApiClient.patch(this, "/v1/me", payload);
                boolean emailPending = false;
                if (!requestedEmail.equalsIgnoreCase(originalEmail)) {
                    try {
                        JSONObject response = ApiClient.post(this, "/v1/me/email", new JSONObject().put("email", requestedEmail));
                        emailPending = response.optBoolean("email_change_pending", true);
                    } catch (Exception emailError) {
                        runOnUiThread(() -> {
                            findViewById(R.id.btnSaveProfile).setEnabled(true);
                            Toast.makeText(this, "Hồ sơ đã lưu. Email chưa đổi: " + emailError.getMessage(), Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                }
                final boolean pending = emailPending;
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    Toast.makeText(this, pending ? "Hồ sơ đã lưu. Xác nhận email để hoàn tất đổi email đăng nhập." : "Đã lưu hồ sơ lên tài khoản.", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    findViewById(R.id.btnSaveProfile).setEnabled(true);
                    Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String editableValue(JSONObject profile, String key, String fallback) {
        String value = profile.isNull(key) ? "" : profile.optString(key, "").trim();
        return value.isEmpty() || "null".equals(value) ? fallback : value;
    }

    private void openAvatarPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_AVATAR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_AVATAR_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        profileExecutor.execute(() -> {
            try (InputStream boundsInput = getContentResolver().openInputStream(uri)) {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(boundsInput, null, bounds);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 1024);
                InputStream imageInput = getContentResolver().openInputStream(uri);
                if (imageInput == null) throw new IllegalStateException("Không mở được ảnh đã chọn.");
                Bitmap bitmap;
                try (InputStream stream = imageInput) {
                    bitmap = BitmapFactory.decodeStream(stream, null, options);
                }
                if (bitmap == null) throw new IllegalStateException("Không đọc được ảnh đã chọn.");
                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, compressed);
                bitmap.recycle();
                File file = new File(getCacheDir(), "profile-avatar.jpg");
                try (FileOutputStream output = new FileOutputStream(file)) { output.write(compressed.toByteArray()); }
                selectedAvatar = file;
                runOnUiThread(() -> ivAvatar.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath())));
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this, "Không đọc được ảnh: " + exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadAvatar(String avatarUrl) {
        profileExecutor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(avatarUrl).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                try (InputStream input = connection.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) runOnUiThread(() -> ivAvatar.setImageBitmap(bitmap));
                } finally { connection.disconnect(); }
            } catch (Exception ignored) { }
        });
    }

    @Override
    protected void onDestroy() {
        profileExecutor.shutdownNow();
        super.onDestroy();
    }
}
