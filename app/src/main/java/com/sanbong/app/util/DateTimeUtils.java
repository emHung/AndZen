package com.sanbong.app.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tiện ích xử lý ngày và thứ tiếng Việt
 */
public final class DateTimeUtils {

    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd/MM", Locale.getDefault());

    private DateTimeUtils() {
        // Tránh khởi tạo instance
    }

    /**
     * Tạo danh sách ngày dạng yyyy-MM-dd bắt đầu từ hôm nay cho N ngày tới
     */
    public static List<String> getNextDays(int numberOfDays) {
        List<String> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < numberOfDays; i++) {
            days.add(ISO_FORMAT.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return days;
    }

    /**
     * Định dạng ngày hiển thị (VD: "Hôm nay", "Ngày mai", "T5, 16/10")
     */
    public static String formatDisplayDate(String isoDate) {
        try {
            Date date = ISO_FORMAT.parse(isoDate);
            if (date == null) return isoDate;

            Calendar today = Calendar.getInstance();
            Calendar target = Calendar.getInstance();
            target.setTime(date);

            if (isSameDay(today, target)) {
                return "Hôm nay, " + DISPLAY_FORMAT.format(date);
            }

            today.add(Calendar.DAY_OF_YEAR, 1);
            if (isSameDay(today, target)) {
                return "Ngày mai, " + DISPLAY_FORMAT.format(date);
            }

            int dayOfWeek = target.get(Calendar.DAY_OF_WEEK);
            String dayName = getVietnameseDayOfWeek(dayOfWeek);
            return dayName + ", " + DISPLAY_FORMAT.format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static String getVietnameseDayOfWeek(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY: return "Thứ 2";
            case Calendar.TUESDAY: return "Thứ 3";
            case Calendar.WEDNESDAY: return "Thứ 4";
            case Calendar.THURSDAY: return "Thứ 5";
            case Calendar.FRIDAY: return "Thứ 6";
            case Calendar.SATURDAY: return "Thứ 7";
            default: return "CN";
        }
    }
}
