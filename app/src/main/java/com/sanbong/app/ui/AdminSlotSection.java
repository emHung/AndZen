package com.sanbong.app.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

final class AdminSlotSection extends AdminSection {
    interface OpenBooking { void open(String slotId, String pitchId, String day); }
    private final List<JSONObject> pitches;
    private final OpenBooking openBooking;
    private String pitchId, day = DateTimeUtils.getNextDays(1).get(0);
    private JSONArray slots = new JSONArray();
    AdminSlotSection(AppCompatActivity activity, LinearLayout parent, ExecutorService executor,
                     List<JSONObject> pitches, String pitchId, OpenBooking openBooking) {
        super(activity, parent, executor); this.pitches = pitches; this.pitchId = pitchId; this.openBooking = openBooking;
    }
    void show() {
        if (pitches.isEmpty()) { ui.note(parent, "Chưa có sân nào được phân quyền."); return; }
        boolean found = false; for (JSONObject p : pitches) if (p.optString("id").equals(pitchId)) found = true;
        if (!found) pitchId = pitches.get(0).optString("id");
        parent.removeAllViews(); ui.note(parent, "Đang tải khung giờ…"); refresh();
    }
    private void refresh() {
        read("/v1/admin/pitches/" + android.net.Uri.encode(pitchId) + "/slots?slot_date=" + day,
                values -> { slots = values; render(); }, this::show);
    }
    private JSONObject pitch() { for (JSONObject p : pitches) if (p.optString("id").equals(pitchId)) return p; return pitches.get(0); }
    private void render() {
        parent.removeAllViews(); LinearLayout selector = ui.card(parent);
        LinearLayout fields = ui.row();
        ui.weighted(fields,ui.dropdown("Sân bóng",pitch().optString("name"),R.drawable.ic_ri_football,this::choosePitch));
        ui.weighted(fields,ui.dropdown("Ngày chơi",day.substring(8,10) + "/" + day.substring(5,7) + "/" + day.substring(0,4),R.drawable.ic_ri_calendar,this::chooseDate));
        selector.addView(fields);
        List<String> days = DateTimeUtils.getBookableDates(); String[] labels = new String[days.size()];
        for (int i = 0; i < days.size(); i++) labels[i] = DateTimeUtils.formatDisplayDate(days.get(i));
        ui.chips(selector, labels, days.indexOf(day), index -> { day = days.get(index); show(); });
        int available = 0, booked = 0, blocked = 0, elapsed = 0;
        for (int i = 0; i < slots.length(); i++) {
            JSONObject s = slots.optJSONObject(i); if (s == null) continue;
            if ("booked".equals(s.optString("status"))) booked++;
            else if (past(s)) elapsed++;
            else if ("available".equals(s.optString("status"))) available++;
            else blocked++;
        }
        ui.stats(parent, new String[]{"Còn trống", "Đã đặt", "Đã khóa"}, new int[]{available, booked, blocked});
        if (elapsed > 0) ui.note(parent, elapsed + " khung giờ đã qua (không tính vào giờ còn trống).");
        ui.title(parent, "Khung giờ trong ngày", this::refresh);
        com.google.android.material.button.MaterialButton add = ui.button("＋ Tạo khung giờ", true, this::createSlot);
        add.setEnabled(pitch().optBoolean("is_active")); parent.addView(add);
        if (!pitch().optBoolean("is_active")) ui.note(parent, "Sân đã ẩn. Không thể tạo khung giờ mới.");
        ui.note(parent, "● Còn trống     ● Đã đặt     ● Đã khóa");
        LinearLayout gridRow = null;
        for (int i = 0; i < slots.length(); i++) {
            JSONObject slot = slots.optJSONObject(i); if (slot == null) continue;
            if (i % 2 == 0) { gridRow = ui.row(); gridRow.setGravity(android.view.Gravity.TOP); parent.addView(gridRow); }
            LinearLayout cell = ui.column(); ui.weighted(gridRow, cell); LinearLayout card = ui.card(cell);
            ui.text(card, range(slot), 14, true);
            boolean bookedSlot = "booked".equals(slot.optString("status"));
            boolean blockedSlot = "unavailable".equals(slot.optString("status"));
            TextView state = ui.text(card, bookedSlot ? "● Đã đặt" : past(slot) ? "Đã qua" : blockedSlot ? "● Đã khóa" : "● Còn trống", 12, true);
            state.setTextColor(ui.color(bookedSlot || past(slot) ? R.color.text_muted : blockedSlot ? R.color.warning : R.color.primary_green));
            ui.text(card, CurrencyUtils.formatVnd(slot.optLong("price")), 17, true).setTextColor(ui.color(R.color.primary_green));
            if (bookedSlot) card.addView(ui.button("Xem lịch đặt", false, () -> openBooking.open(slot.optString("id"), pitchId, day)));
            else {
                com.google.android.material.button.MaterialButton change = ui.button(blockedSlot ? "Mở giờ" : "Khóa giờ", false, () -> changeSlot(slot, blockedSlot));
                change.setEnabled(!past(slot)); card.addView(change);
            }
        }
        if (slots.length() % 2 == 1) ui.weighted(gridRow, ui.column());
        if (slots.length() == 0) ui.note(parent, "Ngày này chưa có khung giờ.");
        ui.note(parent, "Chỉ quản lý khung giờ trong 7 ngày tới.");
    }
    private String range(JSONObject slot) {
        String start = slot.optString("starts_at"), end = slot.optString("ends_at");
        return (start.length() >= 5 ? start.substring(0,5) : start) + " - " + (end.length() >= 5 ? end.substring(0,5) : end);
    }
    private boolean past(JSONObject slot) { return DateTimeUtils.hasBookingStarted(day, range(slot)); }
    private void choosePitch() {
        String[] labels = new String[pitches.size()]; for (int i = 0; i < labels.length; i++) labels[i] = pitches.get(i).optString("name");
        new AlertDialog.Builder(activity).setTitle("Chọn sân bóng").setItems(labels, (d, index) -> { pitchId = pitches.get(index).optString("id"); show(); }).show();
    }
    private void chooseDate() {
        Calendar c = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String[] p = day.split("-"); c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1])-1, Integer.parseInt(p[2]));
        DatePickerDialog dialog = new DatePickerDialog(activity, (v,y,m,d) -> { day = String.format(Locale.ROOT,"%04d-%02d-%02d",y,m+1,d); show(); }, c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
        Calendar first = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); first.set(Calendar.HOUR_OF_DAY,0); first.set(Calendar.MINUTE,0); first.set(Calendar.SECOND,0);
        dialog.getDatePicker().setMinDate(first.getTimeInMillis()); dialog.getDatePicker().setMaxDate(DateTimeUtils.getLatestBookableDay().getTimeInMillis()); dialog.show();
    }
    private void changeSlot(JSONObject slot, boolean opening) {
        if (saving) return;
        new AlertDialog.Builder(activity).setTitle(opening ? "Mở khung giờ?" : "Khóa khung giờ?")
                .setMessage(pitch().optString("name") + "\n" + DateTimeUtils.formatDisplayDate(day) + " · " + range(slot))
                .setNegativeButton("Hủy", null).setPositiveButton("Xác nhận", (d,w) -> write(false,
                        "/v1/admin/slots/" + android.net.Uri.encode(slot.optString("id")), body("status", opening ? "available" : "unavailable"), this::refresh)).show();
    }
    private void createSlot() {
        if (saving) return;
        final String targetPitch = pitchId, targetDate = day;
        LinearLayout fields = ui.column(); fields.setPadding(ui.dp(20),ui.dp(4),ui.dp(20),ui.dp(8));
        ui.note(fields, pitch().optString("name") + " · " + DateTimeUtils.formatDisplayDate(targetDate));
        Button start = ui.button("18:00", false, () -> {}), end = ui.button("19:30", false, () -> {});
        ui.note(fields, "Giờ bắt đầu"); fields.addView(start); ui.note(fields,"Giờ kết thúc"); fields.addView(end);
        start.setOnClickListener(v -> chooseTime(start)); end.setOnClickListener(v -> chooseTime(end));
        ui.note(fields, "Giá khung giờ (đ)"); EditText price = new EditText(activity); price.setInputType(InputType.TYPE_CLASS_NUMBER); price.setText(String.valueOf(pitch().optLong("base_price"))); fields.addView(price);
        TextView error = ui.note(fields, ""); error.setTextColor(ui.color(R.color.danger));
        ScrollView scroll = new ScrollView(activity); scroll.addView(fields);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Tạo khung giờ").setView(scroll).setNegativeButton("Hủy",null).setPositiveButton("Tạo",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String from = start.getText().toString(), to = end.getText().toString();
            if (to.compareTo(from) <= 0) { error.setText("Giờ kết thúc phải sau giờ bắt đầu."); return; }
            if (DateTimeUtils.hasBookingStarted(targetDate, from + " - " + to)) { error.setText("Giờ bắt đầu đã qua. Hãy chọn giờ khác."); return; }
            long amount; try { amount = Long.parseLong(price.getText().toString().trim()); } catch (NumberFormatException e) { error.setText("Nhập giá hợp lệ."); return; }
            if (amount <= 0 || amount > 100_000_000) { error.setText("Giá phải lớn hơn 0 và không quá 100 triệu đồng."); return; }
            if (saving) return; saving = true; dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            JSONObject payload = body("slot_date",targetDate);
            try { payload.put("starts_at",from).put("ends_at",to).put("price",amount); } catch (org.json.JSONException e) { throw new IllegalArgumentException(e); }
            executor.execute(() -> {
                try {
                    ApiClient.post(activity,"/v1/admin/pitches/" + android.net.Uri.encode(targetPitch) + "/slots",payload);
                    activity.runOnUiThread(() -> { saving = false; dialog.dismiss(); if (visible()) refresh(); });
                } catch (Exception e) { activity.runOnUiThread(() -> { saving = false; if (dialog.isShowing()) { error.setText(e.getMessage()); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); } }); }
            });
        })); dialog.show();
    }
    private void chooseTime(Button button) {
        String[] parts = button.getText().toString().split(":");
        new TimePickerDialog(activity,(v,h,m) -> button.setText(String.format(Locale.ROOT,"%02d:%02d",h,m)),Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),true).show();
    }
}
