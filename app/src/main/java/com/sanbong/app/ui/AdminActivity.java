package com.sanbong.app.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android management screens. All reads and writes go through FastAPI. */
public class AdminActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<JSONObject> pitches = new ArrayList<>();
    private LinearLayout content;
    private LinearLayout tabs;
    private TextView message;
    private String role = "customer";
    private String currentTab = "pitches";
    private String pitchQuery = "";
    private String selectedPitchId;
    private AdminSection section;
    private String focusedSlotId, focusedSlotDay;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin);
        content = findViewById(R.id.adminContent);
        tabs = findViewById(R.id.adminTabs);
        message = findViewById(R.id.adminRole);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.adminToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        android.view.MenuItem notifications = toolbar.getMenu().add("Thông báo");
        notifications.setIcon(R.drawable.ic_ri_notification);
        androidx.core.graphics.drawable.DrawableCompat.setTint(notifications.getIcon(), getColor(R.color.white));
        notifications.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        notifications.setOnMenuItemClickListener(item -> {
            startActivity(new android.content.Intent(this, NotificationActivity.class));
            return true;
        });
        message.setText("Đang kiểm tra quyền truy cập...");
        verifyRole();
    }

    private void verifyRole() {
        executor.execute(() -> {
            try {
                JSONObject profile = ApiClient.get(this, "/v1/me");
                role = profile.optString("role", "customer");
                runOnUiThread(() -> {
                    if (!"admin".equals(role) && !"staff".equals(role)) {
                        message.setText("Tài khoản này chưa được cấp quyền nhân viên hoặc quản trị viên.");
                        tabs.setVisibility(View.GONE); content.removeAllViews(); return;
                    }
                    message.setText("admin".equals(role) ? "Quản trị viên" : "Nhân viên");
                    buildTabs(); loadData();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> message.setText("Không xác minh được quyền: " + exception.getMessage()));
            }
        });
    }

    private void buildTabs() {
        tabs.removeAllViews();
        addTab("Lịch đặt", "bookings"); addTab("Sân bóng", "pitches"); addTab("Khung giờ", "slots");
        if ("admin".equals(role)) addTab("Nhân viên", "staff");
    }

    private void addTab(String title, String name) {
        boolean selected = name.equals(currentTab);
        TextView tab = new TextView(this);
        tab.setText(title); tab.setTextSize(12); tab.setGravity(Gravity.CENTER);
        tab.setTypeface(null, selected ? 1 : 0);
        tab.setPadding(dp(2), dp(10), dp(2), dp(10));
        int icon = "bookings".equals(name) ? R.drawable.ic_ri_calendar : "pitches".equals(name)
                ? R.drawable.ic_ri_football : "staff".equals(name) ? R.drawable.ic_ri_group : R.drawable.ic_ri_time;
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(this, icon).mutate();
        drawable.setBounds(0, 0, dp(22), dp(22));
        drawable.setTint(getColor(selected ? R.color.white : R.color.text_primary));
        tab.setCompoundDrawables(null, drawable, null, null); tab.setCompoundDrawablePadding(dp(5));
        tab.setTextColor(getColor(selected ? R.color.white : R.color.text_primary));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setCornerRadius(dp(12));
        background.setColor(getColor(selected ? R.color.primary_green : R.color.brand_100));
        tab.setBackground(background);
        tab.setContentDescription(title);
        tab.setSelected(selected);
        tab.setId(View.generateViewId());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(68), 1);
        params.setMargins(0, 0, tabs.getChildCount() == ("admin".equals(role) ? 3 : 2) ? 0 : dp(6), 0);
        tabs.addView(tab, params);
        tab.setOnClickListener(v -> { currentTab = name; buildTabs(); render(); });
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                JSONArray pitchRows = ApiClient.getArray(this, "/v1/admin/pitches");
                List<JSONObject> loaded = new ArrayList<>();
                for (int i = 0; i < pitchRows.length(); i++) if (pitchRows.optJSONObject(i) != null) loaded.add(pitchRows.getJSONObject(i));
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pitches.clear(); pitches.addAll(loaded); render();
                });
            } catch (Exception exception) { runOnUiThread(() -> message.setText("Không tải được danh sách sân: " + exception.getMessage())); }
        });
    }

    private void render() {
        if (section != null) { section.close(); section = null; }
        content.removeAllViews();
        if ("bookings".equals(currentTab)) {
            AdminBookingSection bookings = new AdminBookingSection(this, content, executor, pitches);
            if (focusedSlotId != null) bookings.focusSlot(focusedSlotId, selectedPitchId, focusedSlotDay);
            focusedSlotId = focusedSlotDay = null;
            section = bookings; bookings.show();
        } else if ("pitches".equals(currentTab)) renderPitches();
        else if ("staff".equals(currentTab) && "admin".equals(role)) {
            AdminStaffSection staff = new AdminStaffSection(this, content, executor, pitches);
            section = staff; staff.show();
        } else if ("slots".equals(currentTab)) {
            AdminSlotSection slots = new AdminSlotSection(this, content, executor, pitches, selectedPitchId,
                    (slotId, pitchId, day) -> {
                        focusedSlotId = slotId; focusedSlotDay = day; selectedPitchId = pitchId;
                        currentTab = "bookings"; buildTabs(); render();
                        ((ScrollView) findViewById(R.id.adminScroll)).smoothScrollTo(0, 0);
                    });
            section = slots; slots.show();
        }
    }

    private void renderPitches() {
        AdminPitchSection.show(content, pitches, "admin".equals(role), pitchQuery,
                value -> pitchQuery = value, () -> showPitchForm(null), this::showPitchForm, id -> {
                    selectedPitchId = id; currentTab = "slots"; buildTabs(); render();
                    ((ScrollView) findViewById(R.id.adminScroll)).smoothScrollTo(0, 0);
                });
    }

    private void showPitchForm(JSONObject current) {
        LinearLayout fields = new LinearLayout(this); fields.setPadding(dp(18), dp(4), dp(18), dp(4)); fields.setOrientation(LinearLayout.VERTICAL);
        EditText id = field(fields, "Mã sân", current == null ? "" : current.optString("id"), false);
        EditText name = field(fields, "Tên sân", current == null ? "" : current.optString("name"), false);
        EditText district = field(fields, "Khu vực", current == null ? "" : current.optString("district"), false);
        EditText address = field(fields, "Địa chỉ", current == null ? "" : current.optString("address"), false);
        Spinner type = new Spinner(this); type.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Sân 5 người", "Sân 7 người", "Sân 11 người"}));
        if (current != null) type.setSelection(java.util.Arrays.asList("FIVE", "SEVEN", "ELEVEN").indexOf(current.optString("pitch_type")));
        fields.addView(type);
        EditText price = field(fields, "Giá cơ bản", current == null ? "" : String.valueOf(current.optLong("base_price")), true);
        EditText description = field(fields, "Mô tả", current == null ? "" : current.optString("description"), false);
        EditText amenities = field(fields, "Tiện ích (phân tách bằng dấu phẩy)", current == null ? "" : join(current.optJSONArray("amenities")), false);
        CheckBox active = new CheckBox(this); active.setText("Sân đang hoạt động"); active.setChecked(current == null || current.optBoolean("is_active", true)); if (current != null) fields.addView(active);
        ScrollView scroll = new ScrollView(this); scroll.addView(fields);
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(current == null ? "Thêm sân" : "Sửa sân")
                .setView(scroll).setNegativeButton("Hủy", null).setPositiveButton("Lưu", (d, w) -> {
                    try {
                        JSONObject body = new JSONObject(); if (current == null) body.put("id", id.getText().toString().trim());
                        body.put("name", name.getText().toString().trim()).put("district", district.getText().toString().trim())
                                .put("address", address.getText().toString().trim()).put("pitch_type", new String[]{"FIVE", "SEVEN", "ELEVEN"}[type.getSelectedItemPosition()])
                                .put("base_price", Long.parseLong(price.getText().toString().trim())).put("rating", current == null ? 0 : current.optDouble("rating"))
                                .put("description", description.getText().toString().trim()).put("image_key", current == null ? JSONObject.NULL : current.opt("image_key"))
                                .put("is_active", active.isChecked());
                        JSONArray amenityList = new JSONArray(); for (String part : amenities.getText().toString().split(",")) if (!part.trim().isEmpty()) amenityList.put(part.trim());
                        body.put("amenities", amenityList);
                        String path = current == null ? "/v1/admin/pitches" : "/v1/admin/pitches/" + current.optString("id");
                        executor.execute(() -> {
                            try { if (current == null) ApiClient.post(this, path, body); else ApiClient.patch(this, path, body); runOnUiThread(this::loadData); }
                            catch (Exception exception) { runOnUiThread(() -> message.setText("Không lưu được sân: " + exception.getMessage())); }
                        });
                    } catch (Exception exception) { message.setText("Thông tin sân chưa hợp lệ: " + exception.getMessage()); }
                }).show();
    }

    private EditText field(LinearLayout parent, String hint, String value, boolean numeric) {
        EditText input = new EditText(this); input.setSingleLine(false); input.setHint(hint); input.setText(value);
        if (numeric) input.setInputType(InputType.TYPE_CLASS_NUMBER);
        parent.addView(input, new LinearLayout.LayoutParams(-1, dp(52))); return input;
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private String join(JSONArray values) { if (values == null) return ""; List<String> out = new ArrayList<>(); for (int i=0;i<values.length();i++) out.add(values.optString(i)); return String.join(", ", out); }
    @Override protected void onDestroy() { if (section != null) section.close(); executor.shutdownNow(); super.onDestroy(); }
}
