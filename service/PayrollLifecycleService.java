package com.mycompany.oop.service;

import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.model.PayrollLifecycleStatus;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollRun;
import com.mycompany.oop.repository.PayrollPeriodDatabaseRepository;
import com.mycompany.oop.repository.PayrollPeriodRepository;

import java.util.List;

// Governance visibility only. This service does not change payroll formulas or persistence schema.
public class PayrollLifecycleService {

    private final PayrollService payrollService;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollRunService payrollRunService;
    private final PayrollPeriodLifecycleService periodLifecycleService;

    public PayrollLifecycleService() {
        this.payrollService = new PayrollService();
        this.payrollPeriodRepository = new PayrollPeriodDatabaseRepository();
        this.payrollRunService = new PayrollRunService();
        this.periodLifecycleService = new PayrollPeriodLifecycleService();
    }

    public PayrollLifecycleStatus getLifecycleStatus(String cutoffPeriod, String periodStart, String periodEnd) {
        PayrollPeriod period = findPayrollPeriod(cutoffPeriod, periodStart, periodEnd);
        if (period != null) {
            PayrollRun run = payrollRunService.getLatestRunForPeriod(period);
            if (run != null) {
                String lifecycleSource = resolveLifecycleSource(period, run);
                return new PayrollLifecycleStatus(
                        cutoffPeriod,
                        toLifecycleStatus(lifecycleSource),
                        payrollRunService.getRunDetailCount(run.getRunId()),
                        safe(period.getPeriodStart() == null ? null : period.getPeriodStart().toString(), periodStart),
                        safe(period.getPeriodEnd() == null ? null : period.getPeriodEnd().toString(), periodEnd),
                        run.getProcessedAt() == null ? "Timestamp unavailable" : run.getProcessedAt().toString()
                );
            }

            return new PayrollLifecycleStatus(
                    cutoffPeriod,
                    toLifecycleStatus(period.getStatus()),
                    0,
                    safe(period.getPeriodStart() == null ? null : period.getPeriodStart().toString(), periodStart),
                    safe(period.getPeriodEnd() == null ? null : period.getPeriodEnd().toString(), periodEnd),
                    "Not processed"
            );
        }

        List<PayrollHistoryRecord> records = payrollService.getPayrollHistoryByCutoff(cutoffPeriod);
        if (records.isEmpty()) {
            return new PayrollLifecycleStatus(
                    cutoffPeriod,
                    PayrollLifecycleStatus.Status.READY_FOR_PROCESSING,
                    0,
                    periodStart,
                    periodEnd,
                    "Not processed"
            );
        }

        PayrollHistoryRecord firstRecord = records.get(0);
        return new PayrollLifecycleStatus(
                cutoffPeriod,
                PayrollLifecycleStatus.Status.PROCESSED,
                records.size(),
                safe(firstRecord.getPeriodStart(), periodStart),
                safe(firstRecord.getPeriodEnd(), periodEnd),
                "Timestamp unavailable"
        );
    }

    String resolveLifecycleSource(PayrollPeriod period, PayrollRun run) {
        return run != null && PayrollRunService.STATUS_READY_ONLY_PROCESSED.equals(run.getStatus())
                ? period == null ? null : period.getStatus()
                : run == null ? null : run.getStatus();
    }

    public boolean isCutoffProcessed(String cutoffPeriod) {
        return getLifecycleStatus(cutoffPeriod, "", "").isProcessed();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private PayrollPeriod findPayrollPeriod(String cutoffPeriod, String periodStart, String periodEnd) {
        try {
            return payrollPeriodRepository.findByCutoffAndRange(cutoffPeriod, periodStart, periodEnd);
        } catch (Exception ex) {
            return null;
        }
    }

    private PayrollLifecycleStatus.Status toLifecycleStatus(String status) {
        if (status == null) {
            return PayrollLifecycleStatus.Status.DRAFT;
        }

        return switch (periodLifecycleService.normalizeStatus(status)) {
            case PayrollPeriodLifecycleService.STATUS_OPEN_WORKFORCE_REVIEW -> PayrollLifecycleStatus.Status.OPEN_WORKFORCE_REVIEW;
            case PayrollPeriodLifecycleService.STATUS_READY_FOR_HR_VALIDATION -> PayrollLifecycleStatus.Status.READY_FOR_HR_VALIDATION;
            case PayrollPeriodLifecycleService.STATUS_READY_FOR_FINANCE_VALIDATION -> PayrollLifecycleStatus.Status.READY_FOR_FINANCE_VALIDATION;
            case PayrollPeriodLifecycleService.STATUS_READY_FOR_PROCESSING -> PayrollLifecycleStatus.Status.READY_FOR_PROCESSING;
            case PayrollPeriodLifecycleService.STATUS_PROCESSING -> PayrollLifecycleStatus.Status.PROCESSING;
            case PayrollPeriodLifecycleService.STATUS_PROCESSED -> PayrollLifecycleStatus.Status.PROCESSED;
            case PayrollPeriodLifecycleService.STATUS_LOCKED -> PayrollLifecycleStatus.Status.LOCKED;
            default -> PayrollLifecycleStatus.Status.DRAFT;
        };
    }

    public PayrollPeriod findPeriod(String cutoffPeriod, String periodStart, String periodEnd) {
        return findPayrollPeriod(cutoffPeriod, periodStart, periodEnd);
    }

    public PayrollPeriodLifecycleService getPeriodLifecycleService() {
        return periodLifecycleService;
    }
}
