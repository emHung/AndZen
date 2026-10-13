package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sanbong.app.R;
import com.sanbong.app.data.BookingRepository;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.NotificationItem;
import com.sanbong.app.model.Pitch;
import com.sanbong.app.ui.adapter.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private final List<NotificationItem> list = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllRead());

        initData();
        setupRecyclerView();
    }

    private void initData() {
        List<Booking> myBookings = BookingRepository.getInstance().getMyBookings();
        if (!myBookings.isEmpty()) {
            Booking b = myBookings.get(0);
            list.add(new NotificationItem("b1", "Đặt sân thành công!",
                    "Lịch đá tại " + b.getPitchName() + " (" + b.getTimeRange() + ") đã xác nhận. Mã: " + b.getBookingCode(),
                    "10 phút trước", R.drawable.ic_ri_football, false, "BOOKING", b.getBookingCode()));
            list.add(new NotificationItem("b2", "Nhắc nhở giờ thi đấu",
                    "Trận đấu tại " + b.getPitchName() + " sắp diễn ra. Vui lòng có mặt trước 15 phút!",
                    "1 giờ trước", R.drawable.ic_ri_time, false, "BOOKING", b.getBookingCode()));
        }

        Pitch p1 = BookingRepository.getInstance().getPitchById("p1");
        if (p1 != null) {
            list.add(new NotificationItem("p1", "Ưu đãi 20% đặt sân cuối tuần",
                    "Nhập mã FOOTBALL20 khi đặt tại " + p1.getName() + " để nhận ưu đãi 20%!",
                    "Hôm qua", R.drawable.ic_ri_coins, true, "PITCH", "p1"));
        }

        Pitch p2 = BookingRepository.getInstance().getPitchById("p2");
        if (p2 != null) {
            list.add(new NotificationItem("p2", "Sân hot: " + p2.getName(),
                    "Khám phá ngay mặt sân cỏ nhân tạo đạt chuẩn tại " + p2.getAddress(),
                    "2 ngày trước", R.drawable.ic_ri_star, true, "PITCH", "p2"));
        }
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(list, item -> {
            item.setRead(true);
            adapter.notifyDataSetChanged();
            if ("BOOKING".equals(item.getTargetType()) && item.getTargetId() != null) {
                Booking b = BookingRepository.getInstance().getBookingByCode(item.getTargetId());
                if (b != null) {
                    Intent intent = new Intent(this, BookingDetailActivity.class);
                    intent.putExtra("booking", b);
                    startActivity(intent);
                }
            } else if ("PITCH".equals(item.getTargetType()) && item.getTargetId() != null) {
                Pitch p = BookingRepository.getInstance().getPitchById(item.getTargetId());
                if (p != null) {
                    Intent intent = new Intent(this, PitchDetailActivity.class);
                    intent.putExtra("pitch", p);
                    startActivity(intent);
                }
            }
        });
        rv.setAdapter(adapter);
    }

    private void markAllRead() {
        for (NotificationItem item : list) {
            item.setRead(true);
        }
        adapter.notifyDataSetChanged();
    }
}
