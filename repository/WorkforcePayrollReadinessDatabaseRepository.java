package com.mycompany.oop.repository;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.WorkforcePayrollReadiness;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class WorkforcePayrollReadinessDatabaseRepository implements WorkforcePayrollReadinessRepository {

    private static final String SELECT_COLUMNS = """
            readiness_id, payroll_period_id, employee_id, readiness_status, current_owner_role,
            supervisor_employee_id, supervisor_cleared_by_user_id AS supervisor_cleared_by, supervisor_cleared_at,
            hr_validated_by_user_id AS hr_validated_by, hr_validated_at, hr_remarks,
            finance_validated_by_user_id AS finance_validated_by, finance_validated_at, finance_remarks,
            returned_by_user_id AS returned_by, returned_to_role, returned_at, return_reason,
            created_at, updated_at
            """;

    @Override
    public boolean isAvailable() {
        return hasTable("workforce_payroll_readiness");
    }

    @Override
    public WorkforcePayrollReadiness findById(int readinessId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM workforce_payroll_readiness WHERE readiness_id = ?";
        List<WorkforcePayrollReadiness> rows = findMany(sql, readinessId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public WorkforcePayrollReadiness findByPeriodAndEmployee(int payrollPeriodId, int employeeId) {
        String sql = "SELECT " + SELECT_COLUMNS + """
                FROM workforce_payroll_readiness
                WHERE payroll_period_id = ?
                AND employee_id = ?
                """;
        List<WorkforcePayrollReadiness> rows = findMany(sql, payrollPeriodId, employeeId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<WorkforcePayrollReadiness> findByPayrollPeriodId(int payrollPeriodId) {
        String sql = "SELECT " + SELECT_COLUMNS + """
                FROM workforce_payroll_readiness
                WHERE payroll_period_id = ?
                ORDER BY employee_id
                """;
        return findMany(sql, payrollPeriodId);
    }

    @Override
    public List<WorkforcePayrollReadiness> findBySupervisor(int payrollPeriodId, int supervisorEmployeeId) {
        String sql = "SELECT " + SELECT_COLUMNS + """
                FROM workforce_payroll_readiness
                WHERE payroll_period_id = ?
                AND supervisor_employee_id = ?
                ORDER BY employee_id
                """;
        return findMany(sql, payrollPeriodId, supervisorEmployeeId);
    }

    @Override
    public void upsertDerivedState(WorkforcePayrollReadiness readiness) {
        if (readiness == null || !isAvailable()) {
            return;
        }

        String sql = """
                INSERT INTO workforce_payroll_readiness (
                    payroll_period_id, employee_id, readiness_status, current_owner_role,
                    supervisor_employee_id, supervisor_cleared_by_user_id, supervisor_cleared_at,
                    hr_validated_by_user_id, hr_validated_at, hr_remarks,
                    finance_validated_by_user_id, finance_validated_at, finance_remarks,
                    returned_by_user_id, returned_to_role, returned_at, return_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    readiness_status = VALUES(readiness_status),
                    current_owner_role = VALUES(current_owner_role),
                    supervisor_employee_id = VALUES(supervisor_employee_id),
                    updated_at = CURRENT_TIMESTAMP
                """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setAllWritableFields(stmt, readiness);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(WorkforcePayrollReadiness readiness) {
        if (readiness == null || !isAvailable()) {
            return;
        }

        String sql = """
                UPDATE workforce_payroll_readiness SET
                    payroll_period_id = ?,
                    employee_id = ?,
                    readiness_status = ?,
                    current_owner_role = ?,
                    supervisor_employee_id = ?,
                    supervisor_cleared_by_user_id = ?,
                    supervisor_cleared_at = ?,
                    hr_validated_by_user_id = ?,
                    hr_validated_at = ?,
                    hr_remarks = ?,
                    finance_validated_by_user_id = ?,
                    finance_validated_at = ?,
                    finance_remarks = ?,
                    returned_by_user_id = ?,
                    returned_to_role = ?,
                    returned_at = ?,
                    return_reason = ?
                WHERE readiness_id = ?
                """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setAllWritableFields(stmt, readiness);
                stmt.setInt(18, readiness.getReadinessId());
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    throw new IllegalStateException("Readiness row was not updated.");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update workforce readiness row.", e);
        }
    }

    private List<WorkforcePayrollReadiness> findMany(String sql, int... parameters) {
        List<WorkforcePayrollReadiness> rows = new ArrayList<>();
        if (!isAvailable()) {
            return rows;
        }

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return rows;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        rows.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    private void setAllWritableFields(PreparedStatement stmt, WorkforcePayrollReadiness readiness)
            throws SQLException {
        stmt.setInt(1, readiness.getPayrollPeriodId());
        stmt.setInt(2, readiness.getEmployeeId());
        stmt.setString(3, readiness.getReadinessStatus());
        stmt.setString(4, readiness.getCurrentOwnerRole());
        stmt.setObject(5, readiness.getSupervisorEmployeeId());
        stmt.setObject(6, readiness.getSupervisorClearedBy());
        stmt.setTimestamp(7, toTimestamp(readiness.getSupervisorClearedAt()));
        stmt.setObject(8, readiness.getHrValidatedBy());
        stmt.setTimestamp(9, toTimestamp(readiness.getHrValidatedAt()));
        stmt.setString(10, readiness.getHrRemarks());
        stmt.setObject(11, readiness.getFinanceValidatedBy());
        stmt.setTimestamp(12, toTimestamp(readiness.getFinanceValidatedAt()));
        stmt.setString(13, readiness.getFinanceRemarks());
        stmt.setObject(14, readiness.getReturnedBy());
        stmt.setString(15, readiness.getReturnedToRole());
        stmt.setTimestamp(16, toTimestamp(readiness.getReturnedAt()));
        stmt.setString(17, readiness.getReturnReason());
    }

    private WorkforcePayrollReadiness mapRow(ResultSet rs) throws SQLException {
        WorkforcePayrollReadiness readiness = new WorkforcePayrollReadiness(
                rs.getInt("readiness_id"),
                rs.getInt("payroll_period_id"),
                rs.getInt("employee_id"),
                rs.getString("readiness_status"),
                rs.getString("current_owner_role"),
                getNullableInteger(rs, "supervisor_employee_id")
        );
        readiness.setSupervisorClearedBy(getNullableInteger(rs, "supervisor_cleared_by"));
        readiness.setSupervisorClearedAt(toLocalDateTime(rs.getTimestamp("supervisor_cleared_at")));
        readiness.setHrValidatedBy(getNullableInteger(rs, "hr_validated_by"));
        readiness.setHrValidatedAt(toLocalDateTime(rs.getTimestamp("hr_validated_at")));
        readiness.setHrRemarks(rs.getString("hr_remarks"));
        readiness.setFinanceValidatedBy(getNullableInteger(rs, "finance_validated_by"));
        readiness.setFinanceValidatedAt(toLocalDateTime(rs.getTimestamp("finance_validated_at")));
        readiness.setFinanceRemarks(rs.getString("finance_remarks"));
        readiness.setReturnedBy(getNullableInteger(rs, "returned_by"));
        readiness.setReturnedToRole(rs.getString("returned_to_role"));
        readiness.setReturnedAt(toLocalDateTime(rs.getTimestamp("returned_at")));
        readiness.setReturnReason(rs.getString("return_reason"));
        readiness.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        readiness.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return readiness;
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private boolean hasTable(String tableName) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return false;
            }
            try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
