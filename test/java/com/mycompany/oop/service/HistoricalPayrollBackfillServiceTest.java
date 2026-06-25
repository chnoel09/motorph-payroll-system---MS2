package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.WorkforcePayrollReadiness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoricalPayrollBackfillServiceTest {

    private static final Logger LOGGER = Logger.getLogger(HistoricalPayrollBackfillServiceTest.class.getName());

    private StubPayrollService payrollService;
    private StubAttendanceService attendanceService;
    private StubReadinessService readinessService;
    private HistoricalPayrollBackfillService service;

    @BeforeEach
    void setUp() {
        payrollService = new StubPayrollService();
        attendanceService = new StubAttendanceService();
        readinessService = new StubReadinessService();

        List<Employee> employees = List.of(
                ServiceTestSupport.employee(10008, "Alice", "Romualdez", "Employee"),
                ServiceTestSupport.employee(10009, "Rosie", "Atienza", "Employee"),
                ServiceTestSupport.employee(10035, "Test", "Employee", "Employee"));
        PayrollPeriod period = new PayrollPeriod(
                2, "Jun-2024-2nd", LocalDate.of(2024, 6, 16), LocalDate.of(2024, 6, 30),
                PayrollPeriodLifecycleService.STATUS_OPEN_WORKFORCE_REVIEW);

        service = new HistoricalPayrollBackfillService(
                payrollService,
                attendanceService,
                new StubEmployeeService(employees),
                readinessService,
                new StubPeriodService(List.of(period)));
    }

    @Test
    void previewSeparatesAttendanceHistoryAndMissingEmployees() {
        LOGGER.info("Checking historical backfill preview classification.");
        attendanceService.addAttendance(10008, "2024-06-17");
        attendanceService.addAttendance(10009, "2024-06-18");
        payrollService.addHistory(history(10008, 18));
        readinessService.addReadiness(financeReady(2, 10009));

        HistoricalPayrollBackfillService.BackfillPlan plan = service.previewCutoff(2);

        assertEquals(List.of(10008, 10009), plan.attendanceEmployeeIds());
        assertEquals(List.of(10008), plan.alreadyProcessedEmployeeIds());
        assertEquals(List.of(10009), plan.eligibleEmployeeIds());
        assertEquals(List.of(10035), plan.noAttendanceEmployeeIds());
        assertTrue(plan.readinessBlockedEmployeeIds().isEmpty());
    }

    @Test
    void previewKeepsMissingHistoryEmployeesEligibleWhenReadinessIsBlocked() {
        attendanceService.addAttendance(10009, "2024-06-18");
        readinessService.addReadiness(readiness(2, 10009,
                WorkforcePayrollReadiness.STATUS_HR_VALIDATED,
                WorkforcePayrollReadiness.OWNER_HR));

        HistoricalPayrollBackfillService.BackfillPlan plan = service.previewCutoff(2);
        HistoricalPayrollBackfillService.BackfillReadinessResult result = service.verifyReadiness(plan);

        assertEquals(List.of(10009), plan.eligibleEmployeeIds());
        assertEquals(List.of(10009), plan.readinessBlockedEmployeeIds());
        assertFalse(result.isReady());
        assertEquals(List.of(10009), result.blockedEmployeeIds());
    }

    @Test
    void hrCanValidateHrOwnedOpenWorkforceIssues() {
        WorkforcePayrollReadiness row = readiness(2, 10001,
                WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES,
                WorkforcePayrollReadiness.OWNER_HR);

        assertTrue(readinessService.isHrVisible(row));
    }

    @Test
    void processRejectsExplicitlySelectedExistingHistory() {
        attendanceService.addAttendance(10008, "2024-06-17");
        payrollService.addHistory(history(10008, 18));
        readinessService.addReadiness(financeReady(2, 10008));

        assertThrows(IllegalStateException.class,
                () -> service.processCutoff(2, List.of(10008), 10031));
        assertEquals(0, payrollService.processCalls);
    }

    @Test
    void processPreservesExistingHistoryAndProcessesOnlyMissingEmployee() {
        attendanceService.addAttendance(10008, "2024-06-17");
        attendanceService.addAttendance(10009, "2024-06-18");
        payrollService.addHistory(history(10008, 18));
        readinessService.addReadiness(financeReady(2, 10009));

        HistoricalPayrollBackfillService.BackfillResult result = service.processCutoff(
                2, List.of(10009), 10031);

        assertEquals(1, payrollService.processCalls);
        assertEquals(List.of(10009), payrollService.capturedEmployeeIds);
        assertEquals(1, result.processedCount());
        assertEquals(2, payrollService.getPayrollHistoryByCutoff("Jun-2024-2nd").size());
    }

    @Test
    void processRejectsEmployeeWithoutAttendance() {
        readinessService.addReadiness(financeReady(2, 10035));

        assertThrows(IllegalStateException.class,
                () -> service.processCutoff(2, List.of(10035), 10031));
        assertEquals(0, payrollService.processCalls);
    }

    @Test
    void processRejectsEmployeeWithoutFinanceValidation() {
        attendanceService.addAttendance(10009, "2024-06-18");
        readinessService.addReadiness(readiness(2, 10009,
                WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE,
                WorkforcePayrollReadiness.OWNER_FINANCE));

        assertThrows(IllegalStateException.class,
                () -> service.processCutoff(2, List.of(10009), 10031));
        assertEquals(0, payrollService.processCalls);
    }

    @Test
    void processUsesFinanceReadyFlowAndVerifiesLinkedHistory() {
        LOGGER.info("Checking Finance-ready historical payroll delegation.");
        attendanceService.addAttendance(10008, "2024-06-17");
        attendanceService.addAttendance(10009, "2024-06-18");
        readinessService.addReadiness(financeReady(2, 10008));
        readinessService.addReadiness(financeReady(2, 10009));

        HistoricalPayrollBackfillService.BackfillResult result = service.processCutoff(
                2, List.of(10008, 10009), 10031);

        assertEquals(1, payrollService.processCalls);
        assertEquals("Jun-2024-2nd", payrollService.capturedCutoff);
        assertEquals("2024-06-16", payrollService.capturedStart);
        assertEquals("2024-06-30", payrollService.capturedEnd);
        assertEquals(List.of(10008, 10009), payrollService.capturedEmployeeIds);
        assertEquals(10031, payrollService.capturedFinanceEmployeeId);
        assertEquals(2, result.processedCount());
        assertEquals(2, result.verification().historyCount());
        assertEquals(0, result.verification().missingRunDetailCount());
    }

    private static WorkforcePayrollReadiness financeReady(int periodId, int employeeId) {
        return readiness(periodId, employeeId,
                WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED,
                WorkforcePayrollReadiness.OWNER_FINANCE);
    }

    private static WorkforcePayrollReadiness readiness(int periodId, int employeeId,
            String status, String owner) {
        WorkforcePayrollReadiness readiness = new WorkforcePayrollReadiness(
                employeeId, periodId, employeeId, status, owner, 10001);
        return readiness;
    }

    private static PayrollHistoryRecord history(int employeeId, int runDetailId) {
        return new PayrollHistoryRecord(
                runDetailId,
                employeeId,
                "Jun-2024-2nd",
                "2024-06-16",
                "2024-06-30",
                80,
                8000,
                750,
                8750,
                393.75,
                131.25,
                50,
                0,
                575,
                8175);
    }

    private static class StubPayrollService extends PayrollService {
        private final List<PayrollHistoryRecord> history = new ArrayList<>();
        private int processCalls;
        private String capturedCutoff;
        private String capturedStart;
        private String capturedEnd;
        private List<Integer> capturedEmployeeIds;
        private Integer capturedFinanceEmployeeId;

        void addHistory(PayrollHistoryRecord record) {
            history.add(record);
        }

        @Override
        public List<PayrollHistoryRecord> getPayrollHistoryByCutoff(String cutoffPeriod) {
            return history.stream()
                    .filter(record -> cutoffPeriod.equals(record.getCutoffPeriod()))
                    .toList();
        }

        @Override
        public int processAndSavePayrollForEmployees(String cutoffPeriod, String periodStart, String periodEnd,
                List<Integer> employeeIds, Integer processedBy) {
            processCalls++;
            capturedCutoff = cutoffPeriod;
            capturedStart = periodStart;
            capturedEnd = periodEnd;
            capturedEmployeeIds = List.copyOf(employeeIds);
            capturedFinanceEmployeeId = processedBy;
            for (Integer employeeId : employeeIds) {
                history.add(history(employeeId, 1000 + employeeId));
            }
            return employeeIds.size();
        }
    }

    private static class StubAttendanceService extends AttendanceService {
        private final Map<Integer, List<AttendanceRecord>> attendance = new LinkedHashMap<>();

        void addAttendance(int employeeId, String date) {
            attendance.computeIfAbsent(employeeId, ignored -> new ArrayList<>())
                    .add(new AttendanceRecord(employeeId, date, "08:00", "17:00"));
        }

        @Override
        public List<AttendanceRecord> getAttendanceHistory(int employeeId) {
            return attendance.getOrDefault(employeeId, List.of());
        }
    }

    private static class StubEmployeeService extends EmployeeService {
        private final List<Employee> employees;

        StubEmployeeService(List<Employee> employees) {
            this.employees = employees;
        }

        @Override
        public List<Employee> getAllEmployees() {
            return employees;
        }
    }

    private static class StubReadinessService extends WorkforceReadinessService {
        private final List<WorkforcePayrollReadiness> readiness = new ArrayList<>();

        void addReadiness(WorkforcePayrollReadiness row) {
            readiness.add(row);
        }

        @Override
        public List<WorkforcePayrollReadiness> getEmployeeReadinessList(int payrollPeriodId) {
            return readiness.stream()
                    .filter(row -> row.getPayrollPeriodId() == payrollPeriodId)
                    .toList();
        }
    }

    private static class StubPeriodService extends PayrollPeriodLifecycleService {
        private final List<PayrollPeriod> periods;

        StubPeriodService(List<PayrollPeriod> periods) {
            this.periods = periods;
        }

        @Override
        public List<PayrollPeriod> getPayrollPeriods() {
            return periods;
        }
    }
}
