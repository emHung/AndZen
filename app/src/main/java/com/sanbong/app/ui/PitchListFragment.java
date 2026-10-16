package com.sanbong.app.ui;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.PitchMapper;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.model.PitchType;
import com.sanbong.app.ui.adapter.PitchAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/**
 * Màn hình danh sách sân bóng kèm tìm kiếm và bộ lọc loại sân
 */
public class PitchListFragment extends Fragment implements PitchAdapter.OnPitchClickListener {
    private final ExecutorService catalogExecutor = Executors.newSingleThreadExecutor();
    private final java.util.Set<String> availablePitchIds = new java.util.HashSet<>();
    private boolean availabilityLoaded;

    private static final int REQUEST_FILTER = 1001;

    private EditText etSearchPitch;
    private ChipGroup chipGroupPitchType;
    private RecyclerView rvPitches;
    private TextView tvEmptyPitches;
    private PitchAdapter pitchAdapter;

    private List<Pitch> allPitches = new ArrayList<>();
    private PitchType selectedType = PitchType.ALL;
    private String currentKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pitch_list, container, false);
        initViews(view);
        setupRecyclerView();
        setupSearchListener();
        setupFilterChips();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pitchAdapter != null) loadData();
        if (hasAvailabilityFilter()) loadAvailability();
    }

    private void initViews(View view) {
        etSearchPitch = view.findViewById(R.id.etSearchPitch);
        chipGroupPitchType = view.findViewById(R.id.chipGroupPitchType);
        rvPitches = view.findViewById(R.id.rvPitches);
        tvEmptyPitches = view.findViewById(R.id.tvEmptyPitches);

        View btnNotification = view.findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v ->
                    startActivity(new Intent(getContext(), NotificationActivity.class)));
        }
        View btnFilter = view.findViewById(R.id.btnOpenFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v ->
                    startActivityForResult(new Intent(getContext(), PitchFilterActivity.class), REQUEST_FILTER));
        }
    }

    private void setupRecyclerView() {
        pitchAdapter = new PitchAdapter(this);
        rvPitches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPitches.setAdapter(pitchAdapter);
    }

    private void setupSearchListener() {
        etSearchPitch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {
                currentKeyword = s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        chipGroupPitchType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFive) selectedType = PitchType.FIVE;
            else if (id == R.id.chipSeven) selectedType = PitchType.SEVEN;
            else if (id == R.id.chipEleven) selectedType = PitchType.ELEVEN;
            else selectedType = PitchType.ALL;
            applyFilters();
        });
    }

    private void loadData() {
        catalogExecutor.execute(() -> {
            try {
                JSONArray rows = ApiClient.getPublicArray("/v1/pitches");
                List<Pitch> loaded = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) if (rows.optJSONObject(i) != null) loaded.add(PitchMapper.fromJson(rows.getJSONObject(i)));
                java.util.Set<String> favorites = new java.util.HashSet<>();
                if (com.sanbong.app.data.AuthSession.hasSession(requireContext())) {
                    JSONArray favoriteRows = ApiClient.getArray(requireContext(), "/v1/favorites");
                    for (int i = 0; i < favoriteRows.length(); i++) favorites.add(favoriteRows.optString(i));
                }
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> { allPitches = loaded; pitchAdapter.setFavoriteIds(favorites); applyFilters(); });
            } catch (Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> android.widget.Toast.makeText(getContext(), "Không tải được danh sách sân: " + exception.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FILTER || resultCode != Activity.RESULT_OK || data == null) return;
        int typeIndex = data.getIntExtra(PitchFilterActivity.EXTRA_TYPE, 0);
        selectedType = typeIndex >= 0 && typeIndex < PitchType.values().length
                ? PitchType.values()[typeIndex] : PitchType.ALL;
        currentDistrict = data.getStringExtra(PitchFilterActivity.EXTRA_DISTRICT);
        filterDate = data.getStringExtra(PitchFilterActivity.EXTRA_DATE);
        maxPrice = data.getLongExtra(PitchFilterActivity.EXTRA_MAX_PRICE, Long.MAX_VALUE);
        selectedTimePeriod = data.getStringExtra(PitchFilterActivity.EXTRA_TIME_PERIOD);
        requiredAmenities = data.getStringArrayListExtra(PitchFilterActivity.EXTRA_AMENITIES);
        if (requiredAmenities == null) requiredAmenities = new ArrayList<>();
        availabilityLoaded = !hasAvailabilityFilter();
        if (hasAvailabilityFilter()) loadAvailability();
        applyFilters();
    }

    private String currentDistrict;
    private String filterDate;
    private long maxPrice = Long.MAX_VALUE;
    private String selectedTimePeriod;
    private List<String> requiredAmenities = new ArrayList<>();

    private boolean hasAvailabilityFilter() {
        return filterDate != null || (selectedTimePeriod != null && !"ALL".equals(selectedTimePeriod));
    }

    private void loadAvailability() {
        String date = filterDate;
        String period = selectedTimePeriod;
        android.content.Context appContext = requireContext().getApplicationContext();
        catalogExecutor.execute(() -> {
            java.util.Set<String> matching = new java.util.HashSet<>();
            try {
                String path = "/v1/availability?";
                if (date != null) path += "date=" + date + "&";
                if (period != null && !"ALL".equals(period)) path += "time_range=" + android.net.Uri.encode(period);
                JSONArray rows = ApiClient.getArray(appContext, path);
                for (int i = 0; i < rows.length(); i++) matching.add(rows.optString(i));
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded() || !java.util.Objects.equals(date, filterDate) || !java.util.Objects.equals(period, selectedTimePeriod)) return;
                    availablePitchIds.clear(); availablePitchIds.addAll(matching); availabilityLoaded = true; applyFilters();
                });
            } catch (Exception exception) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> android.widget.Toast.makeText(getContext(), "Không tải được tình trạng khung giờ: " + exception.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }

    private void applyFilters() {
        List<Pitch> filtered = new ArrayList<>();
        for (Pitch p : allPitches) {
            boolean matchesType = selectedType == PitchType.ALL || p.getType() == selectedType;
            String keyword = currentKeyword.toLowerCase(Locale.ROOT);
            boolean matchesKeyword = keyword.isEmpty() ||
                    p.getName().toLowerCase(Locale.ROOT).contains(keyword) ||
                    p.getAddress().toLowerCase(Locale.ROOT).contains(keyword) ||
                    p.getDistrict().toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesDistrict = currentDistrict == null || currentDistrict.equals("Tất cả khu vực") ||
                    p.getDistrict().equalsIgnoreCase(currentDistrict.replace(", Hà Nội", "").trim());
            boolean matchesPrice = p.getMinPrice() <= maxPrice;
            boolean matchesAmenities = p.getAmenities().containsAll(requiredAmenities);
            boolean matchesAvailability = !hasAvailabilityFilter() || (availabilityLoaded && availablePitchIds.contains(p.getId()));

            if (matchesType && matchesKeyword && matchesDistrict && matchesPrice && matchesAmenities && matchesAvailability) {
                filtered.add(p);
            }
        }
        pitchAdapter.updateData(filtered);
        tvEmptyPitches.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPitchClick(Pitch pitch) {
        Intent intent = new Intent(getContext(), PitchDetailActivity.class);
        intent.putExtra("pitch", pitch);
        startActivity(intent);
    }

    @Override public void onDestroy() {
        catalogExecutor.shutdownNow();
        super.onDestroy();
    }
}
