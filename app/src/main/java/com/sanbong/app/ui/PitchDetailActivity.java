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
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.TimeSlot;
import com.sanbong.app.ui.adapter.DateChipAdapter;
import com.sanbong.app.ui.adapter.TimeSlotAdapter;
import com.sanbong.app.util.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class PitchDetailActivity extends AppCompatActivity {
    private Pitch pitch;
    private String selectedDate;
    private TimeSlot selectedSlot;
    private boolean filterOnlyAvailable = true;
    private TextView tvSummary, tvPrice, tvAvailableTitle, btnToggleFilter;
    private MaterialButton btnProceed; private View layoutNoSlots;
    private RecyclerView rvDates, rvSlots; private DateChipAdapter dateAdapter; private TimeSlotAdapter slotAdapter;
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
            boolean fav = BookingRepository.getInstance().toggleFavorite(pitch.getId());
            updateFavoriteIcon();
            Toast.makeText(this, fav ? "Đã lưu yêu thích" : "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
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
            boolean fav = BookingRepository.getInstance().isFavorite(pitch.getId());
            btnFav.setImageResource(fav ? R.drawable.ic_ri_heart_fill : R.drawable.ic_ri_heart);
        }
    }
    private void bindPitchInfo() {
        ImageView iv = findViewById(R.id.ivDetailBanner);
        if (iv != null && pitch.getImageResId() != 0) iv.setImageResource(pitch.getImageResId());
        ((TextView) findViewById(R.id.tvDetailName)).setText(pitch.getName());
        ((TextView) findViewById(R.id.tvDetailAddress)).setText(pitch.getAddress());
        ((TextView) findViewById(R.id.tvDetailType)).setText(pitch.getType().getDisplayName());
        ((TextView) findViewById(R.id.tvDetailDescription)).setText(pitch.getDescription());
    }
    private void setupDateList() {
        List<String> nextDays = DateTimeUtils.getNextDays(10);
        selectedDate = BookingRepository.getInstance().getFirstAvailableDate(pitch.getId(), nextDays.get(0));
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
        loadSlotsForDate();
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
            btnCal.setOnClickListener(v -> ThemePickerUtils.showDatePicker(this, Calendar.getInstance(), (vw, y, m, d) -> {
                Calendar c = Calendar.getInstance(); c.set(y, m, d);
                String picked = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(c.getTime());
                if (BookingRepository.getInstance().getAvailableSlotsCount(pitch.getId(), picked) == 0) {
                    Toast.makeText(this, "Ngày này đã kín sân! Chuyển sang ngày trống gần nhất", Toast.LENGTH_SHORT).show();
                    picked = BookingRepository.getInstance().getFirstAvailableDate(pitch.getId(), picked);
                }
                dateAdapter.setSelectedDate(picked);
                selectDate(picked);
            }));
        }
        View btnSug = findViewById(R.id.btnSuggestNextAvailable);
        if (btnSug != null) btnSug.setOnClickListener(v -> {
            String next = BookingRepository.getInstance().getFirstAvailableDate(pitch.getId(), selectedDate);
            dateAdapter.setSelectedDate(next);
            selectDate(next);
        });
    }
    private void loadSlotsForDate() {
        List<TimeSlot> all = BookingRepository.getInstance().getSlots(pitch.getId(), selectedDate);
        List<TimeSlot> display = new ArrayList<>();
        int availCount = 0;
        for (TimeSlot s : all) {
            if (s.isAvailable()) availCount++;
            if (!filterOnlyAvailable || s.isAvailable()) display.add(s);
        }
        slotAdapter.updateData(display);
        boolean hasAvail = (availCount > 0);
        layoutNoSlots.setVisibility(hasAvail ? View.GONE : View.VISIBLE);
        rvSlots.setVisibility(hasAvail ? View.VISIBLE : View.GONE);
        if (tvAvailableTitle != null) tvAvailableTitle.setText("Khung giờ còn trống (" + availCount + ")");
        for (TimeSlot s : display) {
            if (s.isAvailable()) { selectedSlot = s; slotAdapter.setSelectedSlotId(s.getId()); break; }
        }
        updateBottomSummary();
    }
    private void updateBottomSummary() {
        boolean valid = (selectedSlot != null && selectedSlot.isAvailable());
        tvSummary.setText(valid ? "1 khung giờ · 1,5 giờ" : "Vui lòng chọn khung giờ");
        tvPrice.setText(valid ? CurrencyUtils.formatVnd(selectedSlot.getPrice()) : "0 đ");
        btnProceed.setEnabled(valid);
    }
}
