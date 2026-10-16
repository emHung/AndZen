package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.AuthSession;
import com.sanbong.app.data.SecureSessionStore;
import com.sanbong.app.model.Booking;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private TextView tvStatBookingsNum;
    private TextView tvStatFavoritesNum;
    private TextView tvFrequentPitchName;
    private TextView tvFrequentPitchAddress;
    private TextView tvFrequentPitchDate;
    private String frequentPitchId;
    private final ExecutorService accountExecutor = Executors.newFixedThreadPool(3);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupHeaderButtons(view);
        setupCardActions(view);
        setupSettingsAndLogout(view);
        return view;
    }

    private void initViews(View view) {
        tvStatBookingsNum = view.findViewById(R.id.tvStatBookingsNum);
        tvStatFavoritesNum = view.findViewById(R.id.tvStatFavoritesNum);
        tvFrequentPitchName = view.findViewById(R.id.tvFrequentPitchName);
        tvFrequentPitchAddress = view.findViewById(R.id.tvFrequentPitchAddress);
        tvFrequentPitchDate = view.findViewById(R.id.tvFrequentPitchDate);
        tvStatBookingsNum.setText("—");
        tvStatFavoritesNum.setText("—");
        tvFrequentPitchName.setText("Chưa có lịch đặt");
        tvFrequentPitchAddress.setText("Đặt sân để xem thông tin tại đây");
        tvFrequentPitchDate.setText("");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDynamicStats();
    }

    private void updateDynamicStats() {
        android.content.Context appContext = requireContext().getApplicationContext();
        CompletableFuture<JSONObject> profileFuture = CompletableFuture.supplyAsync(() -> {
            try { return ApiClient.get(appContext, "/v1/me"); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, accountExecutor);
        CompletableFuture<JSONArray> bookingsFuture = CompletableFuture.supplyAsync(() -> {
            try { return ApiClient.getArray(appContext, "/v1/bookings"); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, accountExecutor);
        CompletableFuture<JSONArray> favoritesFuture = CompletableFuture.supplyAsync(() -> {
            try { return ApiClient.getArray(appContext, "/v1/favorites"); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, accountExecutor);

        profileFuture.thenAccept(profile -> {
            android.app.Activity activity = getActivity();
            if (activity != null) activity.runOnUiThread(() -> {
                if (isAdded() && getView() != null) bindProfile(profile);
            });
        });
        CompletableFuture.allOf(profileFuture, bookingsFuture, favoritesFuture).whenComplete((ignored, failure) -> {
            android.app.Activity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                if (failure != null) {
                    Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                            ? failure.getCause() : failure;
                    Toast.makeText(getContext(), "Không tải được dữ liệu tài khoản: " + cause.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                JSONArray bookings = bookingsFuture.join();
                JSONArray favorites = favoritesFuture.join();
                tvStatBookingsNum.setText(String.valueOf(bookings.length()));
                tvStatFavoritesNum.setText(String.valueOf(favorites.length()));
                java.util.Map<String, Integer> counts = new java.util.HashMap<>();
                for (int i = 0; i < bookings.length(); i++) {
                    JSONObject row = bookings.optJSONObject(i);
                    if (row != null && "confirmed".equals(row.optString("status"))) {
                        String id = row.optString("pitch_id");
                        counts.put(id, counts.getOrDefault(id, 0) + 1);
                    }
                }
                JSONObject latest = null;
                int highestCount = 0;
                for (int i = 0; i < bookings.length(); i++) {
                    JSONObject row = bookings.optJSONObject(i);
                    if (row == null || !"confirmed".equals(row.optString("status"))) continue;
                    int count = counts.get(row.optString("pitch_id"));
                    if (count > highestCount || (count == highestCount && latest != null
                            && row.optString("booking_date").compareTo(latest.optString("booking_date")) > 0)) {
                        highestCount = count;
                        latest = row;
                    }
                }
                if (latest != null) {
                    frequentPitchId = latest.optString("pitch_id", "");
                    tvFrequentPitchName.setText(latest.optString("pitch_name", "Chưa có lịch đặt"));
                    tvFrequentPitchAddress.setText(latest.optString("pitch_address", ""));
                    tvFrequentPitchDate.setText("Lần gần nhất · " + latest.optString("booking_date", ""));
                } else {
                    frequentPitchId = null;
                    tvFrequentPitchName.setText("Chưa có lịch đặt");
                    tvFrequentPitchAddress.setText("Đặt sân để xem thông tin tại đây");
                    tvFrequentPitchDate.setText("");
                }
            });
        });
    }

    private void bindProfile(JSONObject profile) {
        View view = getView();
        if (!isAdded() || view == null) return;
        ((TextView) view.findViewById(R.id.tvProfileName)).setText(profileValue(profile, "full_name"));
        ((TextView) view.findViewById(R.id.tvProfilePhone)).setText(profileValue(profile, "phone"));
        ((TextView) view.findViewById(R.id.tvProfileEmail)).setText(profileValue(profile, "email"));
        ((TextView) view.findViewById(R.id.tvProfileArea)).setText(profileValue(profile, "district"));
        ((TextView) view.findViewById(R.id.tvProfileGender)).setText(profileValue(profile, "gender"));
        String birthdate = profileValue(profile, "birthdate");
        if (birthdate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = birthdate.split("-");
            birthdate = parts[2] + "/" + parts[1] + "/" + parts[0];
        }
        ((TextView) view.findViewById(R.id.tvProfileBirthdate)).setText(birthdate);
        String role = profile.optString("role", "customer");
        view.findViewById(R.id.btnAdminManagement).setVisibility("admin".equals(role) || "staff".equals(role) ? View.VISIBLE : View.GONE);
        String avatar = profile.optString("avatar_url", "");
        if (!avatar.isEmpty() && !"null".equals(avatar)) loadAvatar(avatar);
    }

    private String profileValue(JSONObject profile, String key) {
        String value = profile.isNull(key) ? "" : profile.optString(key, "").trim();
        return value.isEmpty() || "null".equalsIgnoreCase(value) ? "Chưa cập nhật" : value;
    }

    private void setupHeaderButtons(View view) {
        view.findViewById(R.id.btnProfileNotification).setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificationActivity.class)));
        view.findViewById(R.id.btnProfileSettings).setOnClickListener(v ->
                showAccountSettings());
    }

    private void setupCardActions(View view) {
        view.findViewById(R.id.btnAdminManagement).setOnClickListener(v ->
                startActivity(new Intent(getContext(), AdminActivity.class)));
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class)));
        view.findViewById(R.id.btnChangeAvatar).setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class)));

        view.findViewById(R.id.btnStatBookings).setOnClickListener(v -> navigateToTab(R.id.nav_my_bookings));
        view.findViewById(R.id.btnNavMyBookings).setOnClickListener(v -> navigateToTab(R.id.nav_my_bookings));
        view.findViewById(R.id.btnStatFavorites).setOnClickListener(v -> navigateToTab(R.id.nav_favorites));
        view.findViewById(R.id.btnNavFavorites).setOnClickListener(v -> navigateToTab(R.id.nav_favorites));
        view.findViewById(R.id.btnViewAllFavorite).setOnClickListener(v -> navigateToTab(R.id.nav_favorites));

        view.findViewById(R.id.cardFrequentPitch).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_pitches);
            }
        });
    }

    private void setupSettingsAndLogout(View view) {
        view.findViewById(R.id.btnPaymentMethods).setOnClickListener(v ->
                Toast.makeText(getContext(), "Khi đặt sân có thể chọn tiền mặt hoặc QR. QR hiện chưa quét/đối soát tự động.", Toast.LENGTH_LONG).show());
        view.findViewById(R.id.btnSettingNotif).setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificationActivity.class)));
        view.findViewById(R.id.btnSettingPrivacy).setOnClickListener(v ->
                Toast.makeText(getContext(), "Chính sách bảo mật thông tin người dùng", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnSettingHelp).setOnClickListener(v ->
                Toast.makeText(getContext(), "Trung tâm hỗ trợ hotline: 1900 6868", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext()).setTitle("Đăng xuất")
                    .setMessage("Bạn muốn đăng xuất tài khoản này?")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Đăng xuất", (dialog, which) -> logout())
                    .show();
        });
    }

    private void loadAvatar(String avatarUrl) {
        android.app.Activity activity = getActivity();
        if (activity == null) return;
        accountExecutor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(avatarUrl).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                try (InputStream input = connection.getInputStream()) {
                    android.graphics.Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null && !activity.isFinishing()) activity.runOnUiThread(() -> {
                        if (!isAdded() || getView() == null) return;
                        ((android.widget.ImageView) getView().findViewById(R.id.ivProfileAvatar)).setImageBitmap(bitmap);
                    });
                } finally { connection.disconnect(); }
            } catch (Exception ignored) { }
        });
    }

    private void showAccountSettings() {
        String[] options = {"Đổi mật khẩu", "Quyền riêng tư", "Xóa tài khoản"};
        new AlertDialog.Builder(requireContext()).setTitle("Cài đặt tài khoản")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) changePassword();
                    else if (which == 1) changePrivacy();
                    else confirmDeleteAccount();
                }).show();
    }

    private void changePassword() {
        android.widget.LinearLayout form = new android.widget.LinearLayout(requireContext());
        form.setOrientation(android.widget.LinearLayout.VERTICAL);
        form.setPadding(48, 8, 48, 0);
        android.widget.EditText password = new android.widget.EditText(requireContext());
        password.setHint("Mật khẩu mới (ít nhất 8 ký tự)");
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password);
        android.widget.EditText confirmation = new android.widget.EditText(requireContext());
        confirmation.setHint("Nhập lại mật khẩu mới");
        confirmation.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(confirmation);
        new AlertDialog.Builder(requireContext()).setTitle("Đổi mật khẩu").setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newPassword = password.getText().toString();
                    if (newPassword.length() < 8 || !newPassword.equals(confirmation.getText().toString())) {
                        Toast.makeText(getContext(), "Mật khẩu chưa hợp lệ hoặc không trùng nhau.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    accountExecutor.execute(() -> {
                        try {
                            android.content.Context appContext = requireContext().getApplicationContext();
                            AuthSession.updatePassword(newPassword, appContext);
                            android.app.Activity activity = getActivity();
                            if (activity != null) activity.runOnUiThread(() -> Toast.makeText(activity, "Đã đổi mật khẩu.", Toast.LENGTH_SHORT).show());
                        } catch (Exception exception) {
                            android.app.Activity activity = getActivity();
                            if (activity != null) activity.runOnUiThread(() -> Toast.makeText(activity, exception.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                }).show();
    }

    private void changePrivacy() {
        accountExecutor.execute(() -> {
            try {
                JSONObject settings = ApiClient.get(requireContext(), "/v1/me/settings");
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    android.widget.CheckBox checkBox = new android.widget.CheckBox(requireContext());
                    checkBox.setText("Cho phép hiển thị hồ sơ công khai");
                    checkBox.setChecked(settings.optBoolean("profile_public", false));
                    new AlertDialog.Builder(requireContext()).setTitle("Quyền riêng tư").setView(checkBox)
                            .setNegativeButton("Hủy", null)
                            .setPositiveButton("Lưu", (dialog, which) -> accountExecutor.execute(() -> {
                                try {
                                    JSONObject update = new JSONObject()
                                            .put("notifications_enabled", settings.optBoolean("notifications_enabled", true))
                                            .put("profile_public", checkBox.isChecked());
                                    ApiClient.put(requireContext(), "/v1/me/settings", update);
                                    if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Đã lưu cài đặt riêng tư.", Toast.LENGTH_SHORT).show());
                                } catch (Exception exception) {
                                    if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            })).show();
                });
            } catch (Exception exception) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(requireContext()).setTitle("Xóa tài khoản")
                .setMessage("Hành động này xóa hồ sơ và các dữ liệu gắn với tài khoản, không thể hoàn tác.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa tài khoản", (dialog, which) -> accountExecutor.execute(() -> {
                    try {
                        ApiClient.delete(requireContext(), "/v1/me");
                        SecureSessionStore.clear(requireContext());
                        if (isAdded()) requireActivity().runOnUiThread(this::returnToLogin);
                    } catch (Exception exception) {
                        if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })).show();
    }

    private void logout() {
        accountExecutor.execute(() -> {
            try { AuthSession.signOut(requireContext()); }
            catch (Exception ignored) { SecureSessionStore.clear(requireContext()); }
            if (isAdded()) requireActivity().runOnUiThread(this::returnToLogin);
        });
    }

    private void returnToLogin() {
        Intent intent = new Intent(getContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void navigateToTab(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(tabId);
        }
    }

    @Override
    public void onDestroy() {
        accountExecutor.shutdownNow();
        super.onDestroy();
    }
}
