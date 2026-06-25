package com.mycompany.oop.service;

import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.WorkforcePayrollReadiness;
import com.mycompany.oop.model.OperationalNotification;
import com.mycompany.oop.repository.PayrollPeriodDatabaseRepository;
import com.mycompany.oop.repository.PayrollPeriodRepository;
import com.mycompany.oop.service.WorkforceReadinessService.WorkforceReadinessSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Period-level governance only. Transitions remain explicit; this service exposes guard rules.
public class PayrollPeriodLifecycleService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_OPEN_WORKFORCE_REVIEW = "OPEN_WORKFORCE_REVIEW";
    public static final String STATUS_READY_FOR_HR_VALIDATION = "READY_FOR_HR_VALIDATION";
    public static final String STATUS_READY_FOR_FINANCE_VALIDATION = "READY_FOR_FINANCE_VALIDATION";
    public static final String STATUS_READY_FOR_PROCESSING = "READY_FOR_PROCESSING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_LOCKED = "LOCKED";

    private final PayrollPeriodRepository payrollPeriodRepository;
    private final OperationalEventService operationalEventService;

    public PayrollPeriodLifecycleService() {
        this(new PayrollPeriodDatabaseRepository());
    }

    public PayrollPeriodLifecycleService(PayrollPeriodRepository payrollPeriodRepository) {
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.operationalEventService = new OperationalEventService();
    }

    public List<PayrollPeriod> getPayrollPeriods() {
        return payrollPeriodRepository == null ? List.of() : payrollPeriodRepository.findAll();
    }

    public PayrollPeriod findPeriod(String cutoffPeriod, LocalDate periodStart, LocalDate periodEnd) {
        if (payrollPeriodRepository == null || cutoffPeriod == null || periodStart == null || periodEnd == null) {
            return null;
        }
        return payrollPeriodRepository.findByCutoffAndRange(cutoffPeriod, periodStart.toString(), periodEnd.toString());
    }

    public PayrollPeriod createDraftPeriod(String cutoffPeriod, LocalDate periodStart, LocalDate periodEnd) {
        validatePeriodInput(cutoffPeriod, periodStart, periodEnd);
        PayrollPeriod existing = findPeriod(cutoffPeriod.trim(), periodStart, periodEnd);
        if (existing != null) {
            throw new IllegalArgumentException("A payroll period already exists for this cutoff and date range.");
        }

        PayrollPeriod period = new PayrollPeriod(0, cutoffPeriod.trim(), periodStart, periodEnd, STATUS_DRAFT);
        int periodId = payrollPeriodRepository.addAndReturnId(period);
        period.setPeriodId(periodId);
        operationalEventService.recordForRoles(
                OperationalEventService.PAYROLL_PERIOD_CREATED,
                "Payroll",
                OperationalNotification.Severity.INFO,
                OperationalNotification.Priority.REVIEW,
                "payroll_periods",
                String.valueOf(periodId),
                null,
                "Payroll period created",
                "Payroll period " + period.getCutoffPeriod() + " was created as Draft.",
                "Finance");
        return period;
    }

    public WorkforceReadinessSummary activateWorkforceReview(int payrollPeriodId) {
        if (payrollPeriodRepository == null || payrollPeriodId <= 0) {
            return WorkforceReadinessSummary.empty(payrollPeriodId);
        }

        PayrollPeriod period = payrollPeriodRepository.findById(payrollPeriodId);
        if (period == null) {
            throw new IllegalArgumentException("Payroll period not found.");
        }
        if (isLocked(period)) {
            throw new IllegalArgumentException("Locked payroll periods cannot be reopened for workforce review.");
        }

        period.setStatus(STATUS_OPEN_WORKFORCE_REVIEW);
        payrollPeriodRepository.update(period);
        WorkforceReadinessSummary summary = new WorkforceReadinessService().syncReadinessForPeriod(period);
        operationalEventService.recordForRoles(
                OperationalEventService.WORKFORCE_REVIEW_OPENED,
                "Workforce Readiness",
                OperationalNotification.Severity.WARNING,
                OperationalNotification.Priority.ACTION_REQUIRED,
                "payroll_periods",
                String.valueOf(payrollPeriodId),
                null,
                "Workforce review opened",
                "Supervisors should review team readiness for " + period.getCutoffPeriod()
                        + " before HR validation.",
                "Supervisor", "HR");
        return summary;
    }

    public boolean isActiveForReadiness(PayrollPeriod period) {
        String status = normalize(period == null ? null : period.getStatus());
        return STATUS_OPEN_WORKFORCE_REVIEW.equals(status)
                || STATUS_READY_FOR_HR_VALIDATION.equals(status)
                || STATUS_READY_FOR_FINANCE_VALIDATION.equals(status)
                || STATUS_READY_FOR_PROCESSING.equals(status)
                || STATUS_PROCESSING.equals(status);
    }

    public boolean isLocked(PayrollPeriod period) {
        return STATUS_LOCKED.equals(normalize(period == null ? null : period.getStatus()));
    }

    public boolean canMoveToHrValidation(PayrollPeriod period, WorkforceReadinessSummary summary) {
        return STATUS_OPEN_WORKFORCE_REVIEW.equals(normalize(period == null ? null : period.getStatus()))
                && summary != null
                && summary.getSupervisorReviewPendingCount() == 0
                && summary.getOpenWorkforceIssueCount() == 0;
    }

    public boolean canMoveToFinanceValidation(PayrollPeriod period, WorkforceReadinessSummary summary) {
        return STATUS_READY_FOR_HR_VALIDATION.equals(normalize(period == null ? null : period.getStatus()))
                && summary != null
                && summary.getHrValidationPendingCount() == 0
                && summary.getReadyForFinanceCount() > 0;
    }

    public boolean canMoveToProcessing(PayrollPeriod period, WorkforceReadinessSummary summary) {
        return STATUS_READY_FOR_FINANCE_VALIDATION.equals(normalize(period == null ? null : period.getStatus()))
                && summary != null
                && summary.getFinanceValidationPendingCount() == 0
                && summary.getPayrollReadyCount() > 0;
    }

    public boolean canLockPayrollPeriod(PayrollPeriod period) {
        return STATUS_PROCESSED.equals(normalize(period == null ? null : period.getStatus()));
    }

    public boolean areAllAttendanceEmployeesProcessed(Set<Integer> attendanceEmployeeIds,
            Set<Integer> processedEmployeeIds) {
        return attendanceEmployeeIds != null
                && !attendanceEmployeeIds.isEmpty()
                && processedEmployeeIds != null
                && processedEmployeeIds.containsAll(attendanceEmployeeIds);
    }

    public String currentOwnerForStatus(String status) {
        return switch (normalize(status)) {
            case STATUS_OPEN_WORKFORCE_REVIEW -> WorkforcePayrollReadiness.OWNER_SUPERVISOR;
            case STATUS_READY_FOR_HR_VALIDATION -> WorkforcePayrollReadiness.OWNER_HR;
            case STATUS_READY_FOR_FINANCE_VALIDATION, STATUS_READY_FOR_PROCESSING,
                    STATUS_PROCESSING, STATUS_PROCESSED, STATUS_LOCKED -> WorkforcePayrollReadiness.OWNER_FINANCE;
            default -> "";
        };
    }

    public String normalizeStatus(String status) {
        String normalized = normalize(status);
        return switch (normalized) {
            case "", "DRAFT" -> STATUS_DRAFT;
            case "READY", "READY_FOR_PROCESSING" -> STATUS_READY_FOR_PROCESSING;
            case "OPEN_WORKFORCE_REVIEW", "WORKFORCE_REVIEW" -> STATUS_OPEN_WORKFORCE_REVIEW;
            case "READY_FOR_HR_VALIDATION", "HR_VALIDATION" -> STATUS_READY_FOR_HR_VALIDATION;
            case "READY_FOR_FINANCE_VALIDATION", "FINANCE_VALIDATION" -> STATUS_READY_FOR_FINANCE_VALIDATION;
            case "PROCESSING" -> STATUS_PROCESSING;
            case "PROCESSED" -> STATUS_PROCESSED;
            case "FINALIZED", "FINALIZED_LOCKED", "FINALIZED/LOCKED", "LOCKED" -> STATUS_LOCKED;
            default -> normalized;
        };
    }

    private void validatePeriodInput(String cutoffPeriod, LocalDate periodStart, LocalDate periodEnd) {
        if (cutoffPeriod == null || cutoffPeriod.trim().isEmpty()) {
            throw new IllegalArgumentException("Cutoff label is required.");
        }
        if (periodStart == null) {
            throw new IllegalArgumentException("Period start date is required.");
        }
        if (periodEnd == null) {
            throw new IllegalArgumentException("Period end date is required.");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Period end date cannot be before the start date.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
