package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.PayrollRunDetail;

// Prepared for the future normalized payroll_run_details migration.
// This repository is not wired into the current payroll runtime flow yet.
public class PayrollRunDetailDatabaseRepository implements PayrollRunDetailRepository {

    @Override
    public PayrollRunDetail findById(int runDetailId) {
        String sql = """
            SELECT run_detail_id, run_id, employee_id, hours_worked, overtime_hours,
                   basic_component, allowance_component
            FROM payroll_run_details
            WHERE run_detail_id = ?
            """;
        return findSingle(sql, runDetailId);
    }

    @Override
    public List<PayrollRunDetail> findAll() {
        String sql = """
            SELECT run_detail_id, run_id, employee_id, hours_worked, overtime_hours,
                   basic_component, allowance_component
            FROM payroll_run_details
            """;
        return findMany(sql);
    }

    @Override
    public List<PayrollRunDetail> findByRunId(int runId) {
        String sql = """
            SELECT run_detail_id, run_id, employee_id, hours_worked, overtime_hours,
                   basic_component, allowance_component
            FROM payroll_run_details
            WHERE run_id = ?
            """;
        return findMany(sql, runId);
    }

    @Override
    public List<PayrollRunDetail> findByEmployeeId(int employeeId) {
        String sql = """
            SELECT run_detail_id, run_id, employee_id, hours_worked, overtime_hours,
                   basic_component, allowance_component
            FROM payroll_run_details
            WHERE employee_id = ?
            """;
        return findMany(sql, employeeId);
    }

    @Override
    public void add(PayrollRunDetail detail) {
        addAndReturnId(detail);
    }

    @Override
    public int addAndReturnId(PayrollRunDetail detail) {
        if (detail == null) {
            return 0;
        }

        String sql = """
            INSERT INTO payroll_run_details (
                run_id, employee_id, hours_worked, overtime_hours,
                basic_component, allowance_component
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setPayrollRunDetailFields(stmt, detail);
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int generatedId = keys.getInt(1);
                        detail.setRunDetailId(generatedId);
                        return generatedId;
                    }
                }
            }

        } catch (SQLException e) {
            PayrollRunDetail existing = findByRunAndEmployee(detail.getRunId(), detail.getEmployeeId());
            if (existing != null) {
                detail.setRunDetailId(existing.getRunDetailId());
                return existing.getRunDetailId();
            }
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void update(PayrollRunDetail detail) {
        if (detail == null) {
            return;
        }

        String sql = """
            UPDATE payroll_run_details SET
                run_id = ?,
                employee_id = ?,
                hours_worked = ?,
                overtime_hours = ?,
                basic_component = ?,
                allowance_component = ?
            WHERE run_detail_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollRunDetailFields(stmt, detail);
                stmt.setInt(7, detail.getRunDetailId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int runDetailId) {
        String sql = "DELETE FROM payroll_run_details WHERE run_detail_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, runDetailId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PayrollRunDetail findSingle(String sql, int id) {
        List<PayrollRunDetail> details = findMany(sql, id);
        return details.isEmpty() ? null : details.get(0);
    }

    private PayrollRunDetail findByRunAndEmployee(int runId, int employeeId) {
        String sql = """
            SELECT run_detail_id, run_id, employee_id, hours_worked, overtime_hours,
                   basic_component, allowance_component
            FROM payroll_run_details
            WHERE run_id = ?
            AND employee_id = ?
            """;

        List<PayrollRunDetail> details = findMany(sql, runId, employeeId);
        return details.isEmpty() ? null : details.get(0);
    }

    private List<PayrollRunDetail> findMany(String sql, int... parameters) {
        List<PayrollRunDetail> details = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return details;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        details.add(mapRowToPayrollRunDetail(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return details;
    }

    private void setPayrollRunDetailFields(PreparedStatement stmt, PayrollRunDetail detail) throws SQLException {
        stmt.setInt(1, detail.getRunId());
        stmt.setInt(2, detail.getEmployeeId());
        stmt.setDouble(3, detail.getHoursWorked());
        stmt.setDouble(4, detail.getOvertimeHours());
        stmt.setDouble(5, detail.getBasicComponent());
        stmt.setDouble(6, detail.getAllowanceComponent());
    }

    private PayrollRunDetail mapRowToPayrollRunDetail(ResultSet rs) throws SQLException {
        return new PayrollRunDetail(
                rs.getInt("run_detail_id"),
                rs.getInt("run_id"),
                rs.getInt("employee_id"),
                rs.getDouble("hours_worked"),
                rs.getDouble("overtime_hours"),
                rs.getDouble("basic_component"),
                rs.getDouble("allowance_component")
        );
    }
}
