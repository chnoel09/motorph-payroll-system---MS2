package com.mycompany.oop.service;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.OperationalNotification;
import com.mycompany.oop.model.OvertimeRequest;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollReadinessIssue;
import com.mycompany.oop.model.PayrollReadinessReport;
import com.mycompany.oop.model.User;
import com.mycompany.oop.model.WorkforcePayrollReadiness;
import com.mycompany.oop.repository.OvertimeRequestDatabaseRepository;
import com.mycompany.oop.repository.OvertimeRequestRepository;
import com.mycompany.oop.repository.PayrollPeriodDatabaseRepository;
import com.mycompany.oop.repository.PayrollPeriodRepository;
import com.mycompany.oop.repository.UserDatabaseRepository;
import com.mycompany.oop.repository.UserRepository;
import com.mycompany.oop.repository.WorkforcePayrollReadinessDatabaseRepository;
import com.mycompany.oop.repository.WorkforcePayrollReadinessRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Foundation only: derives and syncs readiness visibility without changing payroll computation.
public class WorkforceReadinessService {

    private final WorkforcePayrollReadinessRepository readinessRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final EmployeeService employeeService;
    private final PayrollReadinessService payrollReadinessService;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final PayrollPeriodLifecycleService periodLifecycleService;
    private final OperationalEventService operationalEventService;
    private final UserRepository userRepository;

    public WorkforceReadinessService() {
        this(new WorkforcePayrollReadinessDatabaseRepository(),
                new PayrollPeriodDatabaseRepository(),
                new EmployeeService(),
                new PayrollReadinessService(),
                new OvertimeRequestDatabaseRepository(),
                new PayrollPeriodLifecycleService());
    }

    public WorkforceReadinessService(WorkforcePayrollReadinessRepository readinessRepository,
            PayrollPeriodRepository payrollPeriodRepository,
            EmployeeService employeeService,
            PayrollReadinessService payrollReadinessService,
            OvertimeRequestRepository overtimeRequestRepository,
            PayrollPeriodLifecycleService periodLifecycleService) {
        this.readinessRepository = readinessRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.employeeService = employeeService;
        this.payrollReadinessService = payrollReadinessService;
        this.overtimeRequestRepository = overtimeRequestRepository;
        this.periodLifecycleService = periodLifecycleService;
        this.operationalEventService = new OperationalEventService();
        this.userRepository = new UserDatabaseRepository();
    }

    public boolean isAvailable() {
        return readinessRepository != null && readinessRepository.isAvailable();
    }

    public WorkforceReadinessSummary syncReadinessForPeriod(int payrollPeriodId) {
        PayrollPeriod period = payrollPeriodRepository == null ? null : payrollPeriodRepository.findById(payrollPeriodId);
        return syncReadinessForPeriod(period);
    }

    public WorkforceReadinessSummary syncReadinessForPeriod(PayrollPeriod period) {
        if (!isAvailable() || !isValidPeriod(period)
                || !periodLifecycleService.isActiveForReadiness(period)
                || periodLifecycleService.isLocked(period)) {
            return WorkforceReadinessSummary.empty(period == null ? 0 : period.getPeriodId());
        }

        List<Employee> employees = employeeService.getAllEmployees();
        Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee = deriveIssuesByEmployee(period);
        Map<Integer, WorkforcePayrollReadiness> existingByEmployee = readinessRepository
                .findByPayrollPeriodId(period.getPeriodId())
                .stream()
                .collect(Collectors.toMap(
                        WorkforcePayrollReadiness::getEmployeeId,
                        readiness -> readiness,
                        (left, right) -> left));

        for (Employee employee : employees) {
            if (employee == null || employee.getEmployeeId() <= 0) {
                continue;
            }

            WorkforcePayrollReadiness existing = existingByEmployee.get(employee.getEmployeeId());
            List<PayrollReadinessIssue> employeeIssues = issuesByEmployee.getOrDefault(
                    employee.getEmployeeId(), List.of());
            WorkforcePayrollReadiness readiness = existing == null
                    ? new WorkforcePayrollReadiness()
                    : existing;

            readiness.setPayrollPeriodId(period.getPeriodId());
            readiness.setEmployeeId(employee.getEmployeeId());
            readiness.setSupervisorEmployeeId(resolveSupervisorId(employee));
            readiness.setReadinessStatus(deriveStatus(period, existing, employeeIssues));
            readiness.setCurrentOwnerRole(deriveOwnerRole(readiness));

            readinessRepository.upsertDerivedState(readiness);
        }

        List<WorkforcePayrollReadiness> syncedRows = readinessRepository.findByPayrollPeriodId(period.getPeriodId());
        return summarize(period.getPeriodId(), syncedRows, issuesByEmployee);
    }

    public WorkforceReadinessSummary getReadinessSummary(int payrollPeriodId) {
        if (!isAvailable()) {
            return WorkforceReadinessSummary.empty(payrollPeriodId);
        }
        return summarize(payrollPeriodId, readinessRepository.findByPayrollPeriodId(payrollPeriodId), Map.of());
    }

    public List<WorkforceReadinessSummary> syncActivePayrollPeriods() {
        if (!isAvailable() || payrollPeriodRepository == null) {
            return List.of();
        }

        List<WorkforceReadinessSummary> summaries = new ArrayList<>();
        for (PayrollPeriod period : payrollPeriodRepository.findAll()) {
            if (periodLifecycleService.isActiveForReadiness(period)) {
                summaries.add(syncReadinessForPeriod(period));
            }
        }
        return summaries;
    }

    public List<WorkforcePayrollReadiness> getEmployeeReadinessList(int payrollPeriodId) {
        if (!isAvailable()) {
            return List.of();
        }
        return readinessRepository.findByPayrollPeriodId(payrollPeriodId);
    }

    public List<WorkforcePayrollReadiness> getSupervisorReadiness(int payrollPeriodId, int supervisorEmployeeId) {
        if (!isAvailable() || supervisorEmployeeId <= 0) {
            return List.of();
        }
        return readinessRepository.findBySupervisor(payrollPeriodId, supervisorEmployeeId);
    }

    public List<WorkforcePayrollReadiness> getSupervisorOperationalQueue(int payrollPeriodId, int supervisorEmployeeId) {
        return getSupervisorReadiness(payrollPeriodId, supervisorEmployeeId)
                .stream()
                .filter(this::isSupervisorOwned)
                .toList();
    }

    public Map<Integer, List<PayrollReadinessIssue>> getReadinessIssuesByEmployee(PayrollPeriod period) {
        if (!isValidPeriod(period)) {
            return Map.of();
        }
        return deriveIssuesByEmployee(period);
    }

    public PayrollPeriod findLatestActivePayrollPeriod() {
        if (payrollPeriodRepository == null) {
            return null;
        }

        PayrollPeriod latest = null;
        for (PayrollPeriod period : payrollPeriodRepository.findAll()) {
            if (!periodLifecycleService.isActiveForReadiness(period)) {
                continue;
            }
            if (latest == null || period.getPeriodId() > latest.getPeriodId()) {
                latest = period;
            }
        }
        return latest;
    }

    public void markReadyForHrReview(int readinessId, Employee supervisor) {
        if (!isAvailable()) {
            throw new IllegalStateException("Workforce readiness persistence is not available.");
        }
        if (supervisor == null || supervisor.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Supervisor is required.");
        }

        WorkforcePayrollReadiness readiness = readinessRepository.findById(readinessId);
        if (readiness == null) {
            throw new IllegalArgumentException("Readiness row not found.");
        }
        if (readiness.getSupervisorEmployeeId() == null
                || readiness.getSupervisorEmployeeId() != supervisor.getEmployeeId()) {
            throw new IllegalArgumentException("This readiness item is outside your assigned team scope.");
        }
        if (!isSupervisorOwned(readiness)) {
            if (isPastSupervisorReview(readiness)) {
                throw new IllegalArgumentException("This employee has already been sent forward in the workforce workflow.");
            }
            throw new IllegalArgumentException("This readiness item is not awaiting supervisor review.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(supervisor.getEmployeeId());
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_HR);
        readiness.setSupervisorClearedBy(actorUserId);
        readiness.setSupervisorClearedAt(LocalDateTime.now());
        readinessRepository.update(readiness);
        logTransition("markReadyForHrReview", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForRoles(
                OperationalEventService.WORKFORCE_READY_FOR_HR,
                "Workforce Readiness",
                OperationalNotification.Severity.WARNING,
                OperationalNotification.Priority.ACTION_REQUIRED,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                supervisor.getEmployeeId(),
                "Employee ready for HR validation",
                "Supervisor cleared employee #" + readiness.getEmployeeId()
                        + " for HR workforce validation.",
                "HR");
    }

    public void keepPendingWithRemarks(int readinessId, Employee supervisor, String remarks) {
        if (!isAvailable()) {
            throw new IllegalStateException("Workforce readiness persistence is not available.");
        }
        if (supervisor == null || supervisor.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Supervisor is required.");
        }

        WorkforcePayrollReadiness readiness = getRequiredReadiness(readinessId);
        if (readiness.getSupervisorEmployeeId() == null
                || readiness.getSupervisorEmployeeId() != supervisor.getEmployeeId()) {
            throw new IllegalArgumentException("This readiness item is outside your assigned team scope.");
        }
        if (!isSupervisorOwned(readiness)) {
            throw new IllegalArgumentException("This readiness item is not awaiting supervisor review.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(supervisor.getEmployeeId());
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_SUPERVISOR);
        readiness.setReturnReason(remarks == null || remarks.isBlank()
                ? "Kept pending for supervisor workforce follow-up."
                : remarks.trim());
        readinessRepository.update(readiness);
        logTransition("keepPendingWithRemarks", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForEmployee(
                OperationalEventService.WORKFORCE_REVIEW_PENDING,
                "Workforce Readiness",
                OperationalNotification.Severity.INFO,
                OperationalNotification.Priority.REVIEW,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                supervisor.getEmployeeId(),
                supervisor.getEmployeeId(),
                "Workforce review kept pending",
                "Employee #" + readiness.getEmployeeId()
                        + " remains in supervisor workforce review.");
    }

    public List<WorkforcePayrollReadiness> getHrReadiness(int payrollPeriodId) {
        return filterByOwner(payrollPeriodId, WorkforcePayrollReadiness.OWNER_HR);
    }

    public List<WorkforcePayrollReadiness> getHrValidationQueue(int payrollPeriodId) {
        if (!isAvailable()) {
            return List.of();
        }
        return readinessRepository.findByPayrollPeriodId(payrollPeriodId)
                .stream()
                .filter(this::isHrVisible)
                .toList();
    }

    public void validateWorkforceReadiness(int readinessId, Employee hrUser) {
        requireHrUser(hrUser);
        WorkforcePayrollReadiness readiness = getRequiredReadiness(readinessId);
        if (!isHrVisible(readiness)) {
            throw new IllegalArgumentException("This readiness item is not available for HR validation.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(hrUser.getEmployeeId());
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_HR_VALIDATED);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_HR);
        readiness.setHrValidatedBy(actorUserId);
        readiness.setHrValidatedAt(LocalDateTime.now());
        readinessRepository.update(readiness);
        logTransition("validateWorkforceReadiness", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForRoles(
                OperationalEventService.WORKFORCE_HR_VALIDATED,
                "Workforce Readiness",
                OperationalNotification.Severity.INFO,
                OperationalNotification.Priority.REVIEW,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                hrUser.getEmployeeId(),
                "HR validated workforce readiness",
                "HR validated employee #" + readiness.getEmployeeId()
                        + " for payroll readiness governance.",
                "HR");
    }

    public void endorseToFinance(int readinessId, Employee hrUser) {
        requireHrUser(hrUser);
        WorkforcePayrollReadiness readiness = getRequiredReadiness(readinessId);
        String status = normalize(readiness.getReadinessStatus());
        if (!WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status)) {
            throw new IllegalArgumentException("Validate workforce readiness before endorsement to Finance.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(hrUser.getEmployeeId());
        if (readiness.getHrValidatedBy() == null) {
            readiness.setHrValidatedBy(actorUserId);
            readiness.setHrValidatedAt(LocalDateTime.now());
        }
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_FINANCE);
        readinessRepository.update(readiness);
        logTransition("endorseToFinance", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForRoles(
                OperationalEventService.WORKFORCE_ENDORSED_TO_FINANCE,
                "Payroll Readiness",
                OperationalNotification.Severity.WARNING,
                OperationalNotification.Priority.ACTION_REQUIRED,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                hrUser.getEmployeeId(),
                "Employee endorsed to Finance",
                "HR endorsed employee #" + readiness.getEmployeeId()
                        + " for Finance payroll readiness validation.",
                "Finance");
    }

    public void returnToSupervisor(int readinessId, Employee hrUser, String reason) {
        requireHrUser(hrUser);
        WorkforcePayrollReadiness readiness = getRequiredReadiness(readinessId);
        if (readiness.getSupervisorEmployeeId() == null) {
            throw new IllegalArgumentException("This employee has no reporting supervisor to return the item to.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(hrUser.getEmployeeId());
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_SUPERVISOR);
        readiness.setReturnedBy(actorUserId);
        readiness.setReturnedToRole(WorkforcePayrollReadiness.OWNER_SUPERVISOR);
        readiness.setReturnedAt(LocalDateTime.now());
        readiness.setReturnReason(reason == null || reason.isBlank()
                ? "Returned for supervisor workforce resolution."
                : reason.trim());
        readinessRepository.update(readiness);
        logTransition("returnToSupervisor", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForEmployee(
                OperationalEventService.WORKFORCE_RETURNED_TO_SUPERVISOR,
                "Workforce Readiness",
                OperationalNotification.Severity.WARNING,
                OperationalNotification.Priority.ACTION_REQUIRED,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                hrUser.getEmployeeId(),
                readiness.getSupervisorEmployeeId(),
                "Workforce item returned",
                "HR returned employee #" + readiness.getEmployeeId()
                        + " to supervisor review.");
    }

    public List<WorkforcePayrollReadiness> getFinanceReadiness(int payrollPeriodId) {
        return filterByOwner(payrollPeriodId, WorkforcePayrollReadiness.OWNER_FINANCE);
    }

    public List<WorkforcePayrollReadiness> getFinanceValidationQueue(int payrollPeriodId) {
        if (!isAvailable()) {
            return List.of();
        }
        return readinessRepository.findByPayrollPeriodId(payrollPeriodId)
                .stream()
                .filter(this::isFinanceVisible)
                .toList();
    }

    public void confirmFinanceReadiness(int readinessId, Employee financeUser) {
        requireFinanceUser(financeUser);
        WorkforcePayrollReadiness readiness = getRequiredReadiness(readinessId);
        if (!isFinanceVisible(readiness)) {
            throw new IllegalArgumentException("This readiness item has not been endorsed to Finance.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(financeUser.getEmployeeId());
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_FINANCE);
        readiness.setFinanceValidatedBy(actorUserId);
        readiness.setFinanceValidatedAt(LocalDateTime.now());
        readinessRepository.update(readiness);
        logTransition("confirmFinanceReadiness", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForRoles(
                OperationalEventService.PAYROLL_READINESS_CONFIRMED,
                "Payroll Readiness",
                OperationalNotification.Severity.SUCCESS,
                OperationalNotification.Priority.INFORMATIONAL,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                financeUser.getEmployeeId(),
                "Finance confirmed payroll readiness",
                "Finance confirmed employee #" + readiness.getEmployeeId()
                        + " as payroll-ready.",
                "Finance", "HR");
    }

    public void markPayrollProcessed(int readinessId, Employee financeUser) {
        requireFinanceUser(financeUser);
        WorkforcePayrollReadiness readiness = getRequiredReadiness(readinessId);
        String status = normalize(readiness.getReadinessStatus());
        String owner = normalize(readiness.getCurrentOwnerRole());
        if (!WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                || !WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)) {
            throw new IllegalArgumentException("Only Finance-confirmed payroll-ready employees can be processed.");
        }

        String oldStatus = readiness.getReadinessStatus();
        String oldOwner = readiness.getCurrentOwnerRole();
        Integer actorUserId = resolveUserId(financeUser.getEmployeeId());
        readiness.setReadinessStatus(WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED);
        readiness.setCurrentOwnerRole(WorkforcePayrollReadiness.OWNER_FINANCE);
        if (readiness.getFinanceValidatedBy() == null) {
            readiness.setFinanceValidatedBy(actorUserId);
            readiness.setFinanceValidatedAt(LocalDateTime.now());
        }
        readinessRepository.update(readiness);
        logTransition("markPayrollProcessed", readiness, oldStatus, oldOwner, actorUserId);
        operationalEventService.recordForRoles(
                OperationalEventService.PAYROLL_PROCESSED,
                "Payroll",
                OperationalNotification.Severity.INFO,
                OperationalNotification.Priority.INFORMATIONAL,
                "workforce_payroll_readiness",
                String.valueOf(readinessId),
                financeUser.getEmployeeId(),
                "Payroll-ready employee processed",
                "Finance processed payroll for employee #" + readiness.getEmployeeId() + ".",
                "Finance", "HR");
    }

    private Map<Integer, List<PayrollReadinessIssue>> deriveIssuesByEmployee(PayrollPeriod period) {
        Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee = new HashMap<>();
        PayrollReadinessReport report = payrollReadinessService.evaluateReadiness(
                period.getPeriodStart(), period.getPeriodEnd());

        for (PayrollReadinessIssue issue : report.getIssues()) {
            if (issue.getEmployeeId() <= 0) {
                continue;
            }
            issuesByEmployee.computeIfAbsent(issue.getEmployeeId(), ignored -> new ArrayList<>()).add(issue);
        }

        addPendingOvertimeIssues(period, issuesByEmployee);
        return issuesByEmployee;
    }

    private void addPendingOvertimeIssues(PayrollPeriod period,
            Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee) {
        if (overtimeRequestRepository == null) {
            return;
        }

        try {
            for (OvertimeRequest request : overtimeRequestRepository.findAll()) {
                if (request == null || !isPending(request.getStatus())
                        || !isInPeriod(request.getOvertimeDate(), period)) {
                    continue;
                }

                issuesByEmployee.computeIfAbsent(request.getEmployeeId(), ignored -> new ArrayList<>())
                        .add(new PayrollReadinessIssue(
                                request.getEmployeeId(),
                                "Employee #" + request.getEmployeeId(),
                                "Pending overtime approval overlaps payroll period",
                                PayrollReadinessIssue.Severity.NEEDS_REVIEW,
                                "HR must resolve overtime approval before Finance validation."
                        ));
            }
        } catch (RuntimeException ignored) {
            // Optional workflow tables may be unavailable in older local databases.
        }
    }

    private String deriveStatus(PayrollPeriod period, WorkforcePayrollReadiness existing,
            List<PayrollReadinessIssue> employeeIssues) {
        String periodStatus = periodLifecycleService.normalizeStatus(period.getStatus());
        if (PayrollPeriodLifecycleService.STATUS_LOCKED.equals(periodStatus)) {
            return WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED;
        }
        if (PayrollPeriodLifecycleService.STATUS_PROCESSED.equals(periodStatus)
                || PayrollPeriodLifecycleService.STATUS_PROCESSING.equals(periodStatus)) {
            return WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED;
        }
        String existingStatus = existing == null ? "" : normalize(existing.getReadinessStatus());
        if (isKnownLifecycleStatus(existingStatus)
                && !WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(existingStatus)
                && !WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(existingStatus)
                && !WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(existingStatus)) {
            return existingStatus;
        }

        if (employeeIssues != null && !employeeIssues.isEmpty()) {
            return WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES;
        }

        if (WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(existingStatus)) {
            return existingStatus;
        }

        return WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW;
    }

    private String deriveOwnerRole(WorkforcePayrollReadiness readiness) {
        String status = normalize(readiness.getReadinessStatus());
        if (WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)) {
            return readiness.getSupervisorEmployeeId() == null
                    ? WorkforcePayrollReadiness.OWNER_HR
                    : WorkforcePayrollReadiness.OWNER_SUPERVISOR;
        }
        if (WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)
                || WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status)) {
            return WorkforcePayrollReadiness.OWNER_FINANCE;
        }
        return WorkforcePayrollReadiness.OWNER_HR;
    }

    private WorkforceReadinessSummary summarize(int payrollPeriodId, List<WorkforcePayrollReadiness> rows,
            Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee) {
        WorkforceReadinessSummary summary = new WorkforceReadinessSummary(payrollPeriodId);
        List<WorkforcePayrollReadiness> safeRows = rows == null ? List.of() : rows;
        summary.totalEmployees = safeRows.size();

        Set<Integer> employeesWithBlockingIssues = new HashSet<>();
        if (issuesByEmployee != null) {
            for (Map.Entry<Integer, List<PayrollReadinessIssue>> entry : issuesByEmployee.entrySet()) {
                for (PayrollReadinessIssue issue : entry.getValue()) {
                    if (issue.getSeverity() == PayrollReadinessIssue.Severity.BLOCKED) {
                        summary.blockingIssueCount++;
                        employeesWithBlockingIssues.add(entry.getKey());
                    }
                }
            }
        }

        for (WorkforcePayrollReadiness row : safeRows) {
            String status = normalize(row.getReadinessStatus());
            String owner = normalize(row.getCurrentOwnerRole());
            if (WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                    || WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)) {
                summary.openWorkforceIssueCount++;
                if (WorkforcePayrollReadiness.OWNER_SUPERVISOR.equals(owner)) {
                    summary.supervisorReviewPendingCount++;
                }
            }
            if (WorkforcePayrollReadiness.OWNER_HR.equals(owner)
                    || WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                    || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)) {
                summary.readyForHrCount++;
                summary.hrValidationPendingCount++;
            }
            if (WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                    || WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)) {
                summary.readyForFinanceCount++;
                if (!WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                        && !WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                        && !WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status)) {
                    summary.financeValidationPendingCount++;
                }
            }
            if (WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                    || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                    || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status)) {
                summary.payrollReadyCount++;
            }
        }

        if (summary.blockingIssueCount == 0) {
            summary.blockingIssueCount = summary.openWorkforceIssueCount;
        }
        summary.employeesWithBlockingIssues = employeesWithBlockingIssues.size();
        return summary;
    }

    private List<WorkforcePayrollReadiness> filterByOwner(int payrollPeriodId, String ownerRole) {
        if (!isAvailable()) {
            return List.of();
        }
        String normalizedOwner = normalize(ownerRole);
        return readinessRepository.findByPayrollPeriodId(payrollPeriodId)
                .stream()
                .filter(readiness -> normalizedOwner.equals(normalize(readiness.getCurrentOwnerRole())))
                .toList();
    }

    private Integer resolveSupervisorId(Employee employee) {
        if (employee == null) {
            return null;
        }
        Integer supervisorId = employee.getSupervisorEmployeeId();
        return supervisorId == null ? employeeService.resolveSupervisorEmployeeId(employee) : supervisorId;
    }

    private boolean isValidPeriod(PayrollPeriod period) {
        return period != null
                && period.getPeriodId() > 0
                && period.getPeriodStart() != null
                && period.getPeriodEnd() != null
                && !period.getPeriodEnd().isBefore(period.getPeriodStart());
    }

    private boolean isPending(String status) {
        String normalized = normalize(status);
        return "PENDING".equals(normalized)
                || normalized.contains("PENDING_SUPERVISOR")
                || normalized.contains("PENDING_HR")
                || normalized.contains("UNDER_REVIEW");
    }

    private boolean isInPeriod(LocalDate date, PayrollPeriod period) {
        return date != null
                && period != null
                && !date.isBefore(period.getPeriodStart())
                && !date.isAfter(period.getPeriodEnd());
    }

    private boolean isKnownLifecycleStatus(String status) {
        return WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)
                || WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(status)
                || WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)
                || WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)
                || WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isSupervisorOwned(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String status = normalize(readiness.getReadinessStatus());
        String owner = normalize(readiness.getCurrentOwnerRole());
        return WorkforcePayrollReadiness.OWNER_SUPERVISOR.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)
                || WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(status));
    }

    private boolean isPastSupervisorReview(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String status = normalize(readiness.getReadinessStatus());
        return WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)
                || WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)
                || WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status);
    }

    private Integer resolveUserId(int employeeId) {
        if (employeeId <= 0 || userRepository == null) {
            return null;
        }
        User user = userRepository.findByEmployeeId(employeeId);
        return user == null || user.getUserId() <= 0 ? null : user.getUserId();
    }

    private void logTransition(String action, WorkforcePayrollReadiness readiness,
            String oldStatus, String oldOwner, Integer actorUserId) {
        if (readiness == null) {
            return;
        }
        System.out.println("[workflow] " + action
                + " employee_id=" + readiness.getEmployeeId()
                + " readiness_id=" + readiness.getReadinessId()
                + " old_status=" + oldStatus
                + " old_owner=" + oldOwner
                + " new_status=" + readiness.getReadinessStatus()
                + " new_owner=" + readiness.getCurrentOwnerRole()
                + " actor_user_id=" + actorUserId);
    }

    private WorkforcePayrollReadiness getRequiredReadiness(int readinessId) {
        if (!isAvailable()) {
            throw new IllegalStateException("Workforce readiness persistence is not available.");
        }
        WorkforcePayrollReadiness readiness = readinessRepository.findById(readinessId);
        if (readiness == null) {
            throw new IllegalArgumentException("Readiness row not found.");
        }
        return readiness;
    }

    private void requireHrUser(Employee user) {
        if (user == null || !"hr".equalsIgnoreCase(user.getRole() == null ? "" : user.getRole().trim())) {
            throw new IllegalArgumentException("Only HR can perform workforce validation actions.");
        }
    }

    private void requireFinanceUser(Employee user) {
        if (user == null || !"finance".equalsIgnoreCase(user.getRole() == null ? "" : user.getRole().trim())) {
            throw new IllegalArgumentException("Only Finance can confirm payroll readiness.");
        }
    }

    boolean isHrVisible(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String status = normalize(readiness.getReadinessStatus());
        String owner = normalize(readiness.getCurrentOwnerRole());
        return WorkforcePayrollReadiness.OWNER_HR.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                || WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)
                || WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status));
    }

    private boolean isFinanceVisible(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String status = normalize(readiness.getReadinessStatus());
        String owner = normalize(readiness.getCurrentOwnerRole());
        return WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)
                || WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status));
    }

    public static class WorkforceReadinessSummary {
        private final int payrollPeriodId;
        private int totalEmployees;
        private int blockingIssueCount;
        private int employeesWithBlockingIssues;
        private int openWorkforceIssueCount;
        private int supervisorReviewPendingCount;
        private int hrValidationPendingCount;
        private int financeValidationPendingCount;
        private int readyForHrCount;
        private int readyForFinanceCount;
        private int payrollReadyCount;

        private WorkforceReadinessSummary(int payrollPeriodId) {
            this.payrollPeriodId = payrollPeriodId;
        }

        public static WorkforceReadinessSummary empty(int payrollPeriodId) {
            return new WorkforceReadinessSummary(payrollPeriodId);
        }

        public int getPayrollPeriodId() {
            return payrollPeriodId;
        }

        public int getTotalEmployees() {
            return totalEmployees;
        }

        public int getBlockingIssueCount() {
            return blockingIssueCount;
        }

        public int getEmployeesWithBlockingIssues() {
            return employeesWithBlockingIssues;
        }

        public int getOpenWorkforceIssueCount() {
            return openWorkforceIssueCount;
        }

        public int getSupervisorReviewPendingCount() {
            return supervisorReviewPendingCount;
        }

        public int getHrValidationPendingCount() {
            return hrValidationPendingCount;
        }

        public int getFinanceValidationPendingCount() {
            return financeValidationPendingCount;
        }

        public int getReadyForHrCount() {
            return readyForHrCount;
        }

        public int getReadyForFinanceCount() {
            return readyForFinanceCount;
        }

        public int getPayrollReadyCount() {
            return payrollReadyCount;
        }
    }
}
