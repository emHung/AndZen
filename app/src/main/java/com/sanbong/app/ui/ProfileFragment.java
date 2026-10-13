package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.Pitch;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvStatBookingsNum;
    private TextView tvStatFavoritesNum;
    private TextView tvFrequentPitchName;
    private TextView tvFrequentPitchAddress;
    private TextView tvFrequentPitchDate;
    private String frequentPitchId = "p1";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        updateDynamicStats();
        setupHeaderButtons(view);
        setupCardActions(view);
        setupSettingsAndLogout(view);
        return view;
    }

    private void initViews(View view) {
        tvStatBookingsNum = view.findViewById(R.id.tvStatBookingsNum);
        tvStatFavoritesNum = view.findViewById(R.id.tvStatFavoritesNum);
        tvFrequentPitchName = view.findViewById(R.id.tvFrequentPitchName);
        tvFrequentPitchAddress = view.findViewById(R.id.tvFrequentPitchAddress);
        tvFrequentPitchDate = view.findViewById(R.id.tvFrequentPitchDate);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDynamicStats();
    }

    private void updateDynamicStats() {
        BookingRepository repo = BookingRepository.getInstance();
        if (tvStatBookingsNum != null) {
            tvStatBookingsNum.setText(String.valueOf(repo.getMyBookings().size()));
        }
        if (tvStatFavoritesNum != null) {
            tvStatFavoritesNum.setText(String.valueOf(repo.getFavoriteCount()));
        }
        List<Booking> bookings = repo.getMyBookings();
        if (!bookings.isEmpty() && tvFrequentPitchName != null) {
            Booking latest = bookings.get(bookings.size() - 1);
            frequentPitchId = latest.getPitchId();
            tvFrequentPitchName.setText(latest.getPitchName());
            tvFrequentPitchAddress.setText(latest.getPitchAddress());
            tvFrequentPitchDate.setText("Lần gần nhất · " + latest.getDate());
        }
    }

    private void setupHeaderButtons(View view) {
        view.findViewById(R.id.btnProfileNotification).setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificationActivity.class)));
        view.findViewById(R.id.btnProfileSettings).setOnClickListener(v ->
                Toast.makeText(getContext(), "Cài đặt tài khoản & ứng dụng", Toast.LENGTH_SHORT).show());
    }

    private void setupCardActions(View view) {
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class)));
        view.findViewById(R.id.btnChangeAvatar).setOnClickListener(v ->
                startActivity(new Intent(getContext(), EditProfileActivity.class)));

        view.findViewById(R.id.btnStatBookings).setOnClickListener(v -> navigateToTab(R.id.nav_my_bookings));
        view.findViewById(R.id.btnNavMyBookings).setOnClickListener(v -> navigateToTab(R.id.nav_my_bookings));
        view.findViewById(R.id.btnStatFavorites).setOnClickListener(v -> navigateToTab(R.id.nav_favorites));
        view.findViewById(R.id.btnNavFavorites).setOnClickListener(v -> navigateToTab(R.id.nav_favorites));
        view.findViewById(R.id.btnViewAllFavorite).setOnClickListener(v -> navigateToTab(R.id.nav_favorites));

        view.findViewById(R.id.cardFrequentPitch).setOnClickListener(v -> {
            Pitch pitch = BookingRepository.getInstance().getPitchById(frequentPitchId);
            if (pitch != null) {
                Intent intent = new Intent(getContext(), PitchDetailActivity.class);
                intent.putExtra("pitch", pitch);
                startActivity(intent);
            }
        });
    }

    private void setupSettingsAndLogout(View view) {
        view.findViewById(R.id.btnPaymentMethods).setOnClickListener(v ->
                Toast.makeText(getContext(), "Phương thức: Tiền mặt, Chuyển khoản VietQR, Ví MoMo", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnSettingNotif).setOnClickListener(v ->
                startActivity(new Intent(getContext(), NotificationActivity.class)));
        view.findViewById(R.id.btnSettingPrivacy).setOnClickListener(v ->
                Toast.makeText(getContext(), "Chính sách bảo mật thông tin người dùng", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnSettingHelp).setOnClickListener(v ->
                Toast.makeText(getContext(), "Trung tâm hỗ trợ hotline: 1900 6868", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void navigateToTab(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(tabId);
        }
    }
}
