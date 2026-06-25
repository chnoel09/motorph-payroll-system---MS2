package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.PayrollPeriod;

// Prepared for the future normalized payroll_periods migration.
// This repository is not wired into the current payroll runtime flow yet.
public class PayrollPeriodDatabaseRepository implements PayrollPeriodRepository {

    @Override
    public PayrollPeriod findById(int periodId) {
        String sql = """
            SELECT period_id, cutoff_period, period_start, period_end, status
            FROM payroll_periods
            WHERE period_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, periodId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToPayrollPeriod(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public PayrollPeriod findByCutoffAndRange(String cutoffPeriod, String periodStart, String periodEnd) {
        String sql = """
            SELECT period_id, cutoff_period, period_start, period_end, status
            FROM payroll_periods
            WHERE LOWER(cutoff_period) = LOWER(?)
            AND period_start = ?
            AND period_end = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cutoffPeriod);
                stmt.setDate(2, java.sql.Date.valueOf(periodStart));
                stmt.setDate(3, java.sql.Date.valueOf(periodEnd));

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToPayrollPeriod(rs);
                    }
                }
            }

        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<PayrollPeriod> findAll() {
        List<PayrollPeriod> periods = new ArrayList<>();

        String sql = """
            SELECT period_id, cutoff_period, period_start, period_end, status
            FROM payroll_periods
            ORDER BY period_start DESC, period_id DESC
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return periods;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    periods.add(mapRowToPayrollPeriod(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return periods;
    }

    @Override
    public void add(PayrollPeriod period) {
        if (period == null) {
            return;
        }

        String sql = """
            INSERT INTO payroll_periods (
                cutoff_period, period_start, period_end, status
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollPeriodFields(stmt, period);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addAndReturnId(PayrollPeriod period) {
        if (period == null) {
            return 0;
        }

        String sql = """
            INSERT INTO payroll_periods (
                cutoff_period, period_start, period_end, status
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setPayrollPeriodFields(stmt, period);
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        PayrollPeriod existing = findByCutoffAndRange(
                period.getCutoffPeriod(),
                period.getPeriodStart() == null ? null : period.getPeriodStart().toString(),
                period.getPeriodEnd() == null ? null : period.getPeriodEnd().toString()
        );
        return existing == null ? 0 : existing.getPeriodId();
    }

    @Override
    public void update(PayrollPeriod period) {
        if (period == null) {
            return;
        }

        String sql = """
            UPDATE payroll_periods SET
                cutoff_period = ?,
                period_start = ?,
                period_end = ?,
                status = ?
            WHERE period_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollPeriodFields(stmt, period);
                stmt.setInt(5, period.getPeriodId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int periodId) {
        String sql = "DELETE FROM payroll_periods WHERE period_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, periodId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setPayrollPeriodFields(PreparedStatement stmt, PayrollPeriod period) throws SQLException {
        stmt.setString(1, period.getCutoffPeriod());
        stmt.setDate(2, period.getPeriodStart() == null ? null : java.sql.Date.valueOf(period.getPeriodStart()));
        stmt.setDate(3, period.getPeriodEnd() == null ? null : java.sql.Date.valueOf(period.getPeriodEnd()));
        stmt.setString(4, period.getStatus());
    }

    private PayrollPeriod mapRowToPayrollPeriod(ResultSet rs) throws SQLException {
        java.sql.Date periodStart = rs.getDate("period_start");
        java.sql.Date periodEnd = rs.getDate("period_end");

        return new PayrollPeriod(
                rs.getInt("period_id"),
                rs.getString("cutoff_period"),
                periodStart == null ? null : periodStart.toLocalDate(),
                periodEnd == null ? null : periodEnd.toLocalDate(),
                rs.getString("status")
        );
    }
}
