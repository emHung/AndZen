package com.sanbong.app.model;

/**
 * Trạng thái của một khung giờ đặt sân
 */
public enum SlotStatus {
    AVAILABLE("Còn trống"),
    BOOKED("Đã đặt"),
    UNAVAILABLE("Không khả dụng");

    private final String label;

    SlotStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
