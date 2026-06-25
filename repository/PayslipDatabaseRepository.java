package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Payslip;

public class PayslipDatabaseRepository implements PayslipRepository {

    @Override
    public Payslip findById(int payslipId) {
        String sql = """
            SELECT payslip_id, run_detail_id, employee_id, generated_at, file_path
            FROM payslips
            WHERE payslip_id = ?
            """;
        List<Payslip> payslips = findMany(sql, payslipId);
        return payslips.isEmpty() ? null : payslips.get(0);
    }

    @Override
    public List<Payslip> findAll() {
        String sql = """
            SELECT payslip_id, run_detail_id, employee_id, generated_at, file_path
            FROM payslips
            """;
        return findMany(sql);
    }

    @Override
    public List<Payslip> findByEmployeeId(int employeeId) {
        String sql = """
            SELECT payslip_id, run_detail_id, employee_id, generated_at, file_path
            FROM payslips
            WHERE employee_id = ?
            """;
        return findMany(sql, employeeId);
    }

    @Override
    public void add(Payslip payslip) {
        if (payslip == null) {
            return;
        }

        String sql = """
            INSERT INTO payslips (
                run_detail_id, employee_id, generated_at, file_path
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayslipFields(stmt, payslip);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Payslip payslip) {
        if (payslip == null) {
            return;
        }

        String sql = """
            UPDATE payslips SET
                run_detail_id = ?,
                employee_id = ?,
                generated_at = ?,
                file_path = ?
            WHERE payslip_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayslipFields(stmt, payslip);
                stmt.setInt(5, payslip.getPayslipId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int payslipId) {
        String sql = "DELETE FROM payslips WHERE payslip_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, payslipId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Payslip> findMany(String sql, int... parameters) {
        List<Payslip> payslips = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return payslips;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        payslips.add(mapRowToPayslip(rs));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return payslips;
    }

    private void setPayslipFields(PreparedStatement stmt, Payslip payslip) throws SQLException {
        stmt.setInt(1, payslip.getRunDetailId());
        stmt.setInt(2, payslip.getEmployeeId());
        stmt.setTimestamp(3, payslip.getGeneratedAt() == null ? null : Timestamp.valueOf(payslip.getGeneratedAt()));
        stmt.setString(4, payslip.getFilePath());
    }

    private Payslip mapRowToPayslip(ResultSet rs) throws SQLException {
        Timestamp generatedAt = rs.getTimestamp("generated_at");

        return new Payslip(
                rs.getInt("payslip_id"),
                rs.getInt("run_detail_id"),
                rs.getInt("employee_id"),
                generatedAt == null ? null : generatedAt.toLocalDateTime(),
                rs.getString("file_path")
        );
    }
}
