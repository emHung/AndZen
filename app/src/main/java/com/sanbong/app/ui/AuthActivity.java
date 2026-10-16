package com.sanbong.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.sanbong.app.R;
import com.sanbong.app.BuildConfig;
import com.sanbong.app.data.AuthSession;
import com.sanbong.app.data.SecureSessionStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthActivity extends AppCompatActivity {

    private View layoutLogin;
    private View layoutRegister;
    private EditText etLoginPhone, etLoginPassword;
    private EditText etRegName, etRegPhone, etRegEmail, etRegPassword, etRegPasswordConfirm;
    private final ExecutorService authExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        initViews();
        setupSwitchListeners();
        setupAuthActions();
        handleAuthCallback(getIntent());
    }

    private void initViews() {
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);
        etLoginPhone = findViewById(R.id.etLoginPhone);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        etRegName = findViewById(R.id.etRegName);
        etRegPhone = findViewById(R.id.etRegPhone);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegPasswordConfirm = findViewById(R.id.etRegPasswordConfirm);
    }

    private void setupSwitchListeners() {
        findViewById(R.id.btnSwitchToRegister).setOnClickListener(v -> {
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btnSwitchToLogin).setOnClickListener(v -> {
            layoutRegister.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
        });
    }

    private void setupAuthActions() {
        findViewById(R.id.btnLoginSubmit).setOnClickListener(v -> {
            String email = etLoginPhone.getText().toString().trim();
            String password = etLoginPassword.getText().toString();
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.isEmpty()) {
                Toast.makeText(this, "Nhập email và mật khẩu để đăng nhập.", Toast.LENGTH_SHORT).show();
                return;
            }
            runAuth(() -> AuthSession.signInWithEmail(this, email, password), response -> goToMain());
        });

        findViewById(R.id.btnLoginOtp).setVisibility(BuildConfig.PHONE_OTP_ENABLED ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> {
            String name = etRegName.getText().toString().trim();
            String phone = etRegPhone.getText().toString().trim();
            String email = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString();
            String passwordConfirm = etRegPasswordConfirm.getText().toString();
            if (name.length() < 2 || (email.isEmpty() && (!BuildConfig.PHONE_OTP_ENABLED || phone.isEmpty()))) {
                Toast.makeText(this, "Nhập họ tên và email để tạo tài khoản.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!email.isEmpty()) {
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Email chưa đúng định dạng.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 8 || !password.equals(passwordConfirm)) {
                    Toast.makeText(this, "Mật khẩu tối thiểu 8 ký tự và phải trùng nhau.", Toast.LENGTH_SHORT).show();
                    return;
                }
                runAuth(() -> AuthSession.signUpWithEmail(this, email, password, name, phone), response -> {
                    if (response.has("access_token")) goToMain();
                    else Toast.makeText(this, "Đã tạo tài khoản. Kiểm tra email để xác minh rồi đăng nhập.", Toast.LENGTH_LONG).show();
                });
            } else {
                requestOtp(phone, true, name);
            }
        });

        findViewById(R.id.btnLoginOtp).setOnClickListener(v -> showPhoneLoginDialog());

        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            try {
                requireAuthConfig();
                startActivity(AuthSession.googleSignInIntent(this));
            } catch (Exception exception) { showError(exception); }
        });

        findViewById(R.id.btnForgotPassword).setOnClickListener(v -> {
            String email = etLoginPhone.getText().toString().trim();
            if (!email.contains("@")) {
                Toast.makeText(this, "Nhập email trước để nhận liên kết đặt lại mật khẩu.", Toast.LENGTH_SHORT).show();
                return;
            }
            runAuth(() -> AuthSession.requestPasswordReset(email), response ->
                    Toast.makeText(this, "Nếu email tồn tại, Supabase sẽ gửi hướng dẫn đặt lại mật khẩu.", Toast.LENGTH_LONG).show());
        });
    }

    private interface AuthOperation { JSONObject run() throws Exception; }
    private interface AuthSuccess { void accept(JSONObject result); }

    private void requestOtp(String phone, boolean createUser, String fullName) {
        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại.", Toast.LENGTH_SHORT).show();
            return;
        }
        runAuth(() -> AuthSession.sendPhoneOtp(phone, createUser, fullName), response -> showOtpDialog(phone));
    }

    private void showPhoneLoginDialog() {
        LinearLayout inputContainer = new LinearLayout(this);
        inputContainer.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        inputContainer.setPadding(horizontalPadding, 0, horizontalPadding, 0);

        com.google.android.material.textfield.TextInputLayout phoneLayout = new com.google.android.material.textfield.TextInputLayout(
                this, null, com.google.android.material.R.attr.textInputStyle);
        phoneLayout.setHint("Số điện thoại, ví dụ 09xxxxxxxx");
        phoneLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        phoneLayout.setBoxStrokeColor(getColor(R.color.primary_green));
        phoneLayout.setStartIconDrawable(R.drawable.ic_ri_phone);
        phoneLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.text_muted)));
        float cornerRadius = 12 * getResources().getDisplayMetrics().density;
        phoneLayout.setBoxCornerRadii(cornerRadius, cornerRadius, cornerRadius, cornerRadius);

        com.google.android.material.textfield.TextInputEditText phoneInput =
                new com.google.android.material.textfield.TextInputEditText(phoneLayout.getContext());
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setSingleLine(true);
        phoneInput.setTextColor(getColor(R.color.text_primary));
        phoneInput.setHintTextColor(getColor(R.color.text_muted));
        phoneLayout.addView(phoneInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        inputContainer.addView(phoneLayout);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng nhập bằng SMS OTP")
                .setMessage("Dùng số điện thoại đã đăng ký tài khoản trên ứng dụng.")
                .setView(inputContainer)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gửi mã OTP", (dialog, which) ->
                        requestOtp(phoneInput.getText().toString().trim(), false, null))
                .show();
    }

    private void showOtpDialog(String phone) {
        EditText codeInput = new EditText(this);
        codeInput.setHint("Mã OTP SMS");
        codeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác minh số điện thoại")
                .setMessage("Nhập mã vừa được gửi đến " + phone)
                .setView(codeInput)
                .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Xác minh", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim();
                    if (code.length() < 4) {
                        Toast.makeText(this, "Mã OTP chưa hợp lệ.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    runAuth(() -> AuthSession.verifyPhoneOtp(this, phone, code), response -> goToMain());
                }).show();
    }

    private void runAuth(AuthOperation operation, AuthSuccess success) {
        if (!AuthSession.isConfigured()) {
            showError(new IllegalStateException("Chưa cấu hình Supabase. Xem backend/README.md để kết nối project."));
            return;
        }
        findViewById(R.id.btnLoginSubmit).setEnabled(false);
        findViewById(R.id.btnLoginOtp).setEnabled(false);
        findViewById(R.id.btnRegisterSubmit).setEnabled(false);
        authExecutor.execute(() -> {
            try {
                JSONObject response = operation.run();
                runOnUiThread(() -> {
                    setAuthButtonsEnabled(true);
                    success.accept(response);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    setAuthButtonsEnabled(true);
                    showError(exception);
                });
            }
        });
    }

    private void setAuthButtonsEnabled(boolean enabled) {
        findViewById(R.id.btnLoginSubmit).setEnabled(enabled);
        findViewById(R.id.btnLoginOtp).setEnabled(enabled);
        findViewById(R.id.btnRegisterSubmit).setEnabled(enabled);
    }

    private void requireAuthConfig() {
        if (!AuthSession.isConfigured()) throw new IllegalStateException("Chưa cấu hình Supabase trong local.properties.");
    }

    private void handleAuthCallback(Intent intent) {
        if (intent == null || intent.getData() == null || !"auth-callback".equals(intent.getData().getHost())) return;
        Uri callback = intent.getData();
        authExecutor.execute(() -> {
            try {
                JSONObject result = AuthSession.completeGoogleCallback(this, callback);
                if (AuthSession.hasSession(this)) runOnUiThread(this::goToMain);
                else runOnUiThread(() -> Toast.makeText(this,
                        "Email đã được xác minh. Hãy đăng nhập để tiếp tục.", Toast.LENGTH_LONG).show());
            } catch (Exception exception) {
                runOnUiThread(() -> showError(exception));
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthCallback(intent);
    }

    private void showError(Exception exception) {
        String message = exception.getMessage();
        String title = "Không thể đăng nhập";
        if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("invalid login credentials")) {
            message = "Email hoặc mật khẩu chưa đúng. Nếu quên mật khẩu, hãy dùng mục Quên mật khẩu.";
        } else if (message != null && message.toLowerCase(java.util.Locale.ROOT).contains("signups not allowed for otp")) {
            title = "SMS OTP chưa được bật";
            message = "Supabase chưa cho phép đăng ký bằng SMS OTP. Cần bật Phone signups và cấu hình nhà cung cấp SMS trong Supabase. Tài khoản đăng ký bằng email không tự liên kết với số điện thoại.";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message == null ? "Có lỗi khi xác thực." : message)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        authExecutor.shutdownNow();
        super.onDestroy();
    }
}
