package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.PayrollRun;

// Prepared for the future normalized payroll_runs migration.
// This repository is not wired into the current payroll runtime flow yet.
public class PayrollRunDatabaseRepository implements PayrollRunRepository {

    @Override
    public PayrollRun findById(int runId) {
        String sql = """
            SELECT run_id, period_id, processed_by_user_id AS processed_by, processed_at, status
            FROM payroll_runs
            WHERE run_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, runId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToPayrollRun(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public PayrollRun findLatestByPeriodId(int periodId) {
        String sql = """
            SELECT run_id, period_id, processed_by_user_id AS processed_by, processed_at, status
            FROM payroll_runs
            WHERE period_id = ?
            ORDER BY processed_at DESC, run_id DESC
            LIMIT 1
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, periodId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToPayrollRun(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<PayrollRun> findAll() {
        List<PayrollRun> runs = new ArrayList<>();

        String sql = """
            SELECT run_id, period_id, processed_by_user_id AS processed_by, processed_at, status
            FROM payroll_runs
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return runs;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    runs.add(mapRowToPayrollRun(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return runs;
    }

    @Override
    public void add(PayrollRun run) {
        if (run == null) {
            return;
        }

        String sql = """
            INSERT INTO payroll_runs (
                period_id, processed_by_user_id, processed_at, status
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollRunFields(stmt, run);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addAndReturnId(PayrollRun run) {
        if (run == null) {
            return 0;
        }

        String sql = """
            INSERT INTO payroll_runs (
                period_id, processed_by_user_id, processed_at, status
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setPayrollRunFields(stmt, run);
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

        PayrollRun latest = findLatestByPeriodId(run.getPeriodId());
        return latest == null ? 0 : latest.getRunId();
    }

    @Override
    public void update(PayrollRun run) {
        if (run == null) {
            return;
        }

        String sql = """
            UPDATE payroll_runs SET
                period_id = ?,
                processed_by_user_id = ?,
                processed_at = ?,
                status = ?
            WHERE run_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollRunFields(stmt, run);
                stmt.setInt(5, run.getRunId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int runId) {
        String sql = "DELETE FROM payroll_runs WHERE run_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, runId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setPayrollRunFields(PreparedStatement stmt, PayrollRun run) throws SQLException {
        stmt.setInt(1, run.getPeriodId());
        stmt.setObject(2, run.getProcessedBy());
        stmt.setTimestamp(3, run.getProcessedAt() == null ? null : Timestamp.valueOf(run.getProcessedAt()));
        stmt.setString(4, run.getStatus());
    }

    private PayrollRun mapRowToPayrollRun(ResultSet rs) throws SQLException {
        Timestamp processedAt = rs.getTimestamp("processed_at");

        return new PayrollRun(
                rs.getInt("run_id"),
                rs.getInt("period_id"),
                getNullableInteger(rs, "processed_by"),
                processedAt == null ? null : processedAt.toLocalDateTime(),
                rs.getString("status")
        );
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
