package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.sanbong.app.R;

public class AuthActivity extends AppCompatActivity {

    private View layoutLogin;
    private View layoutRegister;
    private EditText etLoginPhone, etLoginPassword;
    private EditText etRegName, etRegPhone, etRegPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        initViews();
        setupSwitchListeners();
        setupAuthActions();
    }

    private void initViews() {
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);
        etLoginPhone = findViewById(R.id.etLoginPhone);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        etRegName = findViewById(R.id.etRegName);
        etRegPhone = findViewById(R.id.etRegPhone);
        etRegPassword = findViewById(R.id.etRegPassword);
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
            String phone = etLoginPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đăng nhập thành công! Chào mừng bạn", Toast.LENGTH_SHORT).show();
            goToMain();
        });

        findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> {
            String name = etRegName.getText().toString().trim();
            String phone = etRegPhone.getText().toString().trim();
            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Đăng ký thành công! Chào mừng " + name, Toast.LENGTH_SHORT).show();
            goToMain();
        });

        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
            goToMain();
        });

        findViewById(R.id.btnForgotPassword).setOnClickListener(v -> {
            Toast.makeText(this, "Vui lòng liên hệ hỗ trợ hoặc nhập SĐT để nhận mã OTP", Toast.LENGTH_SHORT).show();
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
