package com.mycompany.oop.model;

public class PayrollLifecycleStatus {

    public enum Status {
        DRAFT,
        OPEN_WORKFORCE_REVIEW,
        READY_FOR_HR_VALIDATION,
        READY_FOR_FINANCE_VALIDATION,
        READY_FOR_PROCESSING,
        PROCESSING,
        PROCESSED,
        LOCKED
    }

    private final String cutoffPeriod;
    private final Status status;
    private final int recordCount;
    private final String periodStart;
    private final String periodEnd;
    private final String processedAt;

    public PayrollLifecycleStatus(String cutoffPeriod, Status status, int recordCount,
            String periodStart, String periodEnd, String processedAt) {
        this.cutoffPeriod = cutoffPeriod;
        this.status = status;
        this.recordCount = recordCount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.processedAt = processedAt;
    }

    public String getCutoffPeriod() {
        return cutoffPeriod;
    }

    public Status getStatus() {
        return status;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public String getProcessedAt() {
        return processedAt;
    }

    public boolean isProcessed() {
        return status == Status.PROCESSED || status == Status.LOCKED;
    }
}
