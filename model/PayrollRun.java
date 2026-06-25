package com.mycompany.oop.model;

import java.time.LocalDateTime;

// Prepared for the future normalized payroll_runs migration.
public class PayrollRun {

    private int runId;
    private int periodId;
    private Integer processedBy;
    private LocalDateTime processedAt;
    private String status;

    public PayrollRun() {
    }

    public PayrollRun(int runId, int periodId, Integer processedBy,
            LocalDateTime processedAt, String status) {
        this.runId = runId;
        this.periodId = periodId;
        this.processedBy = processedBy;
        this.processedAt = processedAt;
        this.status = status;
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    public int getPeriodId() {
        return periodId;
    }

    public void setPeriodId(int periodId) {
        this.periodId = periodId;
    }

    public Integer getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(Integer processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
