package com.sanbong.app.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tiện ích xử lý ngày và thứ tiếng Việt
 */
public final class DateTimeUtils {

    private static final TimeZone BUSINESS_TIMEZONE = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private static SimpleDateFormat formatter(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setTimeZone(BUSINESS_TIMEZONE);
        format.setLenient(false);
        return format;
    }

    private DateTimeUtils() {
        // Tránh khởi tạo instance
    }

    /**
     * Tạo danh sách ngày dạng yyyy-MM-dd bắt đầu từ hôm nay cho N ngày tới
     */
    public static List<String> getNextDays(int numberOfDays) {
        List<String> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance(BUSINESS_TIMEZONE);
        for (int i = 0; i < numberOfDays; i++) {
            days.add(formatter("yyyy-MM-dd").format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return days;
    }

    /** Hôm nay và tối đa 7 ngày tiếp theo được nhận đặt sân. */
    public static List<String> getBookableDates() {
        return getNextDays(8);
    }

    public static Calendar getLatestBookableDay() {
        Calendar latest = Calendar.getInstance(BUSINESS_TIMEZONE);
        latest.add(Calendar.DAY_OF_YEAR, 7);
        return latest;
    }

    /**
     * Định dạng ngày hiển thị (VD: "Hôm nay", "Ngày mai", "T5, 16/10")
     */
    public static String formatDisplayDate(String isoDate) {
        try {
            Date date = formatter("yyyy-MM-dd").parse(isoDate);
            if (date == null) return isoDate;

            Calendar today = Calendar.getInstance(BUSINESS_TIMEZONE);
            Calendar target = Calendar.getInstance(BUSINESS_TIMEZONE);
            target.setTime(date);

            if (isSameDay(today, target)) {
                return "Hôm nay, " + formatter("dd/MM").format(date);
            }

            today.add(Calendar.DAY_OF_YEAR, 1);
            if (isSameDay(today, target)) {
                return "Ngày mai, " + formatter("dd/MM").format(date);
            }

            int dayOfWeek = target.get(Calendar.DAY_OF_WEEK);
            String dayName = getVietnameseDayOfWeek(dayOfWeek);
            return dayName + ", " + formatter("dd/MM").format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }

    public static boolean hasBookingStarted(String date, String range) {
        return bookingTimeReached(date, range, 0, System.currentTimeMillis());
    }

    public static String durationLabel(String range) {
        if (range == null || !range.matches("\\d{2}:\\d{2}\\s*[-–]\\s*\\d{2}:\\d{2}")) return "—";
        String[] times = range.split("\\s*[-–]\\s*");
        String[] start = times[0].split(":"), end = times[1].split(":");
        int startHour = Integer.parseInt(start[0]), startMinute = Integer.parseInt(start[1]);
        int endHour = Integer.parseInt(end[0]), endMinute = Integer.parseInt(end[1]);
        int minutes = endHour * 60 + endMinute - startHour * 60 - startMinute;
        if (startHour > 23 || endHour > 23 || startMinute > 59 || endMinute > 59 || minutes <= 0) return "—";
        return minutes % 60 == 0 ? (minutes / 60) + " giờ"
                : minutes < 60 ? minutes + " phút" : (minutes / 60) + " giờ " + (minutes % 60) + " phút";
    }

    public static boolean hasBookingEnded(String date, String range) {
        return bookingTimeReached(date, range, 1, System.currentTimeMillis());
    }

    public static boolean bookingTimeReached(String date, String range, int part, long nowMillis) {
        try {
            String[] times = range.split("\\s*[-–]\\s*");
            if (times.length != 2 || part < 0 || part > 1) return false;
            Date moment = formatter("yyyy-MM-dd HH:mm").parse(date + " " + times[part].trim());
            return moment != null && moment.getTime() <= nowMillis;
        } catch (Exception invalid) {
            return false;
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
