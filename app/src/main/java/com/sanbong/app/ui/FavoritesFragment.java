package com.sanbong.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.PitchMapper;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.ui.adapter.FavoritePitchAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.json.JSONArray;

public class FavoritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private View layoutEmptyFavorites;
    private TextView chipFavAll;
    private FavoritePitchAdapter adapter;
    private List<Pitch> favoriteData = new ArrayList<>();
    private final ExecutorService favoritesExecutor = Executors.newFixedThreadPool(2);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        rvFavorites = view.findViewById(R.id.rvFavoritePitches);
        layoutEmptyFavorites = view.findViewById(R.id.layoutEmptyFavorites);
        chipFavAll = view.findViewById(R.id.chipFavAll);

        view.findViewById(R.id.btnFavBack).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_pitches);
            }
        });
        view.findViewById(R.id.btnFavSearch).setOnClickListener(v ->
                Toast.makeText(getContext(), "Tìm kiếm trong danh sách yêu thích", Toast.LENGTH_SHORT).show());

        View btnExplore = view.findViewById(R.id.btnEmptyExplorePitch);
        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToTab(R.id.nav_pitches);
                }
            });
        }

        setupFilterChips(view);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void setupFilterChips(View view) {
        chipFavAll.setOnClickListener(v -> loadFavorites());
        view.findViewById(R.id.chipFavRecent).setOnClickListener(v -> loadFavorites());
        view.findViewById(R.id.chipFavGoodPrice).setOnClickListener(v -> {
            List<Pitch> list = new ArrayList<>(favoriteData);
            Collections.sort(list, Comparator.comparingLong(Pitch::getMinPrice));
            adapter.updateData(list);
            Toast.makeText(getContext(), "Đã sắp xếp theo giá tốt nhất", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new FavoritePitchAdapter(this::loadFavorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        android.content.Context appContext = requireContext().getApplicationContext();
        CompletableFuture<JSONArray> favoritesFuture = CompletableFuture.supplyAsync(() -> {
            try { return ApiClient.getArray(appContext, "/v1/favorites"); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, favoritesExecutor);
        CompletableFuture<JSONArray> pitchesFuture = CompletableFuture.supplyAsync(() -> {
            try { return ApiClient.getPublicArray("/v1/pitches"); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, favoritesExecutor);
        CompletableFuture.allOf(favoritesFuture, pitchesFuture).whenComplete((ignored, failure) -> {
            android.app.Activity activity = getActivity();
            if (activity == null) return;
            if (failure != null) {
                Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                        ? failure.getCause() : failure;
                activity.runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(getContext(), "Không tải được sân yêu thích: " + cause.getMessage(), Toast.LENGTH_LONG).show();
                });
                return;
            }
            try {
                JSONArray favoriteIds = favoritesFuture.join();
                Set<String> ids = new HashSet<>();
                for (int i = 0; i < favoriteIds.length(); i++) ids.add(favoriteIds.optString(i));
                JSONArray pitchRows = pitchesFuture.join();
                List<Pitch> list = new ArrayList<>();
                for (int i = 0; i < pitchRows.length(); i++) {
                    if (pitchRows.optJSONObject(i) == null) continue;
                    Pitch pitch = PitchMapper.fromJson(pitchRows.getJSONObject(i));
                    if (ids.contains(pitch.getId())) list.add(pitch);
                }
                activity.runOnUiThread(() -> {
                    if (!isAdded() || getView() == null) return;
                    favoriteData = list;
                    if (chipFavAll != null) chipFavAll.setText("Tất cả (" + list.size() + ")");
                    adapter.updateData(list);
                    if (layoutEmptyFavorites != null) layoutEmptyFavorites.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    rvFavorites.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(getContext(), "Không đọc được dữ liệu sân yêu thích: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        favoritesExecutor.shutdownNow();
        super.onDestroy();
    }
}
