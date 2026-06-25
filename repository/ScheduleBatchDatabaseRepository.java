package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.ScheduleBatch;

// Compatibility adapter: ScheduleBatch maps to schedule_windows.
public class ScheduleBatchDatabaseRepository implements ScheduleBatchRepository {

    @Override
    public ScheduleBatch findById(int scheduleBatchId) {
        String sql = """
            SELECT schedule_window_id AS schedule_batch_id,
                   department_id,
                   created_by_user_id AS uploaded_by,
                   coverage_start AS schedule_month,
                   status,
                   created_at AS uploaded_at,
                   finalized_at,
                   finalized_by_user_id AS finalized_by
            FROM schedule_windows
            WHERE schedule_window_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, scheduleBatchId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToScheduleBatch(rs);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load schedule coverage. Please check the coverage details.", e);
        }

        return null;
    }

    @Override
    public List<ScheduleBatch> findAll() {
        List<ScheduleBatch> batches = new ArrayList<>();

        String sql = """
            SELECT schedule_window_id AS schedule_batch_id,
                   department_id,
                   created_by_user_id AS uploaded_by,
                   coverage_start AS schedule_month,
                   status,
                   created_at AS uploaded_at,
                   finalized_at,
                   finalized_by_user_id AS finalized_by
            FROM schedule_windows
            ORDER BY coverage_start DESC, schedule_window_id DESC
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return batches;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    batches.add(mapRowToScheduleBatch(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return batches;
    }

    @Override
    public void add(ScheduleBatch batch) {
        if (batch == null) {
            return;
        }

        String sql = """
            INSERT INTO schedule_windows (
                department_id, window_name, coverage_start, coverage_end, status,
                created_by_user_id, created_at, finalized_by_user_id, finalized_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setScheduleWindowInsertFields(conn, stmt, batch);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(ScheduleBatch batch) {
        if (batch == null) {
            return;
        }

        String sql = """
            UPDATE schedule_windows SET
                department_id = ?,
                window_name = ?,
                coverage_start = ?,
                coverage_end = ?,
                status = ?,
                created_by_user_id = ?,
                created_at = ?,
                finalized_by_user_id = ?,
                finalized_at = ?
            WHERE schedule_window_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setScheduleWindowUpdateFields(conn, stmt, batch);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int scheduleBatchId) {
        String sql = "DELETE FROM schedule_windows WHERE schedule_window_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, scheduleBatchId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setScheduleWindowInsertFields(Connection conn, PreparedStatement stmt, ScheduleBatch batch) throws SQLException {
        LocalDate coverageStart = batch.getScheduleMonth();
        LocalDate coverageEnd = resolveCoverageEnd(coverageStart);

        stmt.setObject(1, batch.getDepartmentId());
        stmt.setString(2, buildWindowName(coverageStart));
        stmt.setDate(3, coverageStart == null ? null : java.sql.Date.valueOf(coverageStart));
        stmt.setDate(4, coverageEnd == null ? null : java.sql.Date.valueOf(coverageEnd));
        stmt.setString(5, batch.getStatus());
        stmt.setObject(6, resolveUserId(conn, batch.getUploadedBy()));
        stmt.setTimestamp(7, batch.getUploadedAt() == null ? null : Timestamp.valueOf(batch.getUploadedAt()));
        stmt.setObject(8, resolveUserId(conn, batch.getFinalizedBy()));
        stmt.setTimestamp(9, batch.getFinalizedAt() == null ? null : Timestamp.valueOf(batch.getFinalizedAt()));
    }

    private void setScheduleWindowUpdateFields(Connection conn, PreparedStatement stmt, ScheduleBatch batch) throws SQLException {
        LocalDate coverageStart = batch.getScheduleMonth();
        LocalDate coverageEnd = resolveCoverageEnd(coverageStart);

        stmt.setObject(1, batch.getDepartmentId());
        stmt.setString(2, buildWindowName(coverageStart));
        stmt.setDate(3, coverageStart == null ? null : java.sql.Date.valueOf(coverageStart));
        stmt.setDate(4, coverageEnd == null ? null : java.sql.Date.valueOf(coverageEnd));
        stmt.setString(5, batch.getStatus());
        stmt.setObject(6, resolveUserId(conn, batch.getUploadedBy()));
        stmt.setTimestamp(7, batch.getUploadedAt() == null ? null : Timestamp.valueOf(batch.getUploadedAt()));
        stmt.setObject(8, resolveUserId(conn, batch.getFinalizedBy()));
        stmt.setTimestamp(9, batch.getFinalizedAt() == null ? null : Timestamp.valueOf(batch.getFinalizedAt()));
        stmt.setInt(10, batch.getScheduleBatchId());
    }

    private ScheduleBatch mapRowToScheduleBatch(ResultSet rs) throws SQLException {
        java.sql.Date scheduleMonth = rs.getDate("schedule_month");
        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        Timestamp finalizedAt = rs.getTimestamp("finalized_at");

        return new ScheduleBatch(
                rs.getInt("schedule_batch_id"),
                getNullableInteger(rs, "department_id"),
                rs.getInt("uploaded_by"),
                scheduleMonth == null ? null : scheduleMonth.toLocalDate(),
                rs.getString("status"),
                uploadedAt == null ? null : uploadedAt.toLocalDateTime(),
                finalizedAt == null ? null : finalizedAt.toLocalDateTime(),
                getNullableInteger(rs, "finalized_by")
        );
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private String buildWindowName(LocalDate coverageStart) {
        return coverageStart == null ? "Schedule Coverage" : "Schedule Coverage " + coverageStart;
    }

    private LocalDate resolveCoverageEnd(LocalDate coverageStart) {
        return coverageStart == null ? null : coverageStart.plusMonths(1).minusDays(1);
    }

    private Integer resolveUserId(Connection conn, Integer employeeOrUserId) throws SQLException {
        if (employeeOrUserId == null || employeeOrUserId <= 0) {
            return null;
        }

        Integer byUserId = findUserId(conn, "user_id", employeeOrUserId);
        if (byUserId != null) {
            return byUserId;
        }

        return findUserId(conn, "employee_id", employeeOrUserId);
    }

    private Integer findUserId(Connection conn, String columnName, int value) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE " + columnName + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        return null;
    }
}
