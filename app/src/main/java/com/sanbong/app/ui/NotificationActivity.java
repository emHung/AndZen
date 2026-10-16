package com.sanbong.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sanbong.app.R;
import com.sanbong.app.data.ApiClient;
import com.sanbong.app.data.BookingMapper;
import com.sanbong.app.model.Booking;
import com.sanbong.app.model.NotificationItem;
import com.sanbong.app.ui.adapter.NotificationAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

public class NotificationActivity extends AppCompatActivity {
    private final List<NotificationItem> list = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private NotificationAdapter adapter;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_notifications);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllRead());
        RecyclerView rv = findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(list, this::openNotification);
        rv.setAdapter(adapter);
        loadBookings();
    }

    private void loadBookings() {
        executor.execute(() -> {
            try {
                JSONArray rows = ApiClient.getArray(this, "/v1/bookings");
                List<Booking> loadedBookings = new ArrayList<>();
                List<NotificationItem> loadedItems = new ArrayList<>();
                for (int i = 0; i < rows.length(); i++) {
                    if (rows.optJSONObject(i) == null) continue;
                    Booking booking = BookingMapper.fromJson(rows.getJSONObject(i));
                    loadedBookings.add(booking);
                    boolean updated = !rows.getJSONObject(i).isNull("rescheduled_at");
                    String title = booking.getStatus() == com.sanbong.app.model.BookingStatus.CANCELLED
                            ? "Lịch đặt đã hủy" : updated ? "Lịch đặt đã cập nhật" : "Lịch đặt sân đã xác nhận";
                    String body = (updated ? "Ngày/giờ mới: " : "") + booking.getPitchName() + " · " + booking.getDate()
                            + " · " + booking.getTimeRange() + " · Mã " + booking.getBookingCode();
                    loadedItems.add(new NotificationItem(booking.getBookingCode(), title, body, "Cập nhật từ lịch đặt", R.drawable.ic_ri_football,
                            false, "BOOKING", booking.getBookingCode()));
                }
                runOnUiThread(() -> {
                    bookings.clear(); bookings.addAll(loadedBookings);
                    list.clear(); list.addAll(loadedItems); adapter.notifyDataSetChanged();
                    if (list.isEmpty()) Toast.makeText(this, "Bạn chưa có lịch đặt để thông báo.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> Toast.makeText(this, "Không tải được thông báo: " + exception.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openNotification(NotificationItem item) {
        for (Booking booking : bookings) {
            if (booking.getBookingCode().equals(item.getTargetId())) {
                startActivity(new Intent(this, BookingDetailActivity.class).putExtra("booking", booking));
                return;
            }
        }
    }

    private void markAllRead() {
        for (NotificationItem item : list) item.setRead(true);
        adapter.notifyDataSetChanged();
    }

    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
