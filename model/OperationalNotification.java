package com.mycompany.oop.model;

import java.time.LocalDateTime;

public class OperationalNotification {

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL,
        SUCCESS
    }

    public enum Priority {
        CRITICAL,
        ACTION_REQUIRED,
        REVIEW,
        INFORMATIONAL
    }

    private final String title;
    private final String message;
    private final String category;
    private final Severity severity;
    private final Priority priority;
    private final LocalDateTime timestamp;
    private final String status;

    public OperationalNotification(String title, String message, String category,
            Severity severity, LocalDateTime timestamp, String status) {
        this(title, message, category, severity, defaultPriority(severity), timestamp, status);
    }

    public OperationalNotification(String title, String message, String category,
            Severity severity, Priority priority, LocalDateTime timestamp, String status) {
        this.title = title;
        this.message = message;
        this.category = category;
        this.severity = severity;
        this.priority = priority;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    private static Priority defaultPriority(Severity severity) {
        if (severity == null) {
            return Priority.INFORMATIONAL;
        }

        return switch (severity) {
            case CRITICAL -> Priority.CRITICAL;
            case WARNING -> Priority.ACTION_REQUIRED;
            case INFO -> Priority.REVIEW;
            case SUCCESS -> Priority.INFORMATIONAL;
        };
    }
}
