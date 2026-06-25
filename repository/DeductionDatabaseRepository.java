package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Deduction;

// Prepared for the future normalized deductions migration.
// This repository is not wired into the current payroll runtime flow yet.
public class DeductionDatabaseRepository implements DeductionRepository {

    @Override
    public Deduction findById(int deductionId) {
        String sql = """
            SELECT deduction_id, deduction_type_id, deduction_name,
                   government_mandated, computation_method
            FROM deductions
            WHERE deduction_id = ?
            """;
        List<Deduction> deductions = findMany(sql, deductionId);
        return deductions.isEmpty() ? null : deductions.get(0);
    }

    @Override
    public List<Deduction> findAll() {
        String sql = """
            SELECT deduction_id, deduction_type_id, deduction_name,
                   government_mandated, computation_method
            FROM deductions
            """;
        return findMany(sql);
    }

    @Override
    public void add(Deduction deduction) {
        if (deduction == null) {
            return;
        }

        String sql = """
            INSERT INTO deductions (
                deduction_type_id, deduction_name, government_mandated, computation_method
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setDeductionFields(stmt, deduction);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Deduction deduction) {
        if (deduction == null) {
            return;
        }

        String sql = """
            UPDATE deductions SET
                deduction_type_id = ?,
                deduction_name = ?,
                government_mandated = ?,
                computation_method = ?
            WHERE deduction_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setDeductionFields(stmt, deduction);
                stmt.setInt(5, deduction.getDeductionId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int deductionId) {
        String sql = "DELETE FROM deductions WHERE deduction_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, deductionId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Deduction> findMany(String sql, int... parameters) {
        List<Deduction> deductions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return deductions;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        deductions.add(new Deduction(
                                rs.getInt("deduction_id"),
                                rs.getInt("deduction_type_id"),
                                rs.getString("deduction_name"),
                                rs.getBoolean("government_mandated"),
                                rs.getString("computation_method")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return deductions;
    }

    private void setDeductionFields(PreparedStatement stmt, Deduction deduction) throws SQLException {
        stmt.setInt(1, deduction.getDeductionTypeId());
        stmt.setString(2, deduction.getDeductionName());
        stmt.setBoolean(3, deduction.isGovernmentMandated());
        stmt.setString(4, deduction.getComputationMethod());
    }
}
