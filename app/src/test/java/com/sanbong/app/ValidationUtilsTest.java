package com.sanbong.app;

import com.sanbong.app.util.ValidationUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Kiểm thử tính đúng đắn của logic validation họ tên và số điện thoại
 */
public class ValidationUtilsTest {

    @Test
    public void testValidPhoneNumbers() {
        assertTrue(ValidationUtils.isValidPhoneNumber("0912345678"));
        assertTrue(ValidationUtils.isValidPhoneNumber("0388999888"));
        assertTrue(ValidationUtils.isValidPhoneNumber("0771234567"));
        assertTrue(ValidationUtils.isValidPhoneNumber("0865554444"));
        assertTrue(ValidationUtils.isValidPhoneNumber("0589998888"));
    }

    @Test
    public void testInvalidPhoneNumbers() {
        assertFalse(ValidationUtils.isValidPhoneNumber(null));
        assertFalse(ValidationUtils.isValidPhoneNumber(""));
        assertFalse(ValidationUtils.isValidPhoneNumber("12345"));
        assertFalse(ValidationUtils.isValidPhoneNumber("0123456789")); // Đầu số cũ 11 số / sai đầu số
        assertFalse(ValidationUtils.isValidPhoneNumber("091234567a")); // Có chữ cái
        assertFalse(ValidationUtils.isValidPhoneNumber("09123456789")); // 11 số
    }

    @Test
    public void testValidNames() {
        assertTrue(ValidationUtils.isValidName("Nguyễn Văn A"));
        assertTrue(ValidationUtils.isValidName("Trần Bình"));
    }

    @Test
    public void testInvalidNames() {
        assertFalse(ValidationUtils.isValidName(null));
        assertFalse(ValidationUtils.isValidName(""));
        assertFalse(ValidationUtils.isValidName("   "));
        assertFalse(ValidationUtils.isValidName("A")); // Quá ngắn
    }
}
