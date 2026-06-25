package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.AttendanceRecord;

public class AttendanceDatabaseRepository implements AttendanceRepository {

    @Override
    public void saveAttendance(AttendanceRecord record) {
        String sql = """
            INSERT INTO attendance_records (
                employee_id,
                attendance_date,
                time_in,
                time_out
            ) VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, record.getEmployeeId());
            stmt.setDate(2, Date.valueOf(record.getDate()));
            stmt.setString(3, record.getTimeIn());
            stmt.setString(4, record.getTimeOut());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateAttendance(AttendanceRecord updatedRecord) {
        String sql = """
            UPDATE attendance_records
            SET time_in = ?, time_out = ?
            WHERE employee_id = ?
            AND attendance_date = ?
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, updatedRecord.getTimeIn());
            stmt.setString(2, updatedRecord.getTimeOut());
            stmt.setInt(3, updatedRecord.getEmployeeId());
            stmt.setDate(4, Date.valueOf(updatedRecord.getDate()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<AttendanceRecord> findAll() {
        List<AttendanceRecord> records = new ArrayList<>();

        String sql = """
            SELECT employee_id, attendance_date, time_in, time_out
            FROM attendance_records
            ORDER BY attendance_date, employee_id
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                records.add(mapRowToAttendanceRecord(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public List<AttendanceRecord> findByEmployeeId(int employeeId) {
        List<AttendanceRecord> records = new ArrayList<>();

        String sql = """
            SELECT employee_id, attendance_date, time_in, time_out
            FROM attendance_records
            WHERE employee_id = ?
            ORDER BY attendance_date
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRowToAttendanceRecord(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public AttendanceRecord findByEmployeeAndDate(int employeeId, String date) {
        String sql = """
            SELECT employee_id, attendance_date, time_in, time_out
            FROM attendance_records
            WHERE employee_id = ?
            AND attendance_date = ?
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAttendanceRecord(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private AttendanceRecord mapRowToAttendanceRecord(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getInt("employee_id"),
                rs.getDate("attendance_date").toString(),
                rs.getString("time_in"),
                rs.getString("time_out")
        );
    }
}