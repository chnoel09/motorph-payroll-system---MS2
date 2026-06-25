package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.DeductionType;

// Prepared for the future normalized deduction_types migration.
// This repository is not wired into the current payroll runtime flow yet.
public class DeductionTypeDatabaseRepository implements DeductionTypeRepository {

    @Override
    public DeductionType findById(int deductionTypeId) {
        String sql = """
            SELECT deduction_type_id, deduction_type_name
            FROM deduction_types
            WHERE deduction_type_id = ?
            """;
        List<DeductionType> deductionTypes = findMany(sql, deductionTypeId);
        return deductionTypes.isEmpty() ? null : deductionTypes.get(0);
    }

    @Override
    public List<DeductionType> findAll() {
        String sql = """
            SELECT deduction_type_id, deduction_type_name
            FROM deduction_types
            """;
        return findMany(sql);
    }

    @Override
    public void add(DeductionType deductionType) {
        if (deductionType == null) {
            return;
        }

        String sql = "INSERT INTO deduction_types (deduction_type_name) VALUES (?)";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, deductionType.getDeductionTypeName());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(DeductionType deductionType) {
        if (deductionType == null) {
            return;
        }

        String sql = """
            UPDATE deduction_types SET deduction_type_name = ?
            WHERE deduction_type_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, deductionType.getDeductionTypeName());
                stmt.setInt(2, deductionType.getDeductionTypeId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int deductionTypeId) {
        String sql = "DELETE FROM deduction_types WHERE deduction_type_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, deductionTypeId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<DeductionType> findMany(String sql, int... parameters) {
        List<DeductionType> deductionTypes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return deductionTypes;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        deductionTypes.add(new DeductionType(
                                rs.getInt("deduction_type_id"),
                                rs.getString("deduction_type_name")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deductionTypes;
    }
}
