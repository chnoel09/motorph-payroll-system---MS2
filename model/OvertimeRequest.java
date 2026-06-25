package com.mycompany.oop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Prepared for the future normalized overtime_requests workflow migration.
public class OvertimeRequest {

    private int overtimeId;
    private int employeeId;
    private LocalDate overtimeDate;
    private double overtimeHours;
    private String reason;
    private String status;
    private Integer approvedBy;
    private String approvedRole;
    private LocalDateTime approvedAt;
    private String remarks;

    public OvertimeRequest() {
    }

    public OvertimeRequest(int overtimeId, int employeeId, LocalDate overtimeDate,
            double overtimeHours, String reason, String status, Integer approvedBy,
            String approvedRole, LocalDateTime approvedAt, String remarks) {
        this.overtimeId = overtimeId;
        this.employeeId = employeeId;
        this.overtimeDate = overtimeDate;
        this.overtimeHours = overtimeHours;
        this.reason = reason;
        this.status = status;
        this.approvedBy = approvedBy;
        this.approvedRole = approvedRole;
        this.approvedAt = approvedAt;
        this.remarks = remarks;
    }

    public int getOvertimeId() {
        return overtimeId;
    }

    public void setOvertimeId(int overtimeId) {
        this.overtimeId = overtimeId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getOvertimeDate() {
        return overtimeDate;
    }

    public void setOvertimeDate(LocalDate overtimeDate) {
        this.overtimeDate = overtimeDate;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedRole() {
        return approvedRole;
    }

    public void setApprovedRole(String approvedRole) {
        this.approvedRole = approvedRole;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
