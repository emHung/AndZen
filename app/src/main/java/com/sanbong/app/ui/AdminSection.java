package com.sanbong.app.ui;

import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.sanbong.app.data.ApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/** Discard stale read callbacks when a filter or the active section changes. */
abstract class AdminSection {
    final AppCompatActivity activity;
    final LinearLayout parent;
    final AdminUi ui;
    final ExecutorService executor;
    private int generation;
    boolean active = true;
    boolean saving;
    AdminSection(AppCompatActivity activity, LinearLayout parent, ExecutorService executor) {
        this.activity = activity; this.parent = parent; this.executor = executor; ui = new AdminUi(activity);
    }
    void close() { active = false; generation++; }
    boolean visible() { return active && !activity.isFinishing() && !activity.isDestroyed(); }
    void read(String path, Consumer<JSONArray> success, Runnable retry) {
        int request = ++generation;
        executor.execute(() -> {
            try {
                JSONArray rows = ApiClient.getArray(activity, path);
                activity.runOnUiThread(() -> { if (visible() && request == generation) success.accept(rows); });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    if (!visible() || request != generation) return;
                    parent.removeAllViews(); ui.note(parent, "Không tải được dữ liệu: " + e.getMessage());
                    parent.addView(ui.button("Thử lại", true, retry));
                });
            }
        });
    }
    void write(boolean put, String path, JSONObject body, Runnable refresh) {
        if (saving || !visible()) return;
        saving = true;
        executor.execute(() -> {
            try {
                if (put) ApiClient.put(activity, path, body); else ApiClient.patch(activity, path, body);
                activity.runOnUiThread(() -> {
                    saving = false;
                    if (visible()) { Toast.makeText(activity, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show(); refresh.run(); }
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> { saving = false; if (visible()) { Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show(); refresh.run(); } });
            }
        });
    }
    JSONObject body(String key, String value) {
        try { return new JSONObject().put(key, value); } catch (org.json.JSONException e) { throw new IllegalArgumentException("Dữ liệu không hợp lệ", e); }
    }
}
