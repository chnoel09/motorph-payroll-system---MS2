/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollRecord;
import com.mycompany.oop.model.PayrollRun;
import com.mycompany.oop.model.PayrollRunDetail;
import com.mycompany.oop.model.PayrollSummary;
import com.mycompany.oop.model.User;
import com.mycompany.oop.model.WorkforcePayrollReadiness;
import com.mycompany.oop.repository.PayrollHistoryDatabaseRepository;
import com.mycompany.oop.repository.PayrollHistoryRepository;
import com.mycompany.oop.repository.UserDatabaseRepository;
import com.mycompany.oop.repository.UserRepository;

public class PayrollService {

    private EmployeeService employeeService;
    private PayrollProcessor processor;
    private PayrollHistoryRepository historyRepository;
    private AttendanceService attendanceService;
    private AuditService auditService;
    private PayrollRunService payrollRunService;
    private DeductionService deductionService;
    private OperationalEventService operationalEventService;
    private UserRepository userRepository;
    private PayrollPeriodLifecycleService periodLifecycleService;

    public PayrollService() {
        this.employeeService = new EmployeeService();
        this.processor = new PayrollProcessor();
        this.historyRepository = new PayrollHistoryDatabaseRepository();
        this.attendanceService = new AttendanceService();
        this.auditService = new AuditService();
        this.payrollRunService = new PayrollRunService();
        this.deductionService = new DeductionService();
        this.operationalEventService = new OperationalEventService();
        this.userRepository = new UserDatabaseRepository();
        this.periodLifecycleService = new PayrollPeriodLifecycleService();
    }

    // ================= SUMMARY (attendance-based) =================

    public PayrollSummary generatePayrollSummary(String cutoffPeriod) {
        List<Employee> employees = employeeService.getAllEmployees();

        double totalGross = 0;
        double totalDeductions = 0;
        double totalNet = 0;
        double totalSSS = 0;
        double totalPhilhealth = 0;
        double totalPagibig = 0;
        double totalTax = 0;

        for (Employee e : employees) {
            double hours = attendanceService.getHoursForCutoff(
                    e.getEmployeeId(), cutoffPeriod);

            PayrollRecord record = processor.processPayroll(e, hours);

            totalGross += record.getGross();
            totalDeductions += record.getTotalDeductions();
            totalNet += record.getNet();
            totalSSS += record.getSss();
            totalPhilhealth += record.getPhilhealth();
            totalPagibig += record.getPagibig();
            totalTax += record.getTax();
        }

        return new PayrollSummary(
                totalGross,
                totalDeductions,
                totalNet,
                employees.size(),
                totalSSS,
                totalPhilhealth,
                totalPagibig,
                totalTax
        );
    }

    // ================= SUMMARY (legacy, fixed hours) =================

    public PayrollSummary generatePayrollSummary(double hoursWorked) {
        List<Employee> employees = employeeService.getAllEmployees();

        double totalGross = 0;
        double totalDeductions = 0;
        double totalNet = 0;
        double totalSSS = 0;
        double totalPhilhealth = 0;
        double totalPagibig = 0;
        double totalTax = 0;

        for (Employee e : employees) {
            PayrollRecord record = processor.processPayroll(e, hoursWorked);

            totalGross += record.getGross();
            totalDeductions += record.getTotalDeductions();
            totalNet += record.getNet();
            totalSSS += record.getSss();
            totalPhilhealth += record.getPhilhealth();
            totalPagibig += record.getPagibig();
            totalTax += record.getTax();
        }

        return new PayrollSummary(
                totalGross,
                totalDeductions,
                totalNet,
                employees.size(),
                totalSSS,
                totalPhilhealth,
                totalPagibig,
                totalTax
        );
    }

    // ================= PROCESS & SAVE (attendance-based) =================

    public boolean processAndSavePayroll(String cutoffPeriod, boolean overwriteIfExists) {
        LocalDate start = attendanceService.getCutoffStartDate(cutoffPeriod);
        LocalDate end = attendanceService.getCutoffEndDate(cutoffPeriod);
        return processAndSavePayroll(cutoffPeriod, start.toString(), end.toString(), overwriteIfExists);
    }

    public boolean processAndSavePayroll(String cutoffPeriod, String periodStart, String periodEnd, boolean overwriteIfExists) {
        return processAndSavePayroll(cutoffPeriod, periodStart, periodEnd, overwriteIfExists, null);
    }

    public boolean processAndSavePayroll(String cutoffPeriod, String periodStart, String periodEnd,
            boolean overwriteIfExists, Integer processedBy) {
        Integer processedByUserId = resolveUserId(processedBy);
        if (historyRepository.existsByCutoff(cutoffPeriod)) {
            if (!overwriteIfExists) {
                return false;
            }
            historyRepository.deleteByCutoff(cutoffPeriod);
        }

        List<Employee> employees = employeeService.getAllEmployees();
        LocalDate start = parseDate(periodStart);
        LocalDate end = parseDate(periodEnd);
        PayrollPeriod period = payrollRunService.getOrCreatePeriod(cutoffPeriod, start, end);
        PayrollRun run = null;

        for (Employee e : employees) {
            double hours = attendanceService.getHoursForDateRange(e.getEmployeeId(), start, end);

            PayrollRecord record = processor.processPayroll(e, hours);

            PayrollHistoryRecord history = new PayrollHistoryRecord(
                    e.getEmployeeId(),
                    cutoffPeriod,
                    periodStart,
                    periodEnd,
                    record.getHoursWorked(),
                    record.getBasicComponent(),
                    record.getAllowanceComponent(),
                    record.getGross(),
                    record.getSss(),
                    record.getPhilhealth(),
                    record.getPagibig(),
                    record.getTax(),
                    record.getTotalDeductions(),
                    record.getNet()
            );

            historyRepository.savePayrollRecord(history);

            if (run == null) {
                run = payrollRunService.createProcessedRun(period, processedByUserId);
            }

            if (run != null && run.getRunId() > 0) {
                int runDetailId = payrollRunService.addPayrollRunDetailAndReturnId(new PayrollRunDetail(
                        0,
                        run.getRunId(),
                        e.getEmployeeId(),
                        record.getHoursWorked(),
                        0.0,
                        record.getBasicComponent(),
                        record.getAllowanceComponent()
                ));
                deductionService.persistLegacyPayrollBreakdown(runDetailId, record);
            }
        }

        auditService.logAction(null, "PAYROLL_GENERATED", "payroll_history", cutoffPeriod);
        operationalEventService.recordForRoles(
                OperationalEventService.PAYROLL_PROCESSED,
                "Payroll",
                com.mycompany.oop.model.OperationalNotification.Severity.INFO,
                com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                "payroll_history",
                cutoffPeriod,
                processedByUserId,
                "Payroll cutoff processed",
                "Payroll was processed for cutoff " + cutoffPeriod + ".",
                "finance");

        return true;
    }

    public int processAndSavePayrollForEmployees(String cutoffPeriod, String periodStart, String periodEnd,
            List<Integer> employeeIds, Integer processedBy) {
        Integer processedByUserId = resolveUserId(processedBy);
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new IllegalArgumentException("At least one payroll-ready employee is required.");
        }

        Set<Integer> selectedEmployeeIds = new LinkedHashSet<>(employeeIds);
        List<Employee> employees = employeeService.getAllEmployees();
        List<Employee> selectedEmployees = new ArrayList<>();
        for (Employee employee : employees) {
            if (employee != null && selectedEmployeeIds.contains(employee.getEmployeeId())) {
                selectedEmployees.add(employee);
            }
        }
        if (selectedEmployees.isEmpty()) {
            throw new IllegalStateException("No matching payroll-ready employees were found.");
        }

        LocalDate start = parseDate(periodStart);
        LocalDate end = parseDate(periodEnd);
        List<ReadyPayrollWorkItem> workItems = new ArrayList<>();

        for (Employee employee : selectedEmployees) {
            double hours = attendanceService.getHoursForDateRange(employee.getEmployeeId(), start, end);
            PayrollRecord record = processor.processPayroll(employee, hours);
            workItems.add(new ReadyPayrollWorkItem(employee.getEmployeeId(), record));
        }

        int processedCount = persistReadyOnlyPayrollTransaction(
                cutoffPeriod,
                start,
                end,
                selectedEmployeeIds,
                workItems,
                processedByUserId);

        auditService.logAction(null, "PAYROLL_READY_SUBSET_GENERATED", "payroll_history",
                cutoffPeriod + ":" + processedCount);
        operationalEventService.recordForRoles(
                OperationalEventService.PAYROLL_PROCESSED,
                "Payroll",
                com.mycompany.oop.model.OperationalNotification.Severity.INFO,
                com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                "payroll_history",
                cutoffPeriod,
                processedByUserId,
                "Ready payroll processed",
                "Payroll was processed for " + processedCount
                        + " payroll-ready employee" + (processedCount == 1 ? "" : "s")
                        + " in cutoff " + cutoffPeriod + ".",
                "finance");

        return processedCount;
    }

    private int persistReadyOnlyPayrollTransaction(String cutoffPeriod, LocalDate periodStart, LocalDate periodEnd,
            Set<Integer> selectedEmployeeIds, List<ReadyPayrollWorkItem> workItems, Integer processedByUserId) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                throw new IllegalStateException("Unable to open a database connection for ready-only payroll.");
            }

            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int periodId = getOrCreatePayrollPeriod(conn, cutoffPeriod, periodStart, periodEnd);
                Map<Integer, Integer> readinessIdsByEmployee = lockAndValidateReadyOnlyRows(
                        conn, periodId, selectedEmployeeIds);
                int runId = insertReadyOnlyPayrollRun(conn, periodId, processedByUserId);
                Map<String, Integer> deductionIdsByName = loadDeductionIdsByName(conn);

                int processedCount = 0;
                LocalDateTime processedAt = LocalDateTime.now();
                for (ReadyPayrollWorkItem item : workItems) {
                    int runDetailId = insertPayrollRunDetail(conn, runId, item.employeeId(), item.record());
                    insertLegacyPayrollDeductions(conn, runDetailId, item.record(), deductionIdsByName);
                    upsertPayrollHistory(conn, runDetailId, item.employeeId(), cutoffPeriod,
                            periodStart, periodEnd, item.record());
                    markReadinessProcessed(conn, readinessIdsByEmployee.get(item.employeeId()),
                            processedByUserId, processedAt);
                    processedCount++;
                }

                reconcilePayrollPeriodStatus(conn, periodId, cutoffPeriod, periodStart, periodEnd);

                conn.commit();
                conn.setAutoCommit(originalAutoCommit);
                return processedCount;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                conn.setAutoCommit(originalAutoCommit);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to process ready payroll transaction.", e);
        }
    }

    public boolean reconcilePayrollPeriodStatus(int payrollPeriodId) {
        if (payrollPeriodId <= 0) {
            throw new IllegalArgumentException("A valid payroll period ID is required.");
        }

        String sql = """
            SELECT cutoff_period, period_start, period_end
            FROM payroll_periods
            WHERE period_id = ?
            """;
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                throw new IllegalStateException("Unable to open a database connection for payroll reconciliation.");
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, payrollPeriodId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Payroll period not found.");
                    }
                    return reconcilePayrollPeriodStatus(
                            conn,
                            payrollPeriodId,
                            rs.getString("cutoff_period"),
                            rs.getDate("period_start").toLocalDate(),
                            rs.getDate("period_end").toLocalDate());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to reconcile payroll period status.", e);
        }
    }

    private boolean reconcilePayrollPeriodStatus(Connection conn, int payrollPeriodId, String cutoffPeriod,
            LocalDate periodStart, LocalDate periodEnd) throws SQLException {
        Set<Integer> attendanceEmployeeIds = loadAttendanceEmployeeIds(conn, periodStart, periodEnd);
        Set<Integer> processedEmployeeIds = loadFullyProcessedEmployeeIds(
                conn, payrollPeriodId, cutoffPeriod, periodStart, periodEnd);

        if (!periodLifecycleService.areAllAttendanceEmployeesProcessed(
                attendanceEmployeeIds, processedEmployeeIds)) {
            return false;
        }

        String updateSql = """
            UPDATE payroll_periods
            SET status = ?
            WHERE period_id = ?
            AND status <> ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, PayrollPeriodLifecycleService.STATUS_PROCESSED);
            stmt.setInt(2, payrollPeriodId);
            stmt.setString(3, PayrollPeriodLifecycleService.STATUS_LOCKED);
            return stmt.executeUpdate() > 0;
        }
    }

    private Set<Integer> loadAttendanceEmployeeIds(Connection conn, LocalDate periodStart, LocalDate periodEnd)
            throws SQLException {
        Set<Integer> employeeIds = new LinkedHashSet<>();
        String sql = """
            SELECT DISTINCT employee_id
            FROM attendance_records
            WHERE attendance_date BETWEEN ? AND ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(periodStart));
            stmt.setDate(2, Date.valueOf(periodEnd));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employeeIds.add(rs.getInt("employee_id"));
                }
            }
        }
        return employeeIds;
    }

    private Set<Integer> loadFullyProcessedEmployeeIds(Connection conn, int payrollPeriodId, String cutoffPeriod,
            LocalDate periodStart, LocalDate periodEnd) throws SQLException {
        Set<Integer> employeeIds = new LinkedHashSet<>();
        String sql = """
            SELECT DISTINCT w.employee_id
            FROM workforce_payroll_readiness w
            JOIN payroll_history ph
              ON ph.employee_id = w.employee_id
             AND ph.cutoff_period = ?
             AND ph.period_start = ?
             AND ph.period_end = ?
             AND ph.run_detail_id IS NOT NULL
            JOIN payroll_run_details prd
              ON prd.run_detail_id = ph.run_detail_id
            JOIN payroll_runs pr
              ON pr.run_id = prd.run_id
             AND pr.period_id = w.payroll_period_id
            WHERE w.payroll_period_id = ?
              AND w.readiness_status = ?
              AND w.current_owner_role = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cutoffPeriod);
            stmt.setDate(2, Date.valueOf(periodStart));
            stmt.setDate(3, Date.valueOf(periodEnd));
            stmt.setInt(4, payrollPeriodId);
            stmt.setString(5, WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED);
            stmt.setString(6, WorkforcePayrollReadiness.OWNER_FINANCE);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employeeIds.add(rs.getInt("employee_id"));
                }
            }
        }
        return employeeIds;
    }

    private int getOrCreatePayrollPeriod(Connection conn, String cutoffPeriod, LocalDate periodStart,
            LocalDate periodEnd) throws SQLException {
        String findSql = """
            SELECT period_id
            FROM payroll_periods
            WHERE LOWER(cutoff_period) = LOWER(?)
            AND period_start = ?
            AND period_end = ?
            FOR UPDATE
            """;
        try (PreparedStatement stmt = conn.prepareStatement(findSql)) {
            stmt.setString(1, cutoffPeriod);
            stmt.setDate(2, Date.valueOf(periodStart));
            stmt.setDate(3, Date.valueOf(periodEnd));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("period_id");
                }
            }
        }

        String insertSql = """
            INSERT INTO payroll_periods (cutoff_period, period_start, period_end, status)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, cutoffPeriod);
            stmt.setDate(2, Date.valueOf(periodStart));
            stmt.setDate(3, Date.valueOf(periodEnd));
            stmt.setString(4, PayrollPeriodLifecycleService.STATUS_DRAFT);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new IllegalStateException("Unable to create payroll period metadata.");
    }

    private Map<Integer, Integer> lockAndValidateReadyOnlyRows(Connection conn, int periodId,
            Set<Integer> employeeIds) throws SQLException {
        Map<Integer, Integer> readinessIdsByEmployee = new LinkedHashMap<>();
        List<Integer> invalidEmployeeIds = new ArrayList<>();

        String sql = """
            SELECT readiness_id, employee_id, readiness_status, current_owner_role
            FROM workforce_payroll_readiness
            WHERE payroll_period_id = ?
            AND employee_id = ?
            FOR UPDATE
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Integer employeeId : employeeIds) {
                if (employeeId == null) {
                    continue;
                }

                stmt.setInt(1, periodId);
                stmt.setInt(2, employeeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        invalidEmployeeIds.add(employeeId);
                        continue;
                    }

                    String owner = normalizeWorkflowValue(rs.getString("current_owner_role"));
                    String status = normalizeWorkflowValue(rs.getString("readiness_status"));
                    if (!WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                            || !WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)) {
                        invalidEmployeeIds.add(employeeId);
                        continue;
                    }

                    readinessIdsByEmployee.put(employeeId, rs.getInt("readiness_id"));
                }
            }
        }

        if (!invalidEmployeeIds.isEmpty()) {
            throw new IllegalStateException("Only Finance-confirmed Payroll Ready employees can be processed. "
                    + "Invalid employee IDs: " + invalidEmployeeIds);
        }
        if (readinessIdsByEmployee.isEmpty()) {
            throw new IllegalStateException("No Finance-confirmed Payroll Ready employees were found.");
        }
        return readinessIdsByEmployee;
    }

    private int insertReadyOnlyPayrollRun(Connection conn, int periodId, Integer processedByUserId)
            throws SQLException {
        String sql = """
            INSERT INTO payroll_runs (period_id, processed_by_user_id, processed_at, status)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, periodId);
            setNullableInteger(stmt, 2, processedByUserId);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, PayrollRunService.STATUS_READY_ONLY_PROCESSED);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Unable to create ready-only payroll run metadata.");
    }

    private int insertPayrollRunDetail(Connection conn, int runId, int employeeId, PayrollRecord record)
            throws SQLException {
        String sql = """
            INSERT INTO payroll_run_details (
                run_id, employee_id, hours_worked, overtime_hours,
                basic_component, allowance_component
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, runId);
            stmt.setInt(2, employeeId);
            stmt.setDouble(3, record.getHoursWorked());
            stmt.setDouble(4, 0.0);
            stmt.setDouble(5, record.getBasicComponent());
            stmt.setDouble(6, record.getAllowanceComponent());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Unable to create payroll run detail for employee " + employeeId + ".");
    }

    private void insertLegacyPayrollDeductions(Connection conn, int runDetailId, PayrollRecord record,
            Map<String, Integer> deductionIdsByName) throws SQLException {
        Map<String, Double> amounts = new LinkedHashMap<>();
        amounts.put("SSS", record.getSss());
        amounts.put("PhilHealth", record.getPhilhealth());
        amounts.put("Pag-IBIG", record.getPagibig());
        amounts.put("Tax", record.getTax());

        String sql = """
            INSERT INTO payroll_deductions (
                run_detail_id, deduction_id, deduction_amount, remarks
            ) VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                deduction_amount = VALUES(deduction_amount),
                remarks = VALUES(remarks)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Double> entry : amounts.entrySet()) {
                Integer deductionId = deductionIdsByName.get(normalizeDeductionName(entry.getKey()));
                if (deductionId == null) {
                    continue;
                }
                stmt.setInt(1, runDetailId);
                stmt.setInt(2, deductionId);
                stmt.setDouble(3, entry.getValue() == null ? 0.0 : entry.getValue());
                stmt.setString(4, "Persisted from legacy payroll formula output");
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void upsertPayrollHistory(Connection conn, int runDetailId, int employeeId, String cutoffPeriod,
            LocalDate periodStart, LocalDate periodEnd, PayrollRecord record) throws SQLException {
        String sql = """
            INSERT INTO payroll_history (
                run_detail_id, employee_id, cutoff_period, period_start, period_end,
                hours_worked, basic_component, allowance_component, gross, sss,
                philhealth, pagibig, tax, total_deductions, net
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                run_detail_id = VALUES(run_detail_id),
                period_start = VALUES(period_start),
                period_end = VALUES(period_end),
                hours_worked = VALUES(hours_worked),
                basic_component = VALUES(basic_component),
                allowance_component = VALUES(allowance_component),
                gross = VALUES(gross),
                sss = VALUES(sss),
                philhealth = VALUES(philhealth),
                pagibig = VALUES(pagibig),
                tax = VALUES(tax),
                total_deductions = VALUES(total_deductions),
                net = VALUES(net)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, runDetailId);
            stmt.setInt(2, employeeId);
            stmt.setString(3, cutoffPeriod);
            stmt.setDate(4, Date.valueOf(periodStart));
            stmt.setDate(5, Date.valueOf(periodEnd));
            stmt.setDouble(6, record.getHoursWorked());
            stmt.setDouble(7, record.getBasicComponent());
            stmt.setDouble(8, record.getAllowanceComponent());
            stmt.setDouble(9, record.getGross());
            stmt.setDouble(10, record.getSss());
            stmt.setDouble(11, record.getPhilhealth());
            stmt.setDouble(12, record.getPagibig());
            stmt.setDouble(13, record.getTax());
            stmt.setDouble(14, record.getTotalDeductions());
            stmt.setDouble(15, record.getNet());
            stmt.executeUpdate();
        }
    }

    private void markReadinessProcessed(Connection conn, int readinessId, Integer processedByUserId,
            LocalDateTime processedAt) throws SQLException {
        String sql = """
            UPDATE workforce_payroll_readiness
            SET readiness_status = ?,
                current_owner_role = ?,
                finance_validated_by_user_id = COALESCE(finance_validated_by_user_id, ?),
                finance_validated_at = COALESCE(finance_validated_at, ?),
                updated_at = CURRENT_TIMESTAMP
            WHERE readiness_id = ?
            AND current_owner_role = ?
            AND readiness_status = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED);
            stmt.setString(2, WorkforcePayrollReadiness.OWNER_FINANCE);
            setNullableInteger(stmt, 3, processedByUserId);
            stmt.setTimestamp(4, Timestamp.valueOf(processedAt));
            stmt.setInt(5, readinessId);
            stmt.setString(6, WorkforcePayrollReadiness.OWNER_FINANCE);
            stmt.setString(7, WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED);
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("Readiness row was not updated for ready-only payroll.");
            }
        }
    }

    private Map<String, Integer> loadDeductionIdsByName(Connection conn) throws SQLException {
        Map<String, Integer> deductionIdsByName = new LinkedHashMap<>();
        String sql = "SELECT deduction_id, deduction_name FROM deductions";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                deductionIdsByName.put(normalizeDeductionName(rs.getString("deduction_name")),
                        rs.getInt("deduction_id"));
            }
        }
        return deductionIdsByName;
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }
        stmt.setInt(index, value);
    }

    private String normalizeWorkflowValue(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDeductionName(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record ReadyPayrollWorkItem(int employeeId, PayrollRecord record) {
    }

    private Integer resolveUserId(Integer employeeOrUserId) {
        if (employeeOrUserId == null) {
            return null;
        }
        User user = userRepository.findByEmployeeId(employeeOrUserId);
        return user == null ? employeeOrUserId : user.getUserId();
    }

    // ================= PROCESS & SAVE (legacy, fixed hours) =================

    public boolean processAndSavePayroll(double hoursWorked, String cutoffPeriod, boolean overwriteIfExists) {
        if (historyRepository.existsByCutoff(cutoffPeriod)) {
            if (!overwriteIfExists) {
                return false;
            }
            historyRepository.deleteByCutoff(cutoffPeriod);
        }

        List<Employee> employees = employeeService.getAllEmployees();

        for (Employee e : employees) {
            PayrollRecord record = processor.processPayroll(e, hoursWorked);

            PayrollHistoryRecord history = new PayrollHistoryRecord(
                    e.getEmployeeId(),
                    cutoffPeriod,
                    record.getHoursWorked(),
                    record.getBasicComponent(),
                    record.getAllowanceComponent(),
                    record.getGross(),
                    record.getSss(),
                    record.getPhilhealth(),
                    record.getPagibig(),
                    record.getTax(),
                    record.getTotalDeductions(),
                    record.getNet()
            );

            historyRepository.savePayrollRecord(history);
        }

        auditService.logAction(null, "PAYROLL_GENERATED", "payroll_history", cutoffPeriod);

        return true;
    }

    // ================= ATTENDANCE INTEGRATION =================

    public List<String> getAvailableCutoffs() {
        return attendanceService.getAvailableCutoffs();
    }

    public double getHoursForCutoff(int employeeId, String cutoffPeriod) {
        return attendanceService.getHoursForCutoff(employeeId, cutoffPeriod);
    }

    public double getHoursForDateRange(int employeeId, String periodStart, String periodEnd) {
        return attendanceService.getHoursForDateRange(
                employeeId,
                parseDate(periodStart),
                parseDate(periodEnd)
        );
    }

    public String getCutoffStartDate(String cutoffPeriod) {
        return attendanceService.getCutoffStartDate(cutoffPeriod).toString();
    }

    public String getCutoffEndDate(String cutoffPeriod) {
        return attendanceService.getCutoffEndDate(cutoffPeriod).toString();
    }

    // ================= SINGLE EMPLOYEE =================

    public PayrollRecord processPayrollForEmployee(Employee employee, double hoursWorked) {
        return processor.processPayroll(employee, hoursWorked);
    }

    // ================= HISTORY =================

    public List<PayrollHistoryRecord> getPayrollHistoryForEmployee(int employeeId) {
        return historyRepository.findByEmployeeId(employeeId);
    }

    public List<PayrollHistoryRecord> getPayrollHistoryByCutoff(String cutoffPeriod) {
        return historyRepository.findByCutoff(cutoffPeriod);
    }

    public List<Employee> getEmployees() {
        return employeeService.getAllEmployees();
    }

    public List<PayrollHistoryRecord> getAllPayrollHistory() {
        return historyRepository.findAll();
    }

    public List<String> getProcessedCutoffs() {
        Set<String> cutoffs = new LinkedHashSet<>();

        for (PayrollHistoryRecord record : historyRepository.findAll()) {
            if (record.getCutoffPeriod() != null && !record.getCutoffPeriod().trim().isEmpty()) {
                cutoffs.add(record.getCutoffPeriod().trim());
            }
        }

        return new ArrayList<>(cutoffs);
    }

    public boolean exportPayrollHistoryByCutoff(String cutoffPeriod, String filePath) {
        List<PayrollHistoryRecord> records = getPayrollHistoryByCutoff(cutoffPeriod);

        if (records.isEmpty()) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("employeeId,cutoffPeriod,periodStart,periodEnd,hoursWorked,basicComponent,allowanceComponent,gross,sss,philhealth,pagibig,tax,totalDeductions,net");
            writer.newLine();

            for (PayrollHistoryRecord record : records) {
                writer.write(
                        record.getEmployeeId() + "," +
                        record.getCutoffPeriod() + "," +
                        safeCsv(record.getPeriodStart()) + "," +
                        safeCsv(record.getPeriodEnd()) + "," +
                        record.getHoursWorked() + "," +
                        record.getBasicComponent() + "," +
                        record.getAllowanceComponent() + "," +
                        record.getGross() + "," +
                        record.getSss() + "," +
                        record.getPhilhealth() + "," +
                        record.getPagibig() + "," +
                        record.getTax() + "," +
                        record.getTotalDeductions() + "," +
                        record.getNet()
                );
                writer.newLine();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void deleteCutoff(String cutoffPeriod) {
        historyRepository.deleteByCutoff(cutoffPeriod);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("Invalid payroll date range.", ex);
        }
    }

    private String safeCsv(String value) {
        return value == null ? "" : value;
    }
}
