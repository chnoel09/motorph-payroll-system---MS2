package com.mycompany.oop.service;

import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.WorkforcePayrollReadiness;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class HistoricalPayrollBackfillService {

    private static final Logger LOGGER = Logger.getLogger(HistoricalPayrollBackfillService.class.getName());

    private final PayrollService payrollService;
    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final WorkforceReadinessService readinessService;
    private final PayrollPeriodLifecycleService periodService;

    public HistoricalPayrollBackfillService() {
        this(new PayrollService(), new AttendanceService(), new EmployeeService(),
                new WorkforceReadinessService(), new PayrollPeriodLifecycleService());
    }

    public HistoricalPayrollBackfillService(PayrollService payrollService,
            AttendanceService attendanceService, EmployeeService employeeService,
            WorkforceReadinessService readinessService,
            PayrollPeriodLifecycleService periodService) {
        this.payrollService = requireDependency(payrollService, "PayrollService");
        this.attendanceService = requireDependency(attendanceService, "AttendanceService");
        this.employeeService = requireDependency(employeeService, "EmployeeService");
        this.readinessService = requireDependency(readinessService, "WorkforceReadinessService");
        this.periodService = requireDependency(periodService, "PayrollPeriodLifecycleService");
    }

    public BackfillPlan previewCutoff(int payrollPeriodId) {
        PayrollPeriod period = findRequiredPeriod(payrollPeriodId);
        List<Employee> employees = employeeService.getAllEmployees();
        Set<Integer> attendanceEmployeeIds = findAttendanceEmployeeIds(employees, period);
        Set<Integer> alreadyProcessedIds = findAlreadyProcessedIds(period);
        Map<Integer, WorkforcePayrollReadiness> readinessByEmployee = readinessByEmployee(payrollPeriodId);

        List<Integer> eligibleIds = new ArrayList<>();
        List<Integer> noAttendanceIds = new ArrayList<>();
        List<Integer> readinessBlockedIds = new ArrayList<>();

        for (Employee employee : employees) {
            if (employee == null || employee.getEmployeeId() <= 0) {
                continue;
            }

            int employeeId = employee.getEmployeeId();
            if (!attendanceEmployeeIds.contains(employeeId)) {
                noAttendanceIds.add(employeeId);
                continue;
            }
            if (alreadyProcessedIds.contains(employeeId)) {
                continue;
            }

            eligibleIds.add(employeeId);
            if (!isFinanceValidated(readinessByEmployee.get(employeeId))) {
                readinessBlockedIds.add(employeeId);
            }
        }

        return new BackfillPlan(
                period,
                List.copyOf(attendanceEmployeeIds),
                List.copyOf(alreadyProcessedIds),
                List.copyOf(eligibleIds),
                List.copyOf(noAttendanceIds),
                List.copyOf(readinessBlockedIds));
    }

    public BackfillReadinessResult verifyReadiness(BackfillPlan plan) {
        if (plan == null || plan.period() == null) {
            throw new IllegalArgumentException("A backfill plan is required.");
        }

        Map<Integer, WorkforcePayrollReadiness> readinessByEmployee = readinessByEmployee(
                plan.period().getPeriodId());
        List<Integer> financeValidated = new ArrayList<>();
        List<Integer> blocked = new ArrayList<>();

        for (Integer employeeId : plan.eligibleEmployeeIds()) {
            if (isFinanceValidated(readinessByEmployee.get(employeeId))) {
                financeValidated.add(employeeId);
            } else {
                blocked.add(employeeId);
            }
        }

        return new BackfillReadinessResult(List.copyOf(financeValidated), List.copyOf(blocked));
    }

    public BackfillResult processCutoff(int payrollPeriodId, List<Integer> selectedEmployeeIds,
            int financeEmployeeId) {
        BackfillPlan plan = previewCutoff(payrollPeriodId);
        Set<Integer> selectedIds = validateSelection(plan, selectedEmployeeIds);
        validateSelectedReadiness(plan.period().getPeriodId(), selectedIds);

        int processedCount = payrollService.processAndSavePayrollForEmployees(
                plan.period().getCutoffPeriod(),
                plan.period().getPeriodStart().toString(),
                plan.period().getPeriodEnd().toString(),
                List.copyOf(selectedIds),
                financeEmployeeId);

        if (processedCount != selectedIds.size()) {
            throw new IllegalStateException("Processed employee count does not match the selected backfill count.");
        }

        BackfillVerification verification = verifyCutoff(plan.period(), selectedIds);
        return new BackfillResult(
                plan.period().getPeriodId(),
                plan.period().getCutoffPeriod(),
                List.copyOf(selectedIds),
                processedCount,
                verification);
    }

    public List<BackfillResult> processAll(List<Integer> payrollPeriodIds, int financeEmployeeId) {
        if (payrollPeriodIds == null || payrollPeriodIds.isEmpty()) {
            throw new IllegalArgumentException("At least one payroll period is required.");
        }

        List<BackfillResult> results = new ArrayList<>();
        for (Integer payrollPeriodId : payrollPeriodIds) {
            if (payrollPeriodId == null) {
                throw new IllegalArgumentException("Payroll period ID cannot be null.");
            }

            BackfillPlan plan = previewCutoff(payrollPeriodId);
            if (plan.eligibleEmployeeIds().isEmpty()) {
                continue;
            }
            results.add(processCutoff(payrollPeriodId, plan.eligibleEmployeeIds(), financeEmployeeId));
        }
        return List.copyOf(results);
    }

    private Set<Integer> validateSelection(BackfillPlan plan, List<Integer> selectedEmployeeIds) {
        if (selectedEmployeeIds == null || selectedEmployeeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one employee must be selected for backfill.");
        }

        Set<Integer> selectedIds = new LinkedHashSet<>(selectedEmployeeIds);
        if (selectedIds.size() != selectedEmployeeIds.size() || selectedIds.contains(null)) {
            throw new IllegalArgumentException("Selected employee IDs must be unique and non-null.");
        }

        Set<Integer> alreadyProcessed = new LinkedHashSet<>(plan.alreadyProcessedEmployeeIds());
        Set<Integer> attendanceEmployees = new LinkedHashSet<>(plan.attendanceEmployeeIds());
        List<Integer> historyConflicts = selectedIds.stream().filter(alreadyProcessed::contains).toList();
        if (!historyConflicts.isEmpty()) {
            throw new IllegalStateException("Payroll history already exists for employee IDs: " + historyConflicts);
        }

        List<Integer> withoutAttendance = selectedIds.stream()
                .filter(employeeId -> !attendanceEmployees.contains(employeeId))
                .toList();
        if (!withoutAttendance.isEmpty()) {
            throw new IllegalStateException("No attendance exists for employee IDs: " + withoutAttendance);
        }

        return selectedIds;
    }

    private void validateSelectedReadiness(int payrollPeriodId, Set<Integer> selectedEmployeeIds) {
        Map<Integer, WorkforcePayrollReadiness> readinessByEmployee = readinessByEmployee(payrollPeriodId);
        List<Integer> blocked = selectedEmployeeIds.stream()
                .filter(employeeId -> !isFinanceValidated(readinessByEmployee.get(employeeId)))
                .toList();
        if (!blocked.isEmpty()) {
            throw new IllegalStateException("Employees are not Finance validated: " + blocked);
        }
    }

    private BackfillVerification verifyCutoff(PayrollPeriod period, Set<Integer> selectedEmployeeIds) {
        Map<Integer, PayrollHistoryRecord> historyByEmployee = new LinkedHashMap<>();
        for (PayrollHistoryRecord record : payrollService.getPayrollHistoryByCutoff(period.getCutoffPeriod())) {
            if (record != null && selectedEmployeeIds.contains(record.getEmployeeId())) {
                historyByEmployee.put(record.getEmployeeId(), record);
            }
        }

        List<Integer> missingHistory = selectedEmployeeIds.stream()
                .filter(employeeId -> !historyByEmployee.containsKey(employeeId))
                .toList();
        List<Integer> missingRunDetails = historyByEmployee.values().stream()
                .filter(record -> record.getRunDetailId() == null || record.getRunDetailId() <= 0)
                .map(PayrollHistoryRecord::getEmployeeId)
                .toList();

        if (!missingHistory.isEmpty() || !missingRunDetails.isEmpty()) {
            throw new IllegalStateException("Backfill verification failed. Missing history: " + missingHistory
                    + ", missing run details: " + missingRunDetails);
        }

        double totalHours = historyByEmployee.values().stream()
                .mapToDouble(PayrollHistoryRecord::getHoursWorked).sum();
        double totalGross = historyByEmployee.values().stream()
                .mapToDouble(PayrollHistoryRecord::getGross).sum();
        double totalDeductions = historyByEmployee.values().stream()
                .mapToDouble(PayrollHistoryRecord::getTotalDeductions).sum();
        double totalNet = historyByEmployee.values().stream()
                .mapToDouble(PayrollHistoryRecord::getNet).sum();

        BackfillVerification verification = new BackfillVerification(
                selectedEmployeeIds.size(),
                historyByEmployee.size(),
                missingRunDetails.size(),
                totalHours,
                totalGross,
                totalDeductions,
                totalNet);
        LOGGER.info(() -> "Historical payroll verified for " + period.getCutoffPeriod()
                + ": selected=" + verification.selectedCount()
                + ", history=" + verification.historyCount()
                + ", linked=" + (verification.historyCount() - verification.missingRunDetailCount()));
        return verification;
    }

    private Set<Integer> findAttendanceEmployeeIds(List<Employee> employees, PayrollPeriod period) {
        Set<Integer> employeeIds = new LinkedHashSet<>();
        for (Employee employee : employees) {
            if (employee == null || employee.getEmployeeId() <= 0) {
                continue;
            }
            if (hasAttendanceInPeriod(employee.getEmployeeId(), period.getPeriodStart(), period.getPeriodEnd())) {
                employeeIds.add(employee.getEmployeeId());
            }
        }
        return employeeIds;
    }

    private boolean hasAttendanceInPeriod(int employeeId, LocalDate periodStart, LocalDate periodEnd) {
        for (AttendanceRecord record : attendanceService.getAttendanceHistory(employeeId)) {
            try {
                LocalDate date = LocalDate.parse(record.getDate());
                if (!date.isBefore(periodStart) && !date.isAfter(periodEnd)) {
                    return true;
                }
            } catch (DateTimeParseException e) {
                // Skip invalid historical dates.
            }
        }
        return false;
    }

    private Set<Integer> findAlreadyProcessedIds(PayrollPeriod period) {
        Set<Integer> employeeIds = new LinkedHashSet<>();
        for (PayrollHistoryRecord record : payrollService.getPayrollHistoryByCutoff(period.getCutoffPeriod())) {
            if (record != null && record.getEmployeeId() > 0) {
                employeeIds.add(record.getEmployeeId());
            }
        }
        return employeeIds;
    }

    private Map<Integer, WorkforcePayrollReadiness> readinessByEmployee(int payrollPeriodId) {
        Map<Integer, WorkforcePayrollReadiness> readinessByEmployee = new LinkedHashMap<>();
        for (WorkforcePayrollReadiness readiness : readinessService.getEmployeeReadinessList(payrollPeriodId)) {
            if (readiness != null && readiness.getEmployeeId() > 0) {
                readinessByEmployee.put(readiness.getEmployeeId(), readiness);
            }
        }
        return readinessByEmployee;
    }

    private boolean isFinanceValidated(WorkforcePayrollReadiness readiness) {
        return readiness != null
                && WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(readiness.getReadinessStatus())
                && WorkforcePayrollReadiness.OWNER_FINANCE.equals(readiness.getCurrentOwnerRole());
    }

    private PayrollPeriod findRequiredPeriod(int payrollPeriodId) {
        if (payrollPeriodId <= 0) {
            throw new IllegalArgumentException("A valid payroll period ID is required.");
        }
        return periodService.getPayrollPeriods().stream()
                .filter(period -> period != null && period.getPeriodId() == payrollPeriodId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payroll period not found."));
    }

    private static <T> T requireDependency(T dependency, String name) {
        if (dependency == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return dependency;
    }

    public record BackfillPlan(
            PayrollPeriod period,
            List<Integer> attendanceEmployeeIds,
            List<Integer> alreadyProcessedEmployeeIds,
            List<Integer> eligibleEmployeeIds,
            List<Integer> noAttendanceEmployeeIds,
            List<Integer> readinessBlockedEmployeeIds) {
    }

    public record BackfillReadinessResult(
            List<Integer> financeValidatedEmployeeIds,
            List<Integer> blockedEmployeeIds) {

        public boolean isReady() {
            return blockedEmployeeIds.isEmpty();
        }
    }

    public record BackfillResult(
            int payrollPeriodId,
            String cutoffPeriod,
            List<Integer> processedEmployeeIds,
            int processedCount,
            BackfillVerification verification) {
    }

    public record BackfillVerification(
            int selectedCount,
            int historyCount,
            int missingRunDetailCount,
            double totalHours,
            double totalGross,
            double totalDeductions,
            double totalNet) {
    }
}
