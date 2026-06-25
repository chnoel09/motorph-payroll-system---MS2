package com.mycompany.oop.repository;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.PersistentNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationDatabaseRepository implements NotificationRepository {

    @Override
    public void add(PersistentNotification notification) {
        if (notification == null || notification.getEventId() <= 0) {
            return;
        }

        String sql = """
            INSERT INTO notifications (
                event_id, target_employee_id, target_role, title, message, category,
                severity, priority, notification_status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, notification.getEventId());
                if (notification.getTargetEmployeeId() == null) {
                    stmt.setNull(2, java.sql.Types.INTEGER);
                } else {
                    stmt.setInt(2, notification.getTargetEmployeeId());
                }
                stmt.setString(3, notification.getTargetRole());
                stmt.setString(4, notification.getTitle());
                stmt.setString(5, notification.getMessage());
                stmt.setString(6, notification.getCategory());
                stmt.setString(7, notification.getSeverity());
                stmt.setString(8, notification.getPriority());
                stmt.setString(9, notification.getNotificationStatus());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PersistentNotification> findActiveForEmployeeOrRole(int employeeId, String role, int limit) {
        List<PersistentNotification> notifications = new ArrayList<>();
        if (limit <= 0) {
            return notifications;
        }

        String sql = """
            SELECT notification_id, event_id, target_employee_id, target_role, title, message,
                   category, severity, priority, notification_status, read_at,
                   acknowledged_at, created_at
            FROM notifications
            WHERE notification_status = 'ACTIVE'
              AND (
                    target_employee_id = ?
                    OR LOWER(target_role) = LOWER(?)
                    OR (target_employee_id IS NULL AND target_role IS NULL)
                  )
            ORDER BY created_at DESC, notification_id DESC
            LIMIT ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return notifications;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);
                stmt.setString(2, role == null ? "" : role.trim());
                stmt.setInt(3, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notifications.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return notifications;
    }

    @Override
    public int countUnreadActiveForEmployeeOrRole(int employeeId, String role) {
        String sql = """
            SELECT COUNT(*) AS unread_count
            FROM notifications
            WHERE notification_status = 'ACTIVE'
              AND read_at IS NULL
              AND (
                    target_employee_id = ?
                    OR LOWER(target_role) = LOWER(?)
                    OR (target_employee_id IS NULL AND target_role IS NULL)
                  )
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);
                stmt.setString(2, role == null ? "" : role.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt("unread_count") : 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public int markActiveReadForEmployeeOrRole(int employeeId, String role) {
        String sql = """
            UPDATE notifications
            SET read_at = NOW()
            WHERE notification_status = 'ACTIVE'
              AND read_at IS NULL
              AND (
                    target_employee_id = ?
                    OR LOWER(target_role) = LOWER(?)
                    OR (target_employee_id IS NULL AND target_role IS NULL)
                  )
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);
                stmt.setString(2, role == null ? "" : role.trim());
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void markRead(int notificationId) {
        updateTimestamp(notificationId, "read_at");
    }

    @Override
    public void acknowledge(int notificationId) {
        updateTimestamp(notificationId, "acknowledged_at");
    }

    private void updateTimestamp(int notificationId, String column) {
        if (notificationId <= 0) {
            return;
        }

        String sql = "UPDATE notifications SET " + column + " = NOW() WHERE notification_id = ?";
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, notificationId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PersistentNotification mapRow(ResultSet rs) throws SQLException {
        int targetEmployeeId = rs.getInt("target_employee_id");
        Integer targetEmployee = rs.wasNull() ? null : targetEmployeeId;
        Timestamp readAt = rs.getTimestamp("read_at");
        Timestamp acknowledgedAt = rs.getTimestamp("acknowledged_at");
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new PersistentNotification(
                rs.getInt("notification_id"),
                rs.getInt("event_id"),
                targetEmployee,
                rs.getString("target_role"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("category"),
                rs.getString("severity"),
                rs.getString("priority"),
                rs.getString("notification_status"),
                readAt == null ? null : readAt.toLocalDateTime(),
                acknowledgedAt == null ? null : acknowledgedAt.toLocalDateTime(),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }
}
