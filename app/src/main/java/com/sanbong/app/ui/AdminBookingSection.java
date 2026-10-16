package com.sanbong.app.ui;

import android.app.DatePickerDialog;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.sanbong.app.R;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

final class AdminBookingSection extends AdminSection {
    private final List<JSONObject> pitches;
    private JSONArray rows = new JSONArray();
    private LinearLayout cards;
    private String query = "", pitchId, date, slotId;
    private int status;
    AdminBookingSection(AppCompatActivity activity, LinearLayout parent, ExecutorService executor, List<JSONObject> pitches) {
        super(activity, parent, executor); this.pitches = pitches;
    }
    void focusSlot(String id, String pitch, String day) { slotId = id; pitchId = pitch; date = day; status = 1; }
    void show() { parent.removeAllViews(); ui.note(parent, "Đang tải lịch đặt…"); refresh(); }
    private void refresh() { read("/v1/admin/bookings", values -> { rows = values; render(); }, this::show); }
    private void render() {
        parent.removeAllViews();
        int today = 0, upcoming = 0, unpaid = 0;
        String now = DateTimeUtils.getNextDays(1).get(0);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject b = rows.optJSONObject(i); if (b == null || !"confirmed".equals(b.optString("status"))) continue;
            if (now.equals(b.optString("booking_date"))) today++;
            if (!DateTimeUtils.hasBookingEnded(b.optString("booking_date"), b.optString("time_range"))) upcoming++;
            if (!"paid".equals(b.optString("payment_status"))) unpaid++;
        }
        ui.stats(parent, new String[]{"Hôm nay", "Sắp tới", "Chưa thu"}, new int[]{today, upcoming, unpaid});
        ui.title(parent, "Quản lý lịch đặt", this::refresh);
        ui.search(parent, "Tìm mã đặt, tên khách, số điện thoại…", query, text -> { query = text; filter(); });
        LinearLayout filters = ui.row();
        ui.weighted(filters, ui.dropdown("Sân bóng",pitchName(),R.drawable.ic_ri_football,this::choosePitch));
        ui.weighted(filters, ui.dropdown("Ngày chơi",date == null ? "Tất cả ngày" : DateTimeUtils.formatDisplayDate(date),R.drawable.ic_ri_calendar,this::chooseDate));
        parent.addView(filters);
        ui.chips(parent, new String[]{"Tất cả", "Đã xác nhận", "Đã hủy"}, status, value -> { status = value; render(); });
        if (pitchId != null || date != null || slotId != null || !query.isEmpty()) {
            parent.addView(ui.button(slotId == null ? "Xóa bộ lọc" : "Đang lọc một khung giờ · Xóa lọc", false, () -> {
                pitchId = date = slotId = null; query = ""; status = 0; render();
            }));
        }
        cards = ui.column(); parent.addView(cards); filter();
    }
    private String pitchName() {
        for (JSONObject p : pitches) if (p.optString("id").equals(pitchId)) return p.optString("name");
        return "Tất cả sân";
    }
    private void choosePitch() {
        String[] names = new String[pitches.size() + 1]; names[0] = "Tất cả sân";
        for (int i = 0; i < pitches.size(); i++) names[i + 1] = pitches.get(i).optString("name");
        new AlertDialog.Builder(activity).setTitle("Chọn sân").setItems(names, (d, index) -> {
            pitchId = index == 0 ? null : pitches.get(index - 1).optString("id"); slotId = null; render();
        }).show();
    }
    private void chooseDate() {
        Calendar c = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        if (date != null) { String[] p = date.split("-"); c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1])-1, Integer.parseInt(p[2])); }
        new DatePickerDialog(activity, (v, y, m, d) -> {
            date = String.format(Locale.ROOT, "%04d-%02d-%02d", y, m+1, d); slotId = null; render();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void filter() {
        if (cards == null) return;
        cards.removeAllViews(); String keyword = AdminUi.key(query);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject b = rows.optJSONObject(i); if (b == null) continue;
            if (pitchId != null && !pitchId.equals(b.optString("pitch_id"))) continue;
            if (date != null && !date.equals(b.optString("booking_date"))) continue;
            if (slotId != null && !slotId.equals(b.optString("slot_id"))) continue;
            if (status == 1 && !"confirmed".equals(b.optString("status"))) continue;
            if (status == 2 && !"cancelled".equals(b.optString("status"))) continue;
            if (!AdminUi.key(b.optString("booking_code") + " " + b.optString("customer_name") + " " + b.optString("customer_phone") + " " + b.optString("pitch_name")).contains(keyword)) continue;
            bind(b);
        }
        if (cards.getChildCount() == 0) ui.note(cards, "Không có lịch đặt phù hợp với bộ lọc.");
    }
    private String state(JSONObject b) {
        if ("cancelled".equals(b.optString("status"))) return "Đã hủy";
        return DateTimeUtils.hasBookingEnded(b.optString("booking_date"), b.optString("time_range")) ? "Đã hoàn thành" : "Đã xác nhận";
    }
    private String payment(JSONObject b) {
        if ("paid".equals(b.optString("payment_status"))) return "Đã thu tiền";
        return ("qr".equals(b.optString("payment_method")) ? "QR" : "Tiền mặt") + " · Chưa thu";
    }
    private void bind(JSONObject b) {
        LinearLayout card = ui.card(cards);
        ui.note(card, b.optString("booking_code")); ui.badge(card, state(b), "cancelled".equals(b.optString("status")));
        ui.text(card, b.optString("pitch_name"), 17, true);
        ui.text(card, b.optString("customer_name") + "  ·  " + b.optString("customer_phone"), 13, false);
        ui.text(card, DateTimeUtils.formatDisplayDate(b.optString("booking_date")) + "  |  " + b.optString("time_range"), 13, false);
        ui.divider(card);
        ui.text(card, CurrencyUtils.formatVnd(b.optLong("total_price")), 20, true).setTextColor(ui.color(R.color.primary_green));
        ui.badge(card, payment(b), !"paid".equals(b.optString("payment_status")));
        LinearLayout actions = ui.row();
        if ("confirmed".equals(b.optString("status")) && !"paid".equals(b.optString("payment_status"))) {
            ui.weighted(actions, ui.button("Xác nhận thu tiền", true, () -> confirm(b, true)));
        }
        if ("confirmed".equals(b.optString("status")) && !DateTimeUtils.hasBookingStarted(b.optString("booking_date"), b.optString("time_range"))) {
            com.google.android.material.button.MaterialButton cancel = ui.button("Hủy lịch", false, () -> confirm(b, false));
            cancel.setTextColor(ui.color(R.color.danger)); cancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ui.color(R.color.danger_bg))); ui.weighted(actions, cancel);
        }
        card.addView(actions);
        card.addView(ui.button("Chi tiết", false, () -> new AlertDialog.Builder(activity).setTitle(b.optString("booking_code"))
                .setMessage(b.optString("pitch_name") + "\n" + b.optString("pitch_address") + "\n\n" + b.optString("customer_name")
                        + "\n" + b.optString("customer_phone") + "\n" + DateTimeUtils.formatDisplayDate(b.optString("booking_date"))
                        + "\n" + b.optString("time_range") + "\n\n" + CurrencyUtils.formatVnd(b.optLong("total_price")) + "\n" + state(b) + " · " + payment(b))
                .setPositiveButton("Đóng", null).show()));
    }
    private void confirm(JSONObject b, boolean payment) {
        if (saving) return;
        new AlertDialog.Builder(activity).setTitle(payment ? "Xác nhận đã thu đủ tiền?" : "Hủy lịch đặt?")
                .setMessage(b.optString("booking_code") + " · " + b.optString("customer_name")
                        + (payment ? "\nChỉ xác nhận sau khi đã nhận đủ " + CurrencyUtils.formatVnd(b.optLong("total_price")) : "\nKhung giờ sẽ được mở lại sau khi hủy."))
                .setNegativeButton("Quay lại", null).setPositiveButton(payment ? "Đã nhận tiền" : "Hủy lịch", (d, w) -> {
                    write(false, "/v1/admin/bookings/" + b.optString("id") + (payment ? "/payment" : ""),
                            body(payment ? "payment_status" : "status", payment ? "paid" : "cancelled"), this::refresh);
                }).show();
    }
}
