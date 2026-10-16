package com.sanbong.app.data;

import com.sanbong.app.model.Booking;
import com.sanbong.app.model.BookingStatus;
import org.json.JSONObject;

public final class BookingMapper {
    private BookingMapper() {}

    public static Booking fromJson(JSONObject value) {
        BookingStatus status = "cancelled".equalsIgnoreCase(value.optString("status"))
                ? BookingStatus.CANCELLED : BookingStatus.CONFIRMED;
        Booking booking = new Booking(
                value.optString("id", value.optString("booking_id", value.optString("booking_code"))),
                value.optString("booking_code"),
                value.optString("pitch_id"),
                value.optString("pitch_name"),
                value.optString("pitch_address"),
                value.optString("slot_id"),
                value.optString("booking_date"),
                value.optString("time_range"),
                value.optLong("total_price"),
                value.optString("customer_name"),
                value.optString("customer_phone"),
                value.optString("payment_method", "cash"),
                value.optString("payment_status", "cash_due"),
                status
        );
        JSONObject pitch = value.optJSONObject("pitches");
        if (pitch != null) booking.setPitch(PitchMapper.fromJson(pitch));
        return booking;
    }
}
