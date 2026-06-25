package com.mycompany.oop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceAdjustment {

    private int adjustmentId;
    private int employeeId;
    private LocalDate attendanceDate;
    private String adjustmentType;
    private String remarks;
    private Integer adjustedBy;
    private LocalDateTime adjustedAt;

    public AttendanceAdjustment() {
    }

    public AttendanceAdjustment(int adjustmentId, int employeeId, LocalDate attendanceDate,
            String adjustmentType, String remarks, Integer adjustedBy, LocalDateTime adjustedAt) {
        this.adjustmentId = adjustmentId;
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.adjustmentType = adjustmentType;
        this.remarks = remarks;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt;
    }

    public int getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(int adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getAdjustedBy() {
        return adjustedBy;
    }

    public void setAdjustedBy(Integer adjustedBy) {
        this.adjustedBy = adjustedBy;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }

    public void setAdjustedAt(LocalDateTime adjustedAt) {
        this.adjustedAt = adjustedAt;
    }

    public boolean isResolved() {
        return adjustedAt != null;
    }

    public String getStatus() {
        return isResolved() ? "CORRECTED" : "PENDING";
    }
}
