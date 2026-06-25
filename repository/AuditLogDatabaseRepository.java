package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.AuditLog;

// Prepared for the future normalized audit_logs migration.
// This repository is not wired into the current runtime flow yet.
public class AuditLogDatabaseRepository implements AuditLogRepository {

    @Override
    public List<AuditLog> findAll() {
        String sql = """
            SELECT audit_id, user_id, action, table_name, record_id, created_at
            FROM audit_logs
            """;
        return findMany(sql);
    }

    @Override
    public List<AuditLog> findByUserId(int userId) {
        String sql = """
            SELECT audit_id, user_id, action, table_name, record_id, created_at
            FROM audit_logs
            WHERE user_id = ?
            """;
        return findMany(sql, userId);
    }

    @Override
    public void addAuditLog(AuditLog auditLog) {
        if (auditLog == null) {
            return;
        }

        String sql = """
            INSERT INTO audit_logs (
                user_id, action, table_name, record_id, created_at
            ) VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, auditLog.getUserId());
                stmt.setString(2, auditLog.getAction());
                stmt.setString(3, auditLog.getTableName());
                stmt.setString(4, auditLog.getRecordId());
                stmt.setTimestamp(5, auditLog.getCreatedAt() == null
                        ? null
                        : Timestamp.valueOf(auditLog.getCreatedAt()));
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<AuditLog> findMany(String sql, int... parameters) {
        List<AuditLog> auditLogs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return auditLogs;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        auditLogs.add(mapRowToAuditLog(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return auditLogs;
    }

    private AuditLog mapRowToAuditLog(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new AuditLog(
                rs.getInt("audit_id"),
                getNullableInteger(rs, "user_id"),
                rs.getString("action"),
                rs.getString("table_name"),
                rs.getString("record_id"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
