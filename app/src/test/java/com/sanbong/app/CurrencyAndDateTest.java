package com.sanbong.app;

import com.sanbong.app.util.CurrencyUtils;
import com.sanbong.app.util.DateTimeUtils;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Kiểm thử tính đúng đắn của định dạng tiền tệ VND và sinh chuỗi ngày chơi
 */
public class CurrencyAndDateTest {

    @Test
    public void testCurrencyFormatting() {
        assertEquals("350.000 đ", CurrencyUtils.formatVnd(350000L));
        assertEquals("0 đ", CurrencyUtils.formatVnd(0L));
        assertEquals("1.200.000 đ", CurrencyUtils.formatVnd(1200000L));
    }

    @Test
    public void testDateGeneration() {
        List<String> days = DateTimeUtils.getNextDays(8);
        assertNotNull(days);
        assertEquals(8, days.size()); // Hôm nay + 7 ngày tiếp theo = 8 ngày

        // Ngày đầu tiên có định dạng yyyy-MM-dd
        assertTrue(days.get(0).matches("\\d{4}-\\d{2}-\\d{2}"));

        // Định dạng hiển thị ngày
        String todayDisplay = DateTimeUtils.formatDisplayDate(days.get(0));
        assertTrue(todayDisplay.startsWith("Hôm nay"));
    }
}
