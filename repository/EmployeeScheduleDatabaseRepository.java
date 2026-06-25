package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.EmployeeSchedule;

// Compatibility adapter: scheduleBatchId maps to schedule_window_id.
public class EmployeeScheduleDatabaseRepository implements EmployeeScheduleRepository {

    @Override
    public EmployeeSchedule findById(int scheduleId) {
        String sql = """
            SELECT schedule_id, schedule_window_id AS schedule_batch_id, employee_id, shift_id,
                   schedule_date, rest_day, holiday_id, status
            FROM employee_schedules
            WHERE schedule_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, scheduleId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToEmployeeSchedule(rs);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to assign employee schedule. Please check the assignment details.", e);
        }

        return null;
    }

    @Override
    public List<EmployeeSchedule> findAll() {
        List<EmployeeSchedule> schedules = new ArrayList<>();

        String sql = """
            SELECT schedule_id, schedule_window_id AS schedule_batch_id, employee_id, shift_id,
                   schedule_date, rest_day, holiday_id, status
            FROM employee_schedules
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return schedules;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapRowToEmployeeSchedule(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update employee schedule. Please check the assignment details.", e);
        }

        return schedules;
    }

    @Override
    public List<EmployeeSchedule> findByEmployeeId(int employeeId) {
        List<EmployeeSchedule> schedules = new ArrayList<>();

        String sql = """
            SELECT schedule_id, schedule_window_id AS schedule_batch_id, employee_id, shift_id,
                   schedule_date, rest_day, holiday_id, status
            FROM employee_schedules
            WHERE employee_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return schedules;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        schedules.add(mapRowToEmployeeSchedule(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete employee schedule.", e);
        }

        return schedules;
    }

    @Override
    public List<EmployeeSchedule> findByDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        List<EmployeeSchedule> schedules = new ArrayList<>();

        String sql = """
            SELECT schedule_id, schedule_window_id AS schedule_batch_id, employee_id, shift_id,
                   schedule_date, rest_day, holiday_id, status
            FROM employee_schedules
            WHERE employee_id = ?
            AND schedule_date BETWEEN ? AND ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null || startDate == null || endDate == null) {
                return schedules;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);
                stmt.setDate(2, java.sql.Date.valueOf(startDate));
                stmt.setDate(3, java.sql.Date.valueOf(endDate));

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        schedules.add(mapRowToEmployeeSchedule(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return schedules;
    }

    @Override
    public void add(EmployeeSchedule schedule) {
        if (schedule == null) {
            return;
        }

        String sql = """
            INSERT INTO employee_schedules (
                schedule_window_id, employee_id, shift_id, schedule_date,
                rest_day, holiday_id, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setEmployeeScheduleFields(stmt, schedule);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(EmployeeSchedule schedule) {
        if (schedule == null) {
            return;
        }

        String sql = """
            UPDATE employee_schedules SET
                schedule_window_id = ?,
                employee_id = ?,
                shift_id = ?,
                schedule_date = ?,
                rest_day = ?,
                holiday_id = ?,
                status = ?
            WHERE schedule_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setEmployeeScheduleFields(stmt, schedule);
                stmt.setInt(8, schedule.getScheduleId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int scheduleId) {
        String sql = "DELETE FROM employee_schedules WHERE schedule_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, scheduleId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setEmployeeScheduleFields(PreparedStatement stmt, EmployeeSchedule schedule) throws SQLException {
        stmt.setInt(1, schedule.getScheduleBatchId());
        stmt.setInt(2, schedule.getEmployeeId());
        stmt.setObject(3, schedule.getShiftId());
        stmt.setDate(4, schedule.getScheduleDate() == null ? null : java.sql.Date.valueOf(schedule.getScheduleDate()));
        stmt.setBoolean(5, schedule.isRestDay());
        stmt.setObject(6, schedule.getHolidayId());
        stmt.setString(7, schedule.getStatus());
    }

    private EmployeeSchedule mapRowToEmployeeSchedule(ResultSet rs) throws SQLException {
        java.sql.Date scheduleDate = rs.getDate("schedule_date");

        return new EmployeeSchedule(
                rs.getInt("schedule_id"),
                rs.getInt("schedule_batch_id"),
                rs.getInt("employee_id"),
                getNullableInteger(rs, "shift_id"),
                scheduleDate == null ? null : scheduleDate.toLocalDate(),
                rs.getBoolean("rest_day"),
                getNullableInteger(rs, "holiday_id"),
                rs.getString("status")
        );
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
