package com.sanbong.app;

import com.sanbong.app.util.DateTimeUtils;
import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class BookingDateUtilsTest {
    @Test public void durationUsesActualDatabaseRange() {
        assertEquals("1 giờ 30 phút", DateTimeUtils.durationLabel("06:00 - 07:30"));
        assertEquals("1 giờ", DateTimeUtils.durationLabel("09:00 - 10:00"));
        assertEquals("45 phút", DateTimeUtils.durationLabel("09:00 - 09:45"));
        assertEquals("—", DateTimeUtils.durationLabel("25:00 - 26:00"));
        assertEquals("—", DateTimeUtils.durationLabel(null));
    }
    private long instant(String utcTime) throws Exception {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        return parser.parse(utcTime).getTime();
    }

    @Test public void bookingCompletionUsesEndTimeInVietnam() throws Exception {
        String day = "2026-12-31";
        String range = "18:00 - 19:30";
        assertFalse(DateTimeUtils.bookingTimeReached(day, range, 0, instant("2026-12-31 10:59")));
        assertTrue(DateTimeUtils.bookingTimeReached(day, range, 0, instant("2026-12-31 11:00")));
        assertFalse(DateTimeUtils.bookingTimeReached(day, range, 1, instant("2026-12-31 12:29")));
        assertTrue(DateTimeUtils.bookingTimeReached(day, range, 1, instant("2026-12-31 12:30")));
    }

    @Test public void invalidDatesCannotBeMarkedCompleted() {
        assertFalse(DateTimeUtils.bookingTimeReached("2026-02-30", "18:00 - 19:30", 1, Long.MAX_VALUE));
        assertFalse(DateTimeUtils.bookingTimeReached("2026-12-31", "", 1, Long.MAX_VALUE));
    }

    @Test public void bookingCalendarContainsTodayAndSevenDaysAhead() {
        assertEquals(8, DateTimeUtils.getBookableDates().size());
        assertEquals(8, new java.util.HashSet<>(DateTimeUtils.getBookableDates()).size());
    }
}
