package com.mycompany.oop.repository;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.OperationalEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OperationalEventDatabaseRepository implements OperationalEventRepository {

    @Override
    public int addAndReturnId(OperationalEvent event) {
        if (event == null) {
            return 0;
        }

        String sql = """
            INSERT INTO operational_events (
                event_type, category, severity, priority, reference_table, reference_id,
                actor_user_id, title, message, event_status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, event.getEventType());
                stmt.setString(2, event.getCategory());
                stmt.setString(3, event.getSeverity());
                stmt.setString(4, event.getPriority());
                stmt.setString(5, event.getReferenceTable());
                stmt.setString(6, event.getReferenceId());
                if (event.getActorEmployeeId() == null) {
                    stmt.setNull(7, java.sql.Types.INTEGER);
                } else {
                    stmt.setInt(7, event.getActorEmployeeId());
                }
                stmt.setString(8, event.getTitle());
                stmt.setString(9, event.getMessage());
                stmt.setString(10, event.getEventStatus());
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

        return 0;
    }

    @Override
    public List<OperationalEvent> findRecentActive(int limit) {
        List<OperationalEvent> events = new ArrayList<>();
        if (limit <= 0) {
            return events;
        }

        String sql = """
            SELECT event_id, event_type, category, severity, priority, reference_table,
                   reference_id, actor_user_id AS actor_employee_id, title, message, event_status, created_at
            FROM operational_events
            WHERE event_status = 'ACTIVE'
            ORDER BY created_at DESC, event_id DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return events;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        events.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return events;
    }

    private OperationalEvent mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        int actorId = rs.getInt("actor_employee_id");
        Integer actor = rs.wasNull() ? null : actorId;
        return new OperationalEvent(
                rs.getInt("event_id"),
                rs.getString("event_type"),
                rs.getString("category"),
                rs.getString("severity"),
                rs.getString("priority"),
                rs.getString("reference_table"),
                rs.getString("reference_id"),
                actor,
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("event_status"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }
}
