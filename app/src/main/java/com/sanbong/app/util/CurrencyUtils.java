package com.sanbong.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ Việt Nam Đồng (VND)
 */
public final class CurrencyUtils {

    private CurrencyUtils() {
        // Tránh khởi tạo instance
    }

    public static String formatVnd(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " đ";
    }
}
