package com.sanbong.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.TimeSlotMapper;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.ui.adapter.DateChipAdapter;
import com.sanbong.app.ui.adapter.TimeSlotAdapter;
import com.sanbong.app.util.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
public class PitchDetailActivity extends AppCompatActivity {
    private Pitch pitch;
    private String selectedDate;
    private TimeSlot selectedSlot;
    private boolean filterOnlyAvailable = true;
    private TextView tvSummary, tvPrice, tvAvailableTitle, btnToggleFilter;
    private MaterialButton btnProceed; private View layoutNoSlots;
    private RecyclerView rvDates, rvSlots; private DateChipAdapter dateAdapter; private TimeSlotAdapter slotAdapter;
    private final ExecutorService slotExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService favoriteExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean isFavorite;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pitch_detail);
        pitch = (Pitch) getIntent().getSerializableExtra("pitch");
        if (pitch == null) { finish(); return; }
        initViews();
        bindPitchInfo();
        setupToolbar();
        setupDateList();
        setupSlotGrid();
        setupFilterAndCalendar();
    }
    private void initViews() {
        tvSummary = findViewById(R.id.tvSelectedSlotSummary); tvPrice = findViewById(R.id.tvSelectedSlotPrice);
        tvAvailableTitle = findViewById(R.id.tvAvailableSlotsTitle); btnToggleFilter = findViewById(R.id.btnToggleSlotFilter);
        layoutNoSlots = findViewById(R.id.layoutNoSlots); btnProceed = findViewById(R.id.btnProceedBooking);
        rvDates = findViewById(R.id.rvDates); rvSlots = findViewById(R.id.rvSlots);
    }
    private void setupToolbar() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        updateFavoriteIcon();
        ImageView btnFav = findViewById(R.id.btnFavoriteDetail);
        if (btnFav != null) btnFav.setOnClickListener(v -> {
            boolean desired = !isFavorite;
            favoriteExecutor.execute(() -> {
                try {
                    ApiClient.put(this, "/v1/favorites/" + pitch.getId() + "?enabled=" + desired, new org.json.JSONObject());
                    isFavorite = desired;
                    runOnUiThread(() -> { updateFavoriteIcon(); Toast.makeText(this, desired ? "Đã lưu yêu thích" : "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show(); });
                } catch (Exception exception) { runOnUiThread(() -> Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show()); }
            });
        });
        btnProceed.setOnClickListener(v -> {
            if (selectedSlot != null) {
                startActivity(new Intent(this, BookingConfirmActivity.class)
                        .putExtra("pitch", pitch).putExtra("slot", selectedSlot));
            }
        });
    }
    private void updateFavoriteIcon() {
        ImageView btnFav = findViewById(R.id.btnFavoriteDetail);
        if (btnFav != null) {
            favoriteExecutor.execute(() -> {
                try {
                    JSONArray ids = ApiClient.getArray(this, "/v1/favorites");
                    for (int i = 0; i < ids.length(); i++) if (pitch.getId().equals(ids.optString(i))) isFavorite = true;
                } catch (Exception ignored) { }
                runOnUiThread(() -> btnFav.setImageResource(isFavorite ? R.drawable.ic_ri_heart_fill : R.drawable.ic_ri_heart));
            });
        }
    }

    @Override protected void onDestroy() {
        if (dateAdapter != null) dateAdapter.close();
        slotExecutor.shutdownNow();
        favoriteExecutor.shutdownNow();
        super.onDestroy();
    }

    private void bindPitchInfo() {
        ImageView iv = findViewById(R.id.ivDetailBanner);
        if (iv != null && pitch.getImageResId() != 0) iv.setImageResource(pitch.getImageResId());
        ((TextView) findViewById(R.id.tvDetailName)).setText(pitch.getName());
        ((TextView) findViewById(R.id.tvDetailAddress)).setText(pitch.getAddress());
        ((TextView) findViewById(R.id.tvDetailType)).setText(pitch.getType().getDisplayName());
        ((TextView) findViewById(R.id.tvDetailDescription)).setText(pitch.getDescription());
        android.widget.LinearLayout amenities = findViewById(R.id.layoutAmenities);
        if (amenities != null) {
            amenities.removeAllViews();
            for (String label : pitch.getAmenities()) {
                TextView chip = new TextView(this);
                chip.setText(label); chip.setTextSize(12); chip.setTextColor(getColor(R.color.primary_green));
                chip.setBackgroundResource(R.drawable.bg_rating_badge);
                int padding = (int) (10 * getResources().getDisplayMetrics().density);
                chip.setPadding(padding, padding / 2, padding, padding / 2);
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(-2, -2);
                params.setMarginEnd(padding);
                amenities.addView(chip, params);
            }
        }
    }
    private void setupDateList() {
        List<String> nextDays = DateTimeUtils.getBookableDates();
        selectedDate = nextDays.get(0);
        dateAdapter = new DateChipAdapter(pitch.getId(), nextDays, this::selectDate);
        rvDates.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDates.setAdapter(dateAdapter);
        dateAdapter.setSelectedDate(selectedDate);
    }
    private void selectDate(String date) {
        selectedDate = date; selectedSlot = null; updateBottomSummary(); loadSlotsForDate();
    }
    private void setupSlotGrid() {
        slotAdapter = new TimeSlotAdapter(slot -> { selectedSlot = slot; updateBottomSummary(); });
        rvSlots.setLayoutManager(new GridLayoutManager(this, 2));
        rvSlots.setAdapter(slotAdapter);
    }
    @Override protected void onResume() {
        super.onResume();
        if (slotAdapter != null) loadSlotsForDate();
    }
    private void setupFilterAndCalendar() {
        if (btnToggleFilter != null) {
            btnToggleFilter.setOnClickListener(v -> {
                filterOnlyAvailable = !filterOnlyAvailable;
                btnToggleFilter.setText(filterOnlyAvailable ? "✓ Chỉ giờ chưa đặt" : "Hiện tất cả");
                btnToggleFilter.setBackgroundResource(filterOnlyAvailable ? R.drawable.bg_tag_light_green : R.drawable.bg_input_field);
                loadSlotsForDate();
            });
        }
        View btnCal = findViewById(R.id.btnPickCalendar);
        if (btnCal != null) {
            btnCal.setOnClickListener(v -> ThemePickerUtils.showDatePicker(this, Calendar.getInstance(),
                    Calendar.getInstance(), DateTimeUtils.getLatestBookableDay(), (vw, y, m, d) -> {
                Calendar c = Calendar.getInstance(); c.set(y, m, d);
                String picked = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(c.getTime());
                dateAdapter.setSelectedDate(picked);
                selectDate(picked);
            }));
        }
        View btnSug = findViewById(R.id.btnSuggestNextAvailable);
        if (btnSug != null) btnSug.setOnClickListener(v -> {
            java.util.Calendar next = java.util.Calendar.getInstance();
            next.add(java.util.Calendar.DAY_OF_MONTH, 1);
            String nextDate = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(next.getTime());
            dateAdapter.setSelectedDate(nextDate);
            selectDate(nextDate);
        });
    }
    private void loadSlotsForDate() {
        String requestedDate = selectedDate;
        selectedSlot = null;
        updateBottomSummary();
        rvSlots.setVisibility(View.GONE);
        layoutNoSlots.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvNoSlots)).setText("Đang tải khung giờ...");
        findViewById(R.id.btnSuggestNextAvailable).setVisibility(View.GONE);
        if (tvAvailableTitle != null) tvAvailableTitle.setText("Đang tải khung giờ...");
        slotExecutor.execute(() -> {
            try {
                JSONArray response = ApiClient.getArray(this,
                        "/v1/pitches/" + pitch.getId() + "/slots?date=" + requestedDate);
                List<TimeSlot> all = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    org.json.JSONObject row = response.optJSONObject(i);
                    if (row != null) all.add(TimeSlotMapper.fromJson(row));
                }
                runOnUiThread(() -> {
                    if (isFinishing() || !requestedDate.equals(selectedDate)) return;
                    List<TimeSlot> display = new ArrayList<>();
                    int availCount = 0;
                    for (TimeSlot s : all) {
                        if (s.isAvailable()) availCount++;
                        if (!filterOnlyAvailable || s.isAvailable()) display.add(s);
                    }
                    dateAdapter.setSlotCount(requestedDate, availCount);
                    slotAdapter.updateData(display);
                    ((TextView) findViewById(R.id.tvNoSlots)).setText("Ngày này không còn khung giờ có thể đặt.");
                    android.widget.Button retry = findViewById(R.id.btnSuggestNextAvailable);
                    retry.setText("Chọn ngày khác");
                    retry.setVisibility(View.VISIBLE);
                    retry.setOnClickListener(v -> suggestAvailableDate());
                    boolean hasAvail = (availCount > 0);
                    layoutNoSlots.setVisibility(display.isEmpty() ? View.VISIBLE : View.GONE);
                    rvSlots.setVisibility(display.isEmpty() ? View.GONE : View.VISIBLE);
                    if (tvAvailableTitle != null) tvAvailableTitle.setText("Khung giờ còn trống (" + availCount + ")");
                    selectedSlot = null;
                    for (TimeSlot s : display) {
                        if (s.isAvailable()) { selectedSlot = s; slotAdapter.setSelectedSlotId(s.getId()); break; }
                    }
                    updateBottomSummary();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (isFinishing() || !requestedDate.equals(selectedDate)) return;
                    layoutNoSlots.setVisibility(View.VISIBLE);
                    rvSlots.setVisibility(View.GONE);
                    if (tvAvailableTitle != null) tvAvailableTitle.setText("Khung giờ chưa tải được");
                    ((TextView) findViewById(R.id.tvNoSlots)).setText("Không tải được lịch sân. Hãy thử lại.");
                    android.widget.Button retry = findViewById(R.id.btnSuggestNextAvailable);
                    retry.setVisibility(View.VISIBLE); retry.setText("Thử lại");
                    retry.setOnClickListener(v -> loadSlotsForDate());
                    selectedSlot = null;
                    updateBottomSummary();
                    Toast.makeText(this, "Không tải được khung giờ: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void suggestAvailableDate() {
        final String current = selectedDate;
        slotExecutor.execute(() -> {
            try {
                for (String day : DateTimeUtils.getBookableDates()) {
                    if (day.compareTo(current) <= 0) continue;
                    JSONArray rows = ApiClient.getArray(this, "/v1/pitches/" + pitch.getId() + "/slots?date=" + day);
                    for (int i = 0; i < rows.length(); i++) {
                        if ("available".equals(rows.getJSONObject(i).optString("status"))) {
                            runOnUiThread(() -> { if (!isFinishing()) { dateAdapter.setSelectedDate(day); selectDate(day); } });
                            return;
                        }
                    }
                }
                runOnUiThread(() -> Toast.makeText(this, "Không còn ngày trống trong 7 ngày tới.", Toast.LENGTH_LONG).show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this, "Không tải được lịch sân. Hãy thử lại.", Toast.LENGTH_LONG).show());
            }
        });
    }
    private void updateBottomSummary() {
        boolean valid = (selectedSlot != null && selectedSlot.isAvailable());
        tvSummary.setText(valid ? "1 khung giờ · 1,5 giờ" : "Vui lòng chọn khung giờ");
        tvPrice.setText(valid ? CurrencyUtils.formatVnd(selectedSlot.getPrice()) : "0 đ");
        btnProceed.setEnabled(valid);
    }

}
