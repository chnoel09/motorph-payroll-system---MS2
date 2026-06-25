package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.LeaveApproval;

// Prepared for the future normalized leave_approvals workflow migration.
// This repository is not wired into the current leave runtime flow yet.
public class LeaveApprovalDatabaseRepository implements LeaveApprovalRepository {

    @Override
    public List<LeaveApproval> findByLeaveId(int leaveId) {
        List<LeaveApproval> approvals = new ArrayList<>();

        String sql = """
            SELECT approval_id, leave_id, approved_by_user_id AS approved_by, approval_role,
                   action, approval_date, remarks
            FROM leave_approvals
            WHERE leave_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return approvals;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, leaveId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        approvals.add(mapRowToLeaveApproval(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return approvals;
    }

    @Override
    public void addLeaveApproval(LeaveApproval approval) {
        if (approval == null) {
            return;
        }

        String sql = """
            INSERT INTO leave_approvals (
                leave_id, approved_by_user_id, approval_role, action, approval_date, remarks
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, approval.getLeaveId());
                stmt.setInt(2, approval.getApprovedBy());
                stmt.setString(3, approval.getApprovalRole());
                stmt.setString(4, approval.getAction());
                stmt.setTimestamp(5, approval.getApprovalDate() == null
                        ? null
                        : Timestamp.valueOf(approval.getApprovalDate()));
                stmt.setString(6, approval.getRemarks());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private LeaveApproval mapRowToLeaveApproval(ResultSet rs) throws SQLException {
        Timestamp approvalDate = rs.getTimestamp("approval_date");

        return new LeaveApproval(
                rs.getInt("approval_id"),
                rs.getInt("leave_id"),
                rs.getInt("approved_by"),
                rs.getString("approval_role"),
                rs.getString("action"),
                approvalDate == null ? null : approvalDate.toLocalDateTime(),
                rs.getString("remarks")
        );
    }
}
