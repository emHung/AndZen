package com.sanbong.app.model;

/**
 * Phân loại kích thước sân bóng đá
 */
public enum PitchType {
    ALL("Tất cả"),
    FIVE("Sân 5 người"),
    SEVEN("Sân 7 người"),
    ELEVEN("Sân 11 người");

    private final String displayName;

    PitchType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
