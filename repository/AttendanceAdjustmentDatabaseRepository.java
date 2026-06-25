package com.mycompany.oop.repository;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.AttendanceAdjustment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AttendanceAdjustmentDatabaseRepository implements AttendanceAdjustmentRepository {

    @Override
    public boolean isAvailable() {
        return hasTable("attendance_adjustments");
    }

    @Override
    public AttendanceAdjustment findById(int adjustmentId) {
        String sql = """
            SELECT adjustment_id, employee_id, attendance_date, adjustment_type,
                   remarks, adjusted_by_user_id AS adjusted_by, adjusted_at
            FROM attendance_adjustments
            WHERE adjustment_id = ?
            """;
        List<AttendanceAdjustment> adjustments = findMany(sql, adjustmentId);
        return adjustments.isEmpty() ? null : adjustments.get(0);
    }

    @Override
    public List<AttendanceAdjustment> findAll() {
        String sql = """
            SELECT adjustment_id, employee_id, attendance_date, adjustment_type,
                   remarks, adjusted_by_user_id AS adjusted_by, adjusted_at
            FROM attendance_adjustments
            ORDER BY attendance_date DESC, adjustment_id DESC
            """;
        return findMany(sql);
    }

    @Override
    public List<AttendanceAdjustment> findByEmployeeId(int employeeId) {
        String sql = """
            SELECT adjustment_id, employee_id, attendance_date, adjustment_type,
                   remarks, adjusted_by_user_id AS adjusted_by, adjusted_at
            FROM attendance_adjustments
            WHERE employee_id = ?
            ORDER BY attendance_date DESC, adjustment_id DESC
            """;
        return findMany(sql, employeeId);
    }

    @Override
    public void addAttendanceAdjustment(AttendanceAdjustment adjustment) {
        String sql = """
            INSERT INTO attendance_adjustments (
                employee_id, attendance_date, adjustment_type, remarks, adjusted_by_user_id, adjusted_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adjustment.getEmployeeId());
            stmt.setDate(2, java.sql.Date.valueOf(adjustment.getAttendanceDate()));
            stmt.setString(3, adjustment.getAdjustmentType());
            stmt.setString(4, adjustment.getRemarks());
            stmt.setObject(5, adjustment.getAdjustedBy());
            stmt.setTimestamp(6, adjustment.getAdjustedAt() == null ? null : Timestamp.valueOf(adjustment.getAdjustedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save attendance adjustment request.", e);
        }
    }

    @Override
    public void updateAttendanceAdjustment(AttendanceAdjustment adjustment) {
        String sql = """
            UPDATE attendance_adjustments SET
                employee_id = ?,
                attendance_date = ?,
                adjustment_type = ?,
                remarks = ?,
                adjusted_by_user_id = ?,
                adjusted_at = ?
            WHERE adjustment_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adjustment.getEmployeeId());
            stmt.setDate(2, java.sql.Date.valueOf(adjustment.getAttendanceDate()));
            stmt.setString(3, adjustment.getAdjustmentType());
            stmt.setString(4, adjustment.getRemarks());
            stmt.setObject(5, adjustment.getAdjustedBy());
            stmt.setTimestamp(6, adjustment.getAdjustedAt() == null ? null : Timestamp.valueOf(adjustment.getAdjustedAt()));
            stmt.setInt(7, adjustment.getAdjustmentId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update attendance adjustment request.", e);
        }
    }

    private List<AttendanceAdjustment> findMany(String sql, int... parameters) {
        List<AttendanceAdjustment> adjustments = new ArrayList<>();
        if (!isAvailable()) {
            return adjustments;
        }

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                stmt.setInt(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    adjustments.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return adjustments;
    }

    private AttendanceAdjustment mapRow(ResultSet rs) throws SQLException {
        java.sql.Date attendanceDate = rs.getDate("attendance_date");
        Timestamp adjustedAt = rs.getTimestamp("adjusted_at");
        int adjustedBy = rs.getInt("adjusted_by");

        return new AttendanceAdjustment(
                rs.getInt("adjustment_id"),
                rs.getInt("employee_id"),
                attendanceDate == null ? null : attendanceDate.toLocalDate(),
                rs.getString("adjustment_type"),
                rs.getString("remarks"),
                rs.wasNull() ? null : adjustedBy,
                adjustedAt == null ? null : adjustedAt.toLocalDateTime()
        );
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
