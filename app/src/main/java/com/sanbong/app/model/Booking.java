package com.sanbong.app.model;

import java.io.Serializable;

/**
 * Đơn đặt sân bóng của người dùng
 */
public class Booking implements Serializable {
    private String id;
    private final String bookingCode;
    private final String pitchId;
    private final String pitchName;
    private final String pitchAddress;
    private String slotId;
    private String date;
    private String timeRange;
    private final long totalPrice;
    private final String customerName;
    private final String customerPhone;
    private BookingStatus status;
    private String paymentMethod;
    private String paymentStatus;
    private Pitch pitch;

    public Pitch getPitch() { return pitch; }
    public void setPitch(Pitch pitch) { this.pitch = pitch; }

    public Booking(String bookingCode, String pitchId, String pitchName, 
                   String pitchAddress, String slotId, String date, 
                   String timeRange, long totalPrice, 
                   String customerName, String customerPhone) {
        this.bookingCode = bookingCode;
        this.id = bookingCode;
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
        this.paymentMethod = "cash";
        this.paymentStatus = "cash_due";
    }

    public Booking(String id, String bookingCode, String pitchId, String pitchName,
                   String pitchAddress, String slotId, String date, String timeRange,
                   long totalPrice, String customerName, String customerPhone,
                   String paymentMethod, String paymentStatus, BookingStatus status) {
        this(bookingCode, pitchId, pitchName, pitchAddress, slotId, date, timeRange,
                totalPrice, customerName, customerPhone);
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.status = status;
    }

    public String getId() { return id; }
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
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }

    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public void setDate(String date) { this.date = date; }
}
