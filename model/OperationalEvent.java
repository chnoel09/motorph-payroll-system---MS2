package com.mycompany.oop.model;

import java.time.LocalDateTime;

public class OperationalEvent {

    private int eventId;
    private String eventType;
    private String category;
    private String severity;
    private String priority;
    private String referenceTable;
    private String referenceId;
    private Integer actorEmployeeId;
    private String title;
    private String message;
    private String eventStatus;
    private LocalDateTime createdAt;

    public OperationalEvent() {
    }

    public OperationalEvent(int eventId, String eventType, String category, String severity,
            String priority, String referenceTable, String referenceId, Integer actorEmployeeId,
            String title, String message, String eventStatus, LocalDateTime createdAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.category = category;
        this.severity = severity;
        this.priority = priority;
        this.referenceTable = referenceTable;
        this.referenceId = referenceId;
        this.actorEmployeeId = actorEmployeeId;
        this.title = title;
        this.message = message;
        this.eventStatus = eventStatus;
        this.createdAt = createdAt;
    }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getReferenceTable() { return referenceTable; }
    public void setReferenceTable(String referenceTable) { this.referenceTable = referenceTable; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public Integer getActorEmployeeId() { return actorEmployeeId; }
    public void setActorEmployeeId(Integer actorEmployeeId) { this.actorEmployeeId = actorEmployeeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
