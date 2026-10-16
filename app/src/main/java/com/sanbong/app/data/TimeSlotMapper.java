package com.sanbong.app.data;

import com.sanbong.app.model.SlotStatus;
import com.sanbong.app.model.TimeSlot;
import org.json.JSONObject;

public final class TimeSlotMapper {
    private TimeSlotMapper() {}

    public static TimeSlot fromJson(JSONObject value) {
        String statusValue = value.optString("status", "available");
        SlotStatus status = "booked".equalsIgnoreCase(statusValue) ? SlotStatus.BOOKED
                : "unavailable".equalsIgnoreCase(statusValue) ? SlotStatus.UNAVAILABLE : SlotStatus.AVAILABLE;
        String start = shortTime(value.optString("starts_at"));
        String end = shortTime(value.optString("ends_at"));
        return new TimeSlot(
                value.optString("id"), value.optString("pitch_id"), value.optString("slot_date"),
                start, end,
                value.optLong("price"), status
        );
    }

    private static String shortTime(String value) {
        return value != null && value.length() >= 5 ? value.substring(0, 5) : value;
    }
}
