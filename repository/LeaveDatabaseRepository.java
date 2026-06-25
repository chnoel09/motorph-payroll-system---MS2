package com.mycompany.oop.repository;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Leave;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveDatabaseRepository implements LeaveRepository {

    @Override
    public void addLeave(Leave leave) {
        if (leave == null) {
            return;
        }

        String sql = """
            INSERT INTO leaves (
                leave_id, employee_id, leave_type, start_date, end_date, reason, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leave.getLeaveId());
            stmt.setInt(2, leave.getEmployeeId());
            stmt.setString(3, leave.getLeaveType());
            stmt.setDate(4, toSqlDate(leave.getStartDate()));
            stmt.setDate(5, toSqlDate(leave.getEndDate()));
            stmt.setString(6, leave.getReason());
            stmt.setString(7, leave.getStatus());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add leave request.", e);
        }
    }

    @Override
    public void updateLeave(Leave leave) {
        if (leave == null) {
            return;
        }

        String sql = """
            UPDATE leaves SET
                employee_id = ?,
                leave_type = ?,
                start_date = ?,
                end_date = ?,
                reason = ?,
                status = ?
            WHERE leave_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leave.getEmployeeId());
            stmt.setString(2, leave.getLeaveType());
            stmt.setDate(3, toSqlDate(leave.getStartDate()));
            stmt.setDate(4, toSqlDate(leave.getEndDate()));
            stmt.setString(5, leave.getReason());
            stmt.setString(6, leave.getStatus());
            stmt.setInt(7, leave.getLeaveId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update leave request.", e);
        }
    }

    @Override
    public Leave findLeave(int leaveId) {
        String sql = """
            SELECT leave_id, employee_id, leave_type, start_date, end_date, reason, status
            FROM leaves
            WHERE leave_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaveId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLeave(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find leave request.", e);
        }

        return null;
    }

    @Override
    public List<Leave> getAllLeaves() {
        String sql = """
            SELECT leave_id, employee_id, leave_type, start_date, end_date, reason, status
            FROM leaves
            ORDER BY leave_id
            """;

        return findMany(sql);
    }

    @Override
    public List<Leave> getLeavesByEmployee(int employeeId) {
        String sql = """
            SELECT leave_id, employee_id, leave_type, start_date, end_date, reason, status
            FROM leaves
            WHERE employee_id = ?
            ORDER BY start_date DESC, leave_id DESC
            """;

        List<Leave> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    leaves.add(mapRowToLeave(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load employee leave requests.", e);
        }

        return leaves;
    }

    private List<Leave> findMany(String sql) {
        List<Leave> leaves = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                leaves.add(mapRowToLeave(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load leave requests.", e);
        }

        return leaves;
    }

    private Leave mapRowToLeave(ResultSet rs) throws SQLException {
        Date startDate = rs.getDate("start_date");
        Date endDate = rs.getDate("end_date");

        return new Leave(
                rs.getInt("leave_id"),
                rs.getInt("employee_id"),
                rs.getString("leave_type"),
                startDate == null ? null : startDate.toLocalDate().toString(),
                endDate == null ? null : endDate.toLocalDate().toString(),
                rs.getString("reason"),
                rs.getString("status")
        );
    }

    private Date toSqlDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(value.trim()));
    }
}
