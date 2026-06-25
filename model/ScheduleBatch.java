package com.mycompany.oop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Compatibility model for schedule_windows schedule coverage records.
public class ScheduleBatch {

    private int scheduleBatchId;
    private Integer departmentId;
    private int uploadedBy;
    private LocalDate scheduleMonth;
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime finalizedAt;
    private Integer finalizedBy;

    public ScheduleBatch() {
    }

    public ScheduleBatch(int scheduleBatchId, Integer departmentId, int uploadedBy,
            LocalDate scheduleMonth, String status, LocalDateTime uploadedAt,
            LocalDateTime finalizedAt, Integer finalizedBy) {
        this.scheduleBatchId = scheduleBatchId;
        this.departmentId = departmentId;
        this.uploadedBy = uploadedBy;
        this.scheduleMonth = scheduleMonth;
        this.status = status;
        this.uploadedAt = uploadedAt;
        this.finalizedAt = finalizedAt;
        this.finalizedBy = finalizedBy;
    }

    public int getScheduleBatchId() {
        return scheduleBatchId;
    }

    public void setScheduleBatchId(int scheduleBatchId) {
        this.scheduleBatchId = scheduleBatchId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public int getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(int uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDate getScheduleMonth() {
        return scheduleMonth;
    }

    public void setScheduleMonth(LocalDate scheduleMonth) {
        this.scheduleMonth = scheduleMonth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public Integer getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(Integer finalizedBy) {
        this.finalizedBy = finalizedBy;
    }
}
