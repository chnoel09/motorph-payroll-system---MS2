package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.DeductionBracket;

// Prepared for the future normalized deduction_brackets migration.
// This repository is not wired into the current payroll runtime flow yet.
public class DeductionBracketDatabaseRepository implements DeductionBracketRepository {

    @Override
    public DeductionBracket findById(int deductionBracketId) {
        String sql = """
            SELECT deduction_bracket_id, deduction_id, salary_min, salary_max,
                   employee_share, employer_share, rate, effective_date
            FROM deduction_brackets
            WHERE deduction_bracket_id = ?
            """;
        List<DeductionBracket> brackets = findMany(sql, deductionBracketId);
        return brackets.isEmpty() ? null : brackets.get(0);
    }

    @Override
    public List<DeductionBracket> findAll() {
        String sql = """
            SELECT deduction_bracket_id, deduction_id, salary_min, salary_max,
                   employee_share, employer_share, rate, effective_date
            FROM deduction_brackets
            """;
        return findMany(sql);
    }

    @Override
    public List<DeductionBracket> findByDeductionId(int deductionId) {
        String sql = """
            SELECT deduction_bracket_id, deduction_id, salary_min, salary_max,
                   employee_share, employer_share, rate, effective_date
            FROM deduction_brackets
            WHERE deduction_id = ?
            """;
        return findMany(sql, deductionId);
    }

    @Override
    public void add(DeductionBracket bracket) {
        if (bracket == null) {
            return;
        }

        String sql = """
            INSERT INTO deduction_brackets (
                deduction_id, salary_min, salary_max, employee_share,
                employer_share, rate, effective_date
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setDeductionBracketFields(stmt, bracket);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(DeductionBracket bracket) {
        if (bracket == null) {
            return;
        }

        String sql = """
            UPDATE deduction_brackets SET
                deduction_id = ?,
                salary_min = ?,
                salary_max = ?,
                employee_share = ?,
                employer_share = ?,
                rate = ?,
                effective_date = ?
            WHERE deduction_bracket_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setDeductionBracketFields(stmt, bracket);
                stmt.setInt(8, bracket.getDeductionBracketId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int deductionBracketId) {
        String sql = "DELETE FROM deduction_brackets WHERE deduction_bracket_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, deductionBracketId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<DeductionBracket> findMany(String sql, int... parameters) {
        List<DeductionBracket> brackets = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return brackets;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        brackets.add(mapRowToDeductionBracket(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return brackets;
    }

    private void setDeductionBracketFields(PreparedStatement stmt, DeductionBracket bracket) throws SQLException {
        stmt.setInt(1, bracket.getDeductionId());
        stmt.setDouble(2, bracket.getSalaryMin());
        stmt.setDouble(3, bracket.getSalaryMax());
        stmt.setDouble(4, bracket.getEmployeeShare());
        stmt.setDouble(5, bracket.getEmployerShare());
        stmt.setDouble(6, bracket.getRate());
        stmt.setDate(7, bracket.getEffectiveDate() == null ? null : java.sql.Date.valueOf(bracket.getEffectiveDate()));
    }

    private DeductionBracket mapRowToDeductionBracket(ResultSet rs) throws SQLException {
        java.sql.Date effectiveDate = rs.getDate("effective_date");

        return new DeductionBracket(
                rs.getInt("deduction_bracket_id"),
                rs.getInt("deduction_id"),
                rs.getDouble("salary_min"),
                rs.getDouble("salary_max"),
                rs.getDouble("employee_share"),
                rs.getDouble("employer_share"),
                rs.getDouble("rate"),
                effectiveDate == null ? null : effectiveDate.toLocalDate()
        );
    }
}
