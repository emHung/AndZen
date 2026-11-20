package com.sanbong.app.util;

public final class ApiUrlUtils {
    private ApiUrlUtils() {}

    public static String stripTrailingSlashes(String base) {
        int end = base.length();
        while (end > 0 && base.charAt(end - 1) == '/') {
            end--;
        }
        return base.substring(0, end);
    }
}
