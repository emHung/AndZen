package com.sanbong.app.ui;

import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.sanbong.app.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.ExecutorService;

final class AdminStaffSection extends AdminSection {
    private final List<JSONObject> pitches;
    private JSONArray people = new JSONArray();
    private String query = "";
    private int filter;
    private LinearLayout cards;
    AdminStaffSection(AppCompatActivity activity, LinearLayout parent, ExecutorService executor, List<JSONObject> pitches) {
        super(activity, parent, executor); this.pitches = pitches;
    }
    void show() { parent.removeAllViews(); ui.note(parent, "Đang tải nhân viên…"); refresh(); }
    private void refresh() { read("/v1/admin/staff", rows -> { people = rows; render(); }, this::show); }
    private boolean assigned(JSONObject person) { JSONArray ids = person.optJSONArray("pitch_ids"); return ids != null && ids.length() > 0; }
    private void render() {
        parent.removeAllViews(); int assigned = 0;
        for (int i = 0; i < people.length(); i++) if (people.optJSONObject(i) != null && assigned(people.optJSONObject(i))) assigned++;
        ui.stats(parent, new String[]{"Tổng nhân viên", "Đã phân công"}, new int[]{people.length(), assigned});
        ui.title(parent, "Quản lý nhân viên", this::refresh);
        ui.search(parent, "Tìm tên, email nhân viên…", query, text -> { query = text; filter(); });
        ui.chips(parent, new String[]{"Tất cả", "Đã phân công", "Chưa phân công"}, filter, value -> { filter = value; render(); });
        cards = ui.column(); parent.addView(cards); filter();
        ui.note(parent, "Nhân viên chỉ quản lý sân được phân công.");
    }
    private void filter() {
        if (cards == null) return; cards.removeAllViews();
        for (int i = 0; i < people.length(); i++) {
            JSONObject person = people.optJSONObject(i); if (person == null) continue;
            if (filter == 1 && !assigned(person) || filter == 2 && assigned(person)) continue;
            if (!AdminUi.key(person.optString("full_name") + " " + person.optString("email")).contains(AdminUi.key(query))) continue;
            LinearLayout card = ui.card(cards); String name = person.optString("full_name", "").trim();
            if (name.isEmpty()) name = "Nhân viên";
            LinearLayout header = ui.row();
            android.widget.TextView avatar = new android.widget.TextView(activity);
            String[] words = name.replace("[TEST]", "").trim().split("\\s+");
            String initials = words.length == 0 || words[0].isEmpty() ? "NV" : words[0].substring(0, 1)
                    + (words.length > 1 ? words[words.length - 1].substring(0, 1) : "");
            avatar.setText(initials.toUpperCase(java.util.Locale.ROOT)); avatar.setTextSize(18); avatar.setTypeface(null, 1);
            avatar.setTextColor(ui.color(R.color.primary_green)); avatar.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable avatarBg = new android.graphics.drawable.GradientDrawable();
            avatarBg.setShape(android.graphics.drawable.GradientDrawable.OVAL); avatarBg.setColor(ui.color(R.color.brand_100));
            avatar.setBackground(avatarBg); header.addView(avatar, new LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)));
            LinearLayout identity = ui.column(); identity.setPadding(ui.dp(12),0,0,0);
            ui.text(identity, name, 18, true); ui.badge(identity, "Nhân viên", false); ui.note(identity, person.optString("email"));
            header.addView(identity, new LinearLayout.LayoutParams(0,-2,1)); card.addView(header);
            ui.badge(card, assigned(person) ? "● Đã phân công" : "● Chưa phân công", !assigned(person)); ui.divider(card);
            ui.note(card, "Sân phụ trách"); JSONArray ids = person.optJSONArray("pitch_ids");
            if (ids == null || ids.length() == 0) ui.note(card, "Chưa có sân phụ trách");
            else for (int j = 0; j < ids.length(); j++) {
                String id = ids.optString(j), label = id;
                for (JSONObject p : pitches) if (id.equals(p.optString("id"))) label = p.optString("name") + (p.optBoolean("is_active") ? "" : " · Đã ẩn");
                ui.text(card, label, 13, true).setTextColor(ui.color(R.color.primary_green));
            }
            card.addView(ui.button("Phân công sân", false, () -> assign(person)));
        }
        if (cards.getChildCount() == 0) ui.note(cards, "Không có nhân viên phù hợp với bộ lọc.");
    }
    private void assign(JSONObject person) {
        if (saving) return;
        String[] names = new String[pitches.size()]; boolean[] checked = new boolean[pitches.size()]; JSONArray ids = person.optJSONArray("pitch_ids");
        for (int i = 0; i < pitches.size(); i++) {
            names[i] = pitches.get(i).optString("name") + (pitches.get(i).optBoolean("is_active") ? "" : " · Đã ẩn");
            if (ids != null) for (int j = 0; j < ids.length(); j++) if (pitches.get(i).optString("id").equals(ids.optString(j))) checked[i] = true;
        }
        new AlertDialog.Builder(activity).setTitle("Phân công sân")
                .setMultiChoiceItems(names, checked, (d, index, value) -> checked[index] = value)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", (d, w) -> {
                    JSONArray selected = new JSONArray();
                    for (int i = 0; i < checked.length; i++) if (checked[i]) selected.put(pitches.get(i).optString("id"));
                    JSONObject payload = new JSONObject();
                    try { payload.put("pitch_ids", selected); } catch (org.json.JSONException e) { throw new IllegalArgumentException(e); }
                    write(true, "/v1/admin/staff/" + person.optString("id") + "/pitches", payload, this::refresh);
                }).show();
    }
}
