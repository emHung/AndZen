package com.sanbong.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.sanbong.app.R;
import com.sanbong.app.data.PitchMapper;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.util.CurrencyUtils;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Owns the pitch catalog presentation; management writes remain in AdminActivity. */
final class AdminPitchSection {
    static void show(LinearLayout parent, List<JSONObject> rows, boolean canEdit,
                     String query, Consumer<String> onQuery, Runnable onAdd,
                     Consumer<JSONObject> onEdit, Consumer<String> onSlots) {
        Context context = parent.getContext();
        View section = LayoutInflater.from(context).inflate(R.layout.section_admin_pitches, parent, false);
        parent.addView(section);
        int active = 0;
        for (JSONObject row : rows) if (row.optBoolean("is_active")) active++;
        ((TextView) section.findViewById(R.id.adminTotalPitches)).setText("Tổng số sân\n" + rows.size());
        ((TextView) section.findViewById(R.id.adminActivePitches)).setText("Đang hoạt động\n" + active);
        View add = section.findViewById(R.id.adminAddPitch);
        add.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        add.setOnClickListener(v -> onAdd.run());
        LinearLayout cards = section.findViewById(R.id.adminPitchCards);
        TextView empty = section.findViewById(R.id.adminEmptyPitches);
        EditText search = section.findViewById(R.id.adminSearchPitches);
        Consumer<String> filter = term -> {
            cards.removeAllViews();
            String keyword = searchKey(term);
            for (JSONObject row : rows) {
                String searchable = row.optString("name") + " " + row.optString("district") + " " + row.optString("address");
                if (searchKey(searchable).contains(keyword)) {
                    bindCard(cards, row, canEdit, onEdit, onSlots);
                }
            }
            empty.setVisibility(cards.getChildCount() == 0 ? View.VISIBLE : View.GONE);
            empty.setText(rows.isEmpty() ? "Chưa có sân nào được phân quyền."
                    : "Không tìm thấy sân phù hợp. Thử tên sân hoặc khu vực khác.");
        };
        search.setText(query);
        filter.accept(query);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                onQuery.accept(s.toString());
                filter.accept(s.toString());
            }
            @Override public void afterTextChanged(Editable value) { }
        });
    }

    private static String searchKey(String value) {
        return java.text.Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    private static void bindCard(LinearLayout parent, JSONObject row, boolean canEdit,
                                 Consumer<JSONObject> onEdit, Consumer<String> onSlots) {
        Context context = parent.getContext();
        View card = LayoutInflater.from(context).inflate(R.layout.item_admin_pitch, parent, false);
        Pitch pitch = PitchMapper.fromJson(row);
        ImageView image = card.findViewById(R.id.adminPitchImage);
        com.sanbong.app.util.PitchImageLoader.load(image, pitch);
        TextView status = card.findViewById(R.id.adminPitchStatus);
        boolean active = row.optBoolean("is_active");
        status.setText(active ? "● Đang hoạt động" : "● Đã ẩn");
        status.setBackgroundResource(R.drawable.bg_tag_light_green);
        status.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                active ? R.color.primary_green : R.color.slot_booked_bg)));
        status.setTextColor(ContextCompat.getColor(context, active ? R.color.white : R.color.text_secondary));
        ((TextView) card.findViewById(R.id.adminPitchName)).setText(pitch.getName());
        ((TextView) card.findViewById(R.id.adminPitchType)).setText(pitch.getType().getDisplayName());
        ((TextView) card.findViewById(R.id.adminPitchDistrict)).setText(pitch.getDistrict());
        ((TextView) card.findViewById(R.id.adminPitchAddress)).setText(pitch.getAddress());
        ((TextView) card.findViewById(R.id.adminPitchPrice)).setText(CurrencyUtils.formatVnd(pitch.getMinPrice()) + " / khung giờ");
        View edit = card.findViewById(R.id.adminEditPitch);
        edit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        edit.setOnClickListener(v -> onEdit.accept(row));
        card.findViewById(R.id.adminViewPitchSlots).setOnClickListener(v -> onSlots.accept(pitch.getId()));
        parent.addView(card);
    }
}
