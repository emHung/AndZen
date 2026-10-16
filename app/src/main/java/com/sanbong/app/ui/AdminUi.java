package com.sanbong.app.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.sanbong.app.R;
import java.util.Locale;
import java.util.function.Consumer;

/** Shared native controls for the admin sections, using the app's theme tokens. */
final class AdminUi {
    final Activity activity;
    AdminUi(Activity activity) { this.activity = activity; }
    int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    int color(int id) { return activity.getColor(id); }
    LinearLayout column() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.VERTICAL); return v; }
    LinearLayout row() { LinearLayout v = new LinearLayout(activity); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    void round(View view, int background, int border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color(background)); d.setCornerRadius(dp(14));
        if (border != 0) d.setStroke(dp(1), color(border)); view.setBackground(d);
    }
    LinearLayout card(LinearLayout parent) {
        LinearLayout v = column(); v.setPadding(dp(12), dp(12), dp(12), dp(12)); round(v, R.color.surface, R.color.border);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(12); parent.addView(v, p); return v;
    }
    TextView text(LinearLayout parent, String label, int size, boolean bold) {
        TextView t = new TextView(activity); t.setText(label); t.setTextSize(size); t.setTextColor(color(R.color.text_primary));
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(4), 0, dp(4)); parent.addView(t); return t;
    }
    TextView note(LinearLayout parent, String label) {
        TextView t = text(parent, label, 12, false); t.setTextColor(color(R.color.text_muted)); return t;
    }
    MaterialButton button(String label, boolean primary, Runnable action) {
        MaterialButton b = new MaterialButton(activity); b.setTextSize(12); b.setAllCaps(false);
        int icon = 0;
        if (label.startsWith("＋") || label.startsWith("+")) icon = R.drawable.ic_ri_add_line;
        else if (label.startsWith("Khóa giờ")) icon = R.drawable.ic_ri_lock_line;
        else if (label.startsWith("Mở giờ")) icon = R.drawable.ic_ri_lock_unlock_line;
        else if (label.startsWith("Xác nhận thu tiền")) icon = R.drawable.ic_ri_check_line;
        else if (label.startsWith("Hủy lịch")) icon = R.drawable.ic_ri_delete_bin;
        else if (label.equals("Chi tiết")) icon = R.drawable.ic_ri_file_list_3_line;
        else if (label.startsWith("Phân công")) icon = R.drawable.ic_ri_group;
        else if (label.startsWith("Xem lịch")) icon = R.drawable.ic_ri_calendar;
        else if (label.contains("▾")) icon = R.drawable.ic_ri_arrow_down_s_line;
        b.setText(label.replace("＋", "").replace("▾", "").replaceFirst("^\\+", "").trim());
        if (icon != 0) {
            b.setIconResource(icon); b.setIconSize(dp(18)); b.setIconPadding(dp(6));
            b.setIconTint(ColorStateList.valueOf(color(primary ? R.color.white : R.color.primary_green)));
            b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        }
        b.setCornerRadius(dp(10)); b.setInsetTop(0); b.setInsetBottom(0); b.setMinHeight(dp(48));
        b.setPadding(dp(10), dp(6), dp(10), dp(6)); b.setTextColor(color(primary ? R.color.white : R.color.primary_green));
        b.setBackgroundTintList(ColorStateList.valueOf(color(primary ? R.color.primary_green : R.color.brand_100)));
        b.setOnClickListener(v -> action.run()); return b;
    }
    void weighted(LinearLayout row, View child) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMargins(dp(2), dp(4), dp(2), dp(4)); row.addView(child, p);
    }
    void title(LinearLayout parent, String label, Runnable refresh) {
        LinearLayout r = row(); TextView title = new TextView(activity); title.setText(label); title.setTextSize(20); title.setTypeface(null, 1); title.setTextColor(color(R.color.text_primary));
        r.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton reload = button("↻", false, refresh); reload.setContentDescription("Làm mới " + label.toLowerCase(Locale.ROOT));
        reload.setText(""); reload.setIconResource(R.drawable.ic_ri_refresh_line); reload.setIconSize(dp(24));
        reload.setIconTint(ColorStateList.valueOf(color(R.color.primary_green))); reload.setIconPadding(0);
        reload.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START); reload.setMinWidth(0); reload.setMinimumWidth(0);
        r.addView(reload, new LinearLayout.LayoutParams(dp(48), dp(48))); parent.addView(r);
    }
    EditText search(LinearLayout parent, String hint, String initial, Consumer<String> changed) {
        EditText edit = new EditText(activity); edit.setSingleLine(true); edit.setTextSize(14); edit.setHint(hint); edit.setText(initial);
        edit.setTextColor(color(R.color.text_primary)); edit.setHintTextColor(color(R.color.text_muted));
        edit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ri_search, 0, 0, 0); edit.setCompoundDrawableTintList(ColorStateList.valueOf(color(R.color.text_muted)));
        edit.setCompoundDrawablePadding(dp(10)); edit.setPadding(dp(12), 0, dp(12), 0); round(edit, R.color.surface, R.color.border);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.setMargins(0, dp(12), 0, dp(8)); parent.addView(edit, p);
        edit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { changed.accept(s.toString()); }
            public void afterTextChanged(Editable e) { }
        }); return edit;
    }
    void stats(LinearLayout parent, String[] labels, int[] counts) {
        LinearLayout r = row(); round(r, R.color.brand_100, 0); r.setPadding(dp(8), dp(10), dp(8), dp(10));
        for (int i = 0; i < labels.length; i++) {
            LinearLayout cell = column(); note(cell, labels[i]); TextView count = text(cell, String.valueOf(counts[i]), 22, true); count.setTextColor(color(R.color.primary_green));
            weighted(r, cell);
        }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(12); parent.addView(r, p);
    }
    void chips(LinearLayout parent, String[] labels, int selected, java.util.function.IntConsumer choose) {
        HorizontalScrollView scroll = new HorizontalScrollView(activity); scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout r = row(); r.setPadding(0, dp(4), 0, dp(6));
        for (int i = 0; i < labels.length; i++) {
            final int index = i; boolean active = i == selected;
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(activity);
            chip.setText(labels[i]); chip.setTextSize(13); chip.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            chip.setCheckable(true); chip.setChecked(active); chip.setCheckedIconVisible(true);
            chip.setCheckedIconResource(R.drawable.ic_ri_check_line); chip.setCheckedIconTint(ColorStateList.valueOf(color(R.color.white)));
            chip.setChipIconSize(dp(16)); chip.setChipMinHeight(dp(36)); chip.setEnsureMinTouchTargetSize(true);
            chip.setShapeAppearanceModel(chip.getShapeAppearanceModel().toBuilder().setAllCornerSizes(dp(18)).build());
            chip.setChipStartPadding(dp(12)); chip.setChipEndPadding(dp(12));
            chip.setTextStartPadding(dp(4)); chip.setTextEndPadding(dp(4));
            chip.setChipBackgroundColor(ColorStateList.valueOf(color(active ? R.color.primary_green : R.color.surface)));
            chip.setChipStrokeColor(ColorStateList.valueOf(color(active ? R.color.primary_green : R.color.border)));
            chip.setChipStrokeWidth(dp(1)); chip.setTextColor(color(active ? R.color.white : R.color.text_secondary));
            chip.setOnClickListener(v -> choose.accept(index));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(48)); p.rightMargin = i == labels.length - 1 ? 0 : dp(8);
            r.addView(chip, p);
        }
        scroll.addView(r); parent.addView(scroll, new LinearLayout.LayoutParams(-1,-2));
    }

    LinearLayout dropdown(String label, String value, int icon, Runnable action) {
        LinearLayout field = row(); field.setMinimumHeight(dp(64)); field.setPadding(dp(10),dp(8),dp(10),dp(8));
        round(field,R.color.surface,R.color.border); field.setClickable(true); field.setFocusable(true);
        android.util.TypedValue ripple = new android.util.TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true);
        field.setForeground(activity.getDrawable(ripple.resourceId));
        ImageView leading = new ImageView(activity); leading.setImageResource(icon); leading.setImageTintList(ColorStateList.valueOf(color(R.color.primary_green)));
        field.addView(leading,new LinearLayout.LayoutParams(dp(18),dp(18)));
        LinearLayout copy = column(); copy.setPadding(dp(8),0,dp(6),0);
        TextView caption = text(copy,label,11,false); caption.setPadding(0,0,0,dp(3)); caption.setTextColor(color(R.color.text_muted));
        TextView chosen = text(copy,value,13,true); chosen.setPadding(0,0,0,0); chosen.setMaxLines(2); chosen.setEllipsize(android.text.TextUtils.TruncateAt.END);
        field.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        ImageView arrow = new ImageView(activity); arrow.setImageResource(R.drawable.ic_ri_arrow_down_s_line);
        arrow.setImageTintList(ColorStateList.valueOf(color(R.color.text_muted))); field.addView(arrow,new LinearLayout.LayoutParams(dp(16),dp(16)));
        field.setContentDescription(label + ": " + value); field.setOnClickListener(v -> action.run()); return field;
    }
    void badge(LinearLayout parent, String label, boolean warning) {
        TextView t = text(parent, label, 12, true); t.setTextColor(color(warning ? R.color.warning : R.color.primary_green));
    }
    void divider(LinearLayout parent) { View v = new View(activity); v.setBackgroundColor(color(R.color.border)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1)); p.setMargins(0, dp(8), 0, dp(8)); parent.addView(v, p); }
    static String key(String value) { return java.text.Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd'); }
}
