package com.mycompany.oop.model;

import java.time.LocalDate;

public class AttendanceAwareness {

    public enum Severity {
        NORMAL,
        INFO,
        WARNING,
        CRITICAL
    }

    private final int employeeId;
    private final LocalDate date;
    private final String status;
    private final Severity severity;
    private final String message;
    private final String timeIn;
    private final String timeOut;
    private final String shiftLabel;

    public AttendanceAwareness(int employeeId, LocalDate date, String status, Severity severity,
            String message, String timeIn, String timeOut, String shiftLabel) {
        this.employeeId = employeeId;
        this.date = date;
        this.status = status;
        this.severity = severity;
        this.message = message;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.shiftLabel = shiftLabel;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeIn() {
        return timeIn;
    }

    public String getTimeOut() {
        return timeOut;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public boolean requiresReview() {
        return severity == Severity.WARNING || severity == Severity.CRITICAL;
    }
}
