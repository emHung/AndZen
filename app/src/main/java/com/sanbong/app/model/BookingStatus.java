package com.sanbong.app.model;

/**
 * Trạng thái của đơn đặt sân
 */
public enum BookingStatus {
    CONFIRMED("Đã xác nhận"),
    CANCELLED("Đã hủy");

    private final String title;

    BookingStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
