package com.mycompany.oop.model;

import java.time.LocalDate;

public class TodayAttendanceSummary {

    private final LocalDate date;
    private final String status;
    private final String message;
    private final String timeIn;
    private final String timeOut;
    private final double hoursWorked;

    public TodayAttendanceSummary(LocalDate date, String status, String message,
            String timeIn, String timeOut, double hoursWorked) {
        this.date = date;
        this.status = safe(status);
        this.message = safe(message);
        this.timeIn = safe(timeIn);
        this.timeOut = safe(timeOut);
        this.hoursWorked = Math.max(0.0, hoursWorked);
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStatus() {
        return status;
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

    public double getHoursWorked() {
        return hoursWorked;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
