package com.sanbong.app.model;

import java.io.Serializable;

/**
 * Đơn đặt sân bóng của người dùng
 */
public class Booking implements Serializable {
    private final String bookingCode;
    private final String pitchId;
    private final String pitchName;
    private final String pitchAddress;
    private String slotId;
    private final String date;
    private String timeRange;
    private final long totalPrice;
    private final String customerName;
    private final String customerPhone;
    private BookingStatus status;

    public Booking(String bookingCode, String pitchId, String pitchName, 
                   String pitchAddress, String slotId, String date, 
                   String timeRange, long totalPrice, 
                   String customerName, String customerPhone) {
        this.bookingCode = bookingCode;
        this.pitchId = pitchId;
        this.pitchName = pitchName;
        this.pitchAddress = pitchAddress;
        this.slotId = slotId;
        this.date = date;
        this.timeRange = timeRange;
        this.totalPrice = totalPrice;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getBookingCode() { return bookingCode; }
    public String getPitchId() { return pitchId; }
    public String getPitchName() { return pitchName; }
    public String getPitchAddress() { return pitchAddress; }
    public String getSlotId() { return slotId; }
    public String getDate() { return date; }
    public String getTimeRange() { return timeRange; }
    public long getTotalPrice() { return totalPrice; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public BookingStatus getStatus() { return status; }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }
}
