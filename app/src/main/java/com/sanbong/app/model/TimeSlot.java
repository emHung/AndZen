package com.sanbong.app.model;

import java.io.Serializable;

/**
 * Đối tượng mô tả một khung giờ của sân
 */
public class TimeSlot implements Serializable {
    private final String id;
    private final String pitchId;
    private final String date; // định dạng YYYY-MM-DD
    private final String startTime;
    private final String endTime;
    private final long price;
    private SlotStatus status;

    public TimeSlot(String id, String pitchId, String date, 
                    String startTime, String endTime, long price, SlotStatus status) {
        this.id = id;
        this.pitchId = pitchId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = status;
    }

    public String getId() { return id; }
    public String getPitchId() { return pitchId; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public long getPrice() { return price; }
    public SlotStatus getStatus() { return status; }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    public String getTimeRange() {
        return startTime + " - " + endTime;
    }

    public boolean isAvailable() {
        return status == SlotStatus.AVAILABLE;
    }
}
