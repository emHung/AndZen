package com.sanbong.app;

import com.sanbong.app.util.ApiUrlUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ApiUrlUtilsTest {
    @Test
    public void preservesUrlPathAndRemovesOnlyTrailingSlashes() {
        assertEquals("https://example.com/api", ApiUrlUtils.stripTrailingSlashes("https://example.com/api///"));
        assertEquals("http://127.0.0.1:8000", ApiUrlUtils.stripTrailingSlashes("http://127.0.0.1:8000/"));
        assertEquals("https://example.com/api", ApiUrlUtils.stripTrailingSlashes("https://example.com/api"));
        assertEquals("", ApiUrlUtils.stripTrailingSlashes(""));
    }

    @Test(timeout = 1000)
    public void handlesLongSlashSuffixWithoutRegexBacktracking() {
        String base = "https://example.com";
        StringBuilder input = new StringBuilder(base);
        for (int i = 0; i < 100000; i++) input.append('/');
        assertEquals(base, ApiUrlUtils.stripTrailingSlashes(input.toString()));
    }
}
