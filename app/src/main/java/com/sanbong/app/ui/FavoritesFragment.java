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
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.ui.adapter.FavoritePitchAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private View layoutEmptyFavorites;
    private TextView chipFavAll;
    private FavoritePitchAdapter adapter;

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
        loadFavorites();
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
            List<Pitch> list = new ArrayList<>(BookingRepository.getInstance().getFavoritePitches());
            Collections.sort(list, Comparator.comparingLong(Pitch::getMinPrice));
            adapter.updateData(list);
            Toast.makeText(getContext(), "Đã sắp xếp theo giá tốt nhất", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        adapter = new FavoritePitchAdapter(() -> {
            int count = BookingRepository.getInstance().getFavoriteCount();
            if (chipFavAll != null) chipFavAll.setText("Tất cả (" + count + ")");
            if (layoutEmptyFavorites != null) layoutEmptyFavorites.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            rvFavorites.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        });
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        List<Pitch> list = BookingRepository.getInstance().getFavoritePitches();
        if (chipFavAll != null) chipFavAll.setText("Tất cả (" + list.size() + ")");
        adapter.updateData(list);
        if (layoutEmptyFavorites != null) layoutEmptyFavorites.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        rvFavorites.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
