package com.sanbong.app.util;

/**
 * Tiện ích kiểm tra tính hợp lệ của dữ liệu biểu mẫu
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Tránh khởi tạo instance
    }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2;
    }

    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null) return false;
        String trimmed = phone.trim();
        // Số điện thoại Việt Nam 10 chữ số, bắt đầu bằng 0
        return trimmed.matches("^0[35789]\\d{8}$");
    }
}
