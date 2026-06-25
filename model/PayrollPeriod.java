package com.mycompany.oop.model;

import java.time.LocalDate;

// Prepared for the future normalized payroll_periods migration.
public class PayrollPeriod {

    private int periodId;
    private String cutoffPeriod;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;

    public PayrollPeriod() {
    }

    public PayrollPeriod(int periodId, String cutoffPeriod, LocalDate periodStart,
            LocalDate periodEnd, String status) {
        this.periodId = periodId;
        this.cutoffPeriod = cutoffPeriod;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
    }

    public int getPeriodId() {
        return periodId;
    }

    public void setPeriodId(int periodId) {
        this.periodId = periodId;
    }

    public String getCutoffPeriod() {
        return cutoffPeriod;
    }

    public void setCutoffPeriod(String cutoffPeriod) {
        this.cutoffPeriod = cutoffPeriod;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
