package com.sanbong.app.util;

import java.util.Random;

/**
 * Tiện ích sinh mã đặt sân ngẫu nhiên dễ đọc (SB-XXXXXX)
 */
public final class IdGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    private IdGenerator() {
        // Tránh khởi tạo instance
    }

    public static String generateBookingCode() {
        StringBuilder sb = new StringBuilder("SB-");
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
