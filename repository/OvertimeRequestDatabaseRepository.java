package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.OvertimeRequest;

// Prepared for the future normalized overtime_requests workflow migration.
// This repository is not wired into the current payroll or attendance runtime flow yet.
public class OvertimeRequestDatabaseRepository implements OvertimeRequestRepository {

    @Override
    public OvertimeRequest findById(int overtimeId) {
        String sql = """
            SELECT overtime_id, employee_id, overtime_date, overtime_hours,
                   reason, status, approved_by_user_id AS approved_by, approved_role, approved_at, remarks
            FROM overtime_requests
            WHERE overtime_id = ?
            """;
        List<OvertimeRequest> requests = findMany(sql, overtimeId);
        return requests.isEmpty() ? null : requests.get(0);
    }

    @Override
    public List<OvertimeRequest> findByEmployeeId(int employeeId) {
        String sql = """
            SELECT overtime_id, employee_id, overtime_date, overtime_hours,
                   reason, status, approved_by_user_id AS approved_by, approved_role, approved_at, remarks
            FROM overtime_requests
            WHERE employee_id = ?
            """;
        return findMany(sql, employeeId);
    }

    @Override
    public List<OvertimeRequest> findAll() {
        String sql = """
            SELECT overtime_id, employee_id, overtime_date, overtime_hours,
                   reason, status, approved_by_user_id AS approved_by, approved_role, approved_at, remarks
            FROM overtime_requests
            """;
        return findMany(sql);
    }

    @Override
    public void addOvertimeRequest(OvertimeRequest request) {
        if (request == null) {
            return;
        }

        String sql = """
            INSERT INTO overtime_requests (
                employee_id, overtime_date, overtime_hours, reason,
                status, approved_by_user_id, approved_role, approved_at, remarks
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setOvertimeRequestFields(stmt, request);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateOvertimeRequest(OvertimeRequest request) {
        if (request == null) {
            return;
        }

        String sql = """
            UPDATE overtime_requests SET
                employee_id = ?,
                overtime_date = ?,
                overtime_hours = ?,
                reason = ?,
                status = ?,
                approved_by_user_id = ?,
                approved_role = ?,
                approved_at = ?,
                remarks = ?
            WHERE overtime_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setOvertimeRequestFields(stmt, request);
                stmt.setInt(10, request.getOvertimeId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<OvertimeRequest> findMany(String sql, int... parameters) {
        List<OvertimeRequest> requests = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return requests;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        requests.add(mapRowToOvertimeRequest(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }

    private void setOvertimeRequestFields(PreparedStatement stmt, OvertimeRequest request) throws SQLException {
        stmt.setInt(1, request.getEmployeeId());
        stmt.setDate(2, request.getOvertimeDate() == null ? null : java.sql.Date.valueOf(request.getOvertimeDate()));
        stmt.setDouble(3, request.getOvertimeHours());
        stmt.setString(4, request.getReason());
        stmt.setString(5, request.getStatus());
        stmt.setObject(6, request.getApprovedBy());
        stmt.setString(7, request.getApprovedRole());
        stmt.setTimestamp(8, request.getApprovedAt() == null ? null : Timestamp.valueOf(request.getApprovedAt()));
        stmt.setString(9, request.getRemarks());
    }

    private OvertimeRequest mapRowToOvertimeRequest(ResultSet rs) throws SQLException {
        java.sql.Date overtimeDate = rs.getDate("overtime_date");
        Timestamp approvedAt = rs.getTimestamp("approved_at");

        return new OvertimeRequest(
                rs.getInt("overtime_id"),
                rs.getInt("employee_id"),
                overtimeDate == null ? null : overtimeDate.toLocalDate(),
                rs.getDouble("overtime_hours"),
                rs.getString("reason"),
                rs.getString("status"),
                getNullableInteger(rs, "approved_by"),
                rs.getString("approved_role"),
                approvedAt == null ? null : approvedAt.toLocalDateTime(),
                rs.getString("remarks")
        );
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
