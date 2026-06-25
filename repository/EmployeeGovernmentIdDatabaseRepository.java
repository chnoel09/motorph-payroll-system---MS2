package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.EmployeeGovernmentId;

// Prepared for the future employee_government_ids normalization migration.
// This repository is not wired into the current Employee CRUD flow yet.
public class EmployeeGovernmentIdDatabaseRepository implements EmployeeGovernmentIdRepository {

    @Override
    public List<EmployeeGovernmentId> findByEmployeeId(int employeeId) {
        List<EmployeeGovernmentId> governmentIds = new ArrayList<>();

        String sql = """
            SELECT
                government_id AS employee_gov_id,
                employee_id,
                id_type AS government_id_type,
                id_number AS government_id_number
            FROM employee_government_ids
            WHERE employee_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return governmentIds;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        governmentIds.add(mapRowToEmployeeGovernmentId(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return governmentIds;
    }

    @Override
    public void addGovernmentId(EmployeeGovernmentId governmentId) {
        if (governmentId == null) {
            return;
        }

        String sql = """
            INSERT INTO employee_government_ids (
                employee_id,
                id_type,
                id_number
            ) VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, governmentId.getEmployeeId());
                stmt.setString(2, governmentId.getGovernmentIdType());
                stmt.setString(3, governmentId.getGovernmentIdNumber());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateGovernmentId(EmployeeGovernmentId governmentId) {
        if (governmentId == null) {
            return;
        }

        String sql = """
            UPDATE employee_government_ids SET
                employee_id = ?,
                id_type = ?,
                id_number = ?
            WHERE government_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, governmentId.getEmployeeId());
                stmt.setString(2, governmentId.getGovernmentIdType());
                stmt.setString(3, governmentId.getGovernmentIdNumber());
                stmt.setInt(4, governmentId.getEmployeeGovId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteGovernmentId(int employeeGovId) {
        String sql = "DELETE FROM employee_government_ids WHERE government_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeGovId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private EmployeeGovernmentId mapRowToEmployeeGovernmentId(ResultSet rs) throws SQLException {
        return new EmployeeGovernmentId(
                rs.getInt("employee_gov_id"),
                rs.getInt("employee_id"),
                rs.getString("government_id_type"),
                rs.getString("government_id_number")
        );
    }
}
