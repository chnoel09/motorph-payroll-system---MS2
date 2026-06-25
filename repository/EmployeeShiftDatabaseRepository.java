package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.EmployeeShift;

// Prepared for normalized employee_shifts scheduling reference data.
// This repository is not wired into the current attendance runtime flow.
public class EmployeeShiftDatabaseRepository implements EmployeeShiftRepository {

    @Override
    public EmployeeShift findById(int shiftId) {
        String sql = """
            SELECT shift_id, shift_name, start_time, end_time,
                   grace_minutes, effective_date, assigned_by_user_id
            FROM employee_shifts
            WHERE shift_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, shiftId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToEmployeeShift(rs);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to create shift template. Please check the shift details.",
                    e
            );
        }

        return null;
    }

    @Override
    public List<EmployeeShift> findAll() {
        List<EmployeeShift> shifts = new ArrayList<>();

        String sql = """
            SELECT shift_id, shift_name, start_time, end_time,
                   grace_minutes, effective_date, assigned_by_user_id
            FROM employee_shifts
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return shifts;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    shifts.add(mapRowToEmployeeShift(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to update shift template. Please check the shift details.",
                    e
            );
        }

        return shifts;
    }

    @Override
    public void add(EmployeeShift shift) {
        if (shift == null) {
            return;
        }

        String sql = """
            INSERT INTO employee_shifts (
                shift_name,
                start_time,
                end_time,
                grace_minutes,
                effective_date,
                assigned_by_user_id
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setEmployeeShiftFields(stmt, shift);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Unable to create shift template. Please check the shift details.",
                    e
            );
        }
    }

    @Override
    public void update(EmployeeShift shift) {
        if (shift == null) {
            return;
        }

        String sql = """
            UPDATE employee_shifts SET
                shift_name = ?,
                start_time = ?,
                end_time = ?,
                grace_minutes = ?,
                effective_date = ?,
                assigned_by_user_id = ?
            WHERE shift_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setEmployeeShiftFields(stmt, shift);
                stmt.setInt(7, shift.getShiftId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int shiftId) {
        String sql = "DELETE FROM employee_shifts WHERE shift_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, shiftId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setEmployeeShiftFields(
            PreparedStatement stmt,
            EmployeeShift shift
    ) throws SQLException {

        stmt.setString(1, shift.getShiftName());

        stmt.setTime(
                2,
                shift.getStartTime() == null
                        ? null
                        : Time.valueOf(shift.getStartTime())
        );

        stmt.setTime(
                3,
                shift.getEndTime() == null
                        ? null
                        : Time.valueOf(shift.getEndTime())
        );

        stmt.setInt(4, shift.getGraceMinutes());

        stmt.setDate(
                5,
                shift.getEffectiveDate() == null
                        ? null
                        : java.sql.Date.valueOf(shift.getEffectiveDate())
        );

        stmt.setObject(6, shift.getAssignedBy());
    }

    private EmployeeShift mapRowToEmployeeShift(ResultSet rs)
            throws SQLException {

        Time startTime = rs.getTime("start_time");
        Time endTime = rs.getTime("end_time");
        java.sql.Date effectiveDate = rs.getDate("effective_date");

        return new EmployeeShift(
                rs.getInt("shift_id"),
                rs.getString("shift_name"),
                startTime == null ? null : startTime.toLocalTime(),
                endTime == null ? null : endTime.toLocalTime(),
                rs.getInt("grace_minutes"),
                effectiveDate == null ? null : effectiveDate.toLocalDate(),
                getNullableInteger(rs, "assigned_by_user_id")
        );
    }

    private Integer getNullableInteger(ResultSet rs, String columnName)
            throws SQLException {

        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}