package com.sanbong.app.data;

import com.sanbong.app.model.SlotStatus;
import com.sanbong.app.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích khởi tạo danh sách khung giờ mẫu cho sân và ngày
 */
public final class MockSlotData {

    private static final String[][] TIME_RANGES = {
            {"06:00", "07:30"},
            {"07:30", "09:00"},
            {"15:00", "16:30"},
            {"16:30", "18:00"},
            {"18:00", "19:30"},
            {"19:30", "21:00"},
            {"21:00", "22:30"}
    };

    private MockSlotData() {}

    public static List<String> getTimeRanges() {
        List<String> ranges = new ArrayList<>();
        for (String[] range : TIME_RANGES) {
            ranges.add(range[0] + " - " + range[1]);
        }
        return java.util.Collections.unmodifiableList(ranges);
    }

    public static List<TimeSlot> generateSlots(String pitchId, String date, long basePrice) {
        List<TimeSlot> slots = new ArrayList<>();

        for (int i = 0; i < TIME_RANGES.length; i++) {
            String start = TIME_RANGES[i][0];
            String end = TIME_RANGES[i][1];
            String slotId = pitchId + "_" + date + "_" + i;
            long slotPrice = (i >= 4) ? (basePrice + 50000) : basePrice;

            SlotStatus status;
            // Ngày đặc biệt (16/10) được đặt kín để kiểm thử lọc ngày/khung giờ chưa đặt
            if (date.endsWith("-16")) {
                status = SlotStatus.BOOKED;
            } else {
                int h = Math.abs((pitchId + date).hashCode());
                boolean isBooked = ((h + i) % 3 == 0);
                status = isBooked ? SlotStatus.BOOKED : SlotStatus.AVAILABLE;
            }

            slots.add(new TimeSlot(slotId, pitchId, date, start, end, slotPrice, status));
        }

        return slots;
    }
}
