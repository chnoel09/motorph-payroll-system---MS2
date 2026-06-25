package com.mycompany.oop.model;

import java.time.LocalDateTime;

public class PersistentNotification {

    private int notificationId;
    private int eventId;
    private Integer targetEmployeeId;
    private String targetRole;
    private String title;
    private String message;
    private String category;
    private String severity;
    private String priority;
    private String notificationStatus;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;

    public PersistentNotification() {
    }

    public PersistentNotification(int notificationId, int eventId, Integer targetEmployeeId,
            String targetRole, String title, String message, String category, String severity,
            String priority, String notificationStatus, LocalDateTime readAt,
            LocalDateTime acknowledgedAt, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.eventId = eventId;
        this.targetEmployeeId = targetEmployeeId;
        this.targetRole = targetRole;
        this.title = title;
        this.message = message;
        this.category = category;
        this.severity = severity;
        this.priority = priority;
        this.notificationStatus = notificationStatus;
        this.readAt = readAt;
        this.acknowledgedAt = acknowledgedAt;
        this.createdAt = createdAt;
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public Integer getTargetEmployeeId() { return targetEmployeeId; }
    public void setTargetEmployeeId(Integer targetEmployeeId) { this.targetEmployeeId = targetEmployeeId; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getNotificationStatus() { return notificationStatus; }
    public void setNotificationStatus(String notificationStatus) { this.notificationStatus = notificationStatus; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
