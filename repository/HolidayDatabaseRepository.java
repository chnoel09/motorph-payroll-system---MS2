package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Holiday;

// Prepared for normalized holidays scheduling reference data.
// This repository is not wired into the current attendance runtime flow.
public class HolidayDatabaseRepository implements HolidayRepository {

    @Override
    public Holiday findById(int holidayId) {
        String sql = """
            SELECT holiday_id, holiday_name, holiday_date, holiday_type, multiplier
            FROM holidays
            WHERE holiday_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, holidayId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToHoliday(rs);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to create holiday. Please check the holiday details and date.", e);
        }

        return null;
    }

    @Override
    public List<Holiday> findAll() {
        List<Holiday> holidays = new ArrayList<>();

        String sql = """
            SELECT holiday_id, holiday_name, holiday_date, holiday_type, multiplier
            FROM holidays
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return holidays;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    holidays.add(mapRowToHoliday(rs));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update holiday. Please check the holiday details and date.", e);
        }

        return holidays;
    }

    @Override
    public List<Holiday> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Holiday> holidays = new ArrayList<>();

        String sql = """
            SELECT holiday_id, holiday_name, holiday_date, holiday_type, multiplier
            FROM holidays
            WHERE holiday_date BETWEEN ? AND ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null || startDate == null || endDate == null) {
                return holidays;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDate(1, java.sql.Date.valueOf(startDate));
                stmt.setDate(2, java.sql.Date.valueOf(endDate));

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        holidays.add(mapRowToHoliday(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete holiday. It may still be referenced by scheduling records.", e);
        }

        return holidays;
    }

    @Override
    public void add(Holiday holiday) {
        if (holiday == null) {
            return;
        }

        String sql = """
            INSERT INTO holidays (
                holiday_name, holiday_date, holiday_type, multiplier
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setHolidayFields(stmt, holiday);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Holiday holiday) {
        if (holiday == null) {
            return;
        }

        String sql = """
            UPDATE holidays SET
                holiday_name = ?,
                holiday_date = ?,
                holiday_type = ?,
                multiplier = ?
            WHERE holiday_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setHolidayFields(stmt, holiday);
                stmt.setInt(5, holiday.getHolidayId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int holidayId) {
        String sql = "DELETE FROM holidays WHERE holiday_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, holidayId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setHolidayFields(PreparedStatement stmt, Holiday holiday) throws SQLException {
        stmt.setString(1, holiday.getHolidayName());
        stmt.setDate(2, holiday.getHolidayDate() == null ? null : java.sql.Date.valueOf(holiday.getHolidayDate()));
        stmt.setString(3, holiday.getHolidayType());
        stmt.setDouble(4, holiday.getMultiplier());
    }

    private Holiday mapRowToHoliday(ResultSet rs) throws SQLException {
        java.sql.Date holidayDate = rs.getDate("holiday_date");

        return new Holiday(
                rs.getInt("holiday_id"),
                rs.getString("holiday_name"),
                holidayDate == null ? null : holidayDate.toLocalDate(),
                rs.getString("holiday_type"),
                rs.getDouble("multiplier")
        );
    }
}
