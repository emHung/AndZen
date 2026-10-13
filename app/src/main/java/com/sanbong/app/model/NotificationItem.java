package com.sanbong.app.model;

public class NotificationItem {
    private final String id;
    private final String title;
    private final String body;
    private final String time;
    private final int iconRes;
    private boolean isRead;
    private final String targetType; // "BOOKING", "PITCH", "GENERAL"
    private final String targetId;   // bookingCode or pitchId

    public NotificationItem(String id, String title, String body, String time, int iconRes, boolean isRead) {
        this(id, title, body, time, iconRes, isRead, "GENERAL", null);
    }

    public NotificationItem(String id, String title, String body, String time, int iconRes, boolean isRead, String targetType, String targetId) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.time = time;
        this.iconRes = iconRes;
        this.isRead = isRead;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getTime() { return time; }
    public int getIconRes() { return iconRes; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
}
