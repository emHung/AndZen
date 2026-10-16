package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ArrayList;

import com.sanbong.app.util.DateTimeUtils;

public class PitchFilterActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "filter_type_index";
    public static final String EXTRA_DISTRICT = "filter_district";
    public static final String EXTRA_DATE = "filter_date";
    public static final String EXTRA_MAX_PRICE = "filter_max_price";
    public static final String EXTRA_TIME_PERIOD = "filter_time_period";
    public static final String EXTRA_AMENITIES = "filter_amenities";

    private TextView tvDistrict, tvDate, tvMaxPrice;
    private TextView chipType5, chipType7, chipType11;
    private MaterialCheckBox cbLed, cbParking, cbWater, cbShoes;
    private MaterialButton btnApply;
    private int selectedType = 0;
    private android.widget.LinearLayout timeChipRow;
    private int timeRequestGeneration;
    private final ExecutorService filterExecutor = Executors.newSingleThreadExecutor();
    private String selectedTimeSlot;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pitch_filter);

        initViews();
        setupToolbar();
        setupDistrictAndDate();
        setupPitchTypeChips();
        setupPriceSeekbar();
        setupTimeChips();
        setupBottomButtons();
    }

    private void initViews() {
        tvDistrict = findViewById(R.id.tvFilterDistrict);
        tvDate = findViewById(R.id.tvFilterDate);
        tvMaxPrice = findViewById(R.id.tvMaxPrice);
        chipType5 = findViewById(R.id.chipType5);
        chipType7 = findViewById(R.id.chipType7);
        chipType11 = findViewById(R.id.chipType11);
        timeChipRow = findViewById(R.id.timeChipRow);
        cbLed = findViewById(R.id.cbLed);
        cbParking = findViewById(R.id.cbParking);
        cbWater = findViewById(R.id.cbWater);
        cbShoes = findViewById(R.id.cbShoes);
        btnApply = findViewById(R.id.btnApplyFilter);
        tvDistrict.setText("Tất cả khu vực");
        tvDate.setText("Bất kỳ ngày nào");
        cbLed.setChecked(false); cbParking.setChecked(false); cbWater.setChecked(false); cbShoes.setChecked(false);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarFilter);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnResetFilter).setOnClickListener(v -> resetFilters());
    }

    private void setupDistrictAndDate() {
        findViewById(R.id.btnFilterDistrict).setOnClickListener(v -> {
            String[] list = {"Thanh Xuân, Hà Nội", "Cầu Giấy, Hà Nội", "Nam Từ Liêm, Hà Nội", "Tất cả khu vực"};
            com.sanbong.app.util.ThemePickerUtils.showOptionPicker(this, "Chọn khu vực", "Lọc các sân theo quận huyện",
                    R.drawable.ic_ri_map_pin, list, tvDistrict.getText().toString(), tvDistrict::setText);
        });
        findViewById(R.id.btnFilterDate).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            com.sanbong.app.util.ThemePickerUtils.showDatePicker(this, c, Calendar.getInstance(),
                    DateTimeUtils.getLatestBookableDay(), (v1, y, m, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(y, m, day);
                selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(selected.getTime());
                tvDate.setText(DateTimeUtils.formatDisplayDate(selectedDate));
                setupTimeChips();
            });
        });
    }

    private void setupPitchTypeChips() {
        selectType(0);
        chipType5.setOnClickListener(v -> selectType(5));
        chipType7.setOnClickListener(v -> selectType(7));
        chipType11.setOnClickListener(v -> selectType(11));
    }

    private void selectType(int type) {
        selectedType = type;
        chipType5.setBackgroundResource(type == 5 ? R.drawable.bg_btn_primary : R.drawable.bg_input_field);
        chipType5.setTextColor(type == 5 ? 0xFFFFFFFF : 0xFF1E293B);
        chipType5.setText(type == 5 ? "✓ Sân 5" : "Sân 5");

        chipType7.setBackgroundResource(type == 7 ? R.drawable.bg_btn_primary : R.drawable.bg_input_field);
        chipType7.setTextColor(type == 7 ? 0xFFFFFFFF : 0xFF1E293B);
        chipType7.setText(type == 7 ? "✓ Sân 7" : "Sân 7");

        chipType11.setBackgroundResource(type == 11 ? R.drawable.bg_btn_primary : R.drawable.bg_input_field);
        chipType11.setTextColor(type == 11 ? 0xFFFFFFFF : 0xFF1E293B);
        chipType11.setText(type == 11 ? "✓ Sân 11" : "Sân 11");
    }

    private void setupPriceSeekbar() {
        SeekBar sb = findViewById(R.id.seekBarPrice);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean f) {
                int price = Math.max(200, p);
                tvMaxPrice.setText(price + ".000 đ");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void setupTimeChips() {
        android.widget.LinearLayout row = timeChipRow;
        row.removeAllViews();
        TextView all = createTimeChip("Tất cả khung giờ");
        all.setOnClickListener(v -> selectTimeSlot(null));
        row.addView(all);
        final String requestedDate = selectedDate;
        final int generation = ++timeRequestGeneration;
        filterExecutor.execute(() -> {
            try {
                org.json.JSONArray ranges = ApiClient.getPublicArray("/v1/time-ranges" + (requestedDate == null ? "" : "?date=" + requestedDate));
                runOnUiThread(() -> {
                    if (isFinishing() || generation != timeRequestGeneration || !java.util.Objects.equals(requestedDate, selectedDate)) return;
                    for (int i = 0; i < ranges.length(); i++) {
                        String range = ranges.optString(i);
                        TextView chip = createTimeChip(range);
                        chip.setOnClickListener(v -> selectTimeSlot(range));
                        timeChipRow.addView(chip);
                    }
                    paintTimeSelection();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this, "Không tải được khung giờ bộ lọc: " + exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        paintTimeSelection();
    }

    private TextView createTimeChip(String label) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextColor(0xFF1E293B);
        chip.setTextSize(13);
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(14, 0, 14, 0);
        chip.setBackgroundResource(R.drawable.bg_input_field);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 8, 0);
        chip.setLayoutParams(params);
        return chip;
    }

    private void selectTimeSlot(String range) {
        selectedTimeSlot = range == null || range.equals(selectedTimeSlot) ? null : range;
        paintTimeSelection();
    }

    private void paintTimeSelection() {
        android.widget.LinearLayout row = timeChipRow;
        if (row == null) return;
        for (int i = 0; i < row.getChildCount(); i++) {
            TextView chip = (TextView) row.getChildAt(i);
            boolean active = selectedTimeSlot == null
                    ? i == 0 : chip.getText().toString().equals(selectedTimeSlot);
            chip.setBackgroundResource(active ? R.drawable.bg_btn_primary : R.drawable.bg_input_field);
            chip.setTextColor(active ? 0xFFFFFFFF : 0xFF1E293B);
        }
    }


    private void resetFilters() {
        tvDistrict.setText("Tất cả khu vực");
        tvDate.setText("Bất kỳ ngày nào");
        selectedDate = null;
        selectType(0);
        selectTimeSlot(null);
        ((SeekBar) findViewById(R.id.seekBarPrice)).setProgress(1000);
        cbLed.setChecked(false);
        cbParking.setChecked(false);
        cbWater.setChecked(false);
        cbShoes.setChecked(false);
        setupTimeChips();
        Toast.makeText(this, "Đã đặt lại bộ lọc", Toast.LENGTH_SHORT).show();
    }

    private void setupBottomButtons() {
        findViewById(R.id.btnClearFilter).setOnClickListener(v -> resetFilters());
        btnApply.setOnClickListener(v -> {
            Intent res = new Intent();
            res.putExtra(EXTRA_TYPE, selectedType == 5 ? 1 : selectedType == 7 ? 2 : selectedType == 11 ? 3 : 0);
            res.putExtra(EXTRA_DISTRICT, tvDistrict.getText().toString());
            res.putExtra(EXTRA_DATE, selectedDate);
            res.putExtra(EXTRA_MAX_PRICE, Math.max(200, ((SeekBar) findViewById(R.id.seekBarPrice)).getProgress()) * 1000L);
            res.putExtra(EXTRA_TIME_PERIOD, selectedTimeSlot == null ? "ALL" : selectedTimeSlot);
            ArrayList<String> amenities = new ArrayList<>();
            if (cbLed.isChecked()) amenities.add("Đèn LED");
            if (cbParking.isChecked()) amenities.add("Bãi xe");
            if (cbWater.isChecked()) amenities.add("Nước miễn phí");
            if (cbShoes.isChecked()) amenities.add("Cho thuê giày");
            res.putStringArrayListExtra(EXTRA_AMENITIES, amenities);
            setResult(RESULT_OK, res);
            finish();
        });
    }

    @Override protected void onDestroy() {
        filterExecutor.shutdownNow();
        super.onDestroy();
    }
}
