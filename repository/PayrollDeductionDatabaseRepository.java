package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.PayrollDeduction;

// Prepared for the future normalized payroll_deductions migration.
// This repository is not wired into the current payroll runtime flow yet.
public class PayrollDeductionDatabaseRepository implements PayrollDeductionRepository {

    @Override
    public PayrollDeduction findById(int payrollDeductionId) {
        String sql = """
            SELECT payroll_deduction_id, run_detail_id, deduction_id, deduction_amount, remarks
            FROM payroll_deductions
            WHERE payroll_deduction_id = ?
            """;
        List<PayrollDeduction> payrollDeductions = findMany(sql, payrollDeductionId);
        return payrollDeductions.isEmpty() ? null : payrollDeductions.get(0);
    }

    @Override
    public List<PayrollDeduction> findAll() {
        String sql = """
            SELECT payroll_deduction_id, run_detail_id, deduction_id, deduction_amount, remarks
            FROM payroll_deductions
            """;
        return findMany(sql);
    }

    @Override
    public List<PayrollDeduction> findByRunDetailId(int runDetailId) {
        String sql = """
            SELECT payroll_deduction_id, run_detail_id, deduction_id, deduction_amount, remarks
            FROM payroll_deductions
            WHERE run_detail_id = ?
            """;
        return findMany(sql, runDetailId);
    }

    @Override
    public void add(PayrollDeduction payrollDeduction) {
        if (payrollDeduction == null) {
            return;
        }

        PayrollDeduction existing = findByRunDetailAndDeduction(
                payrollDeduction.getRunDetailId(),
                payrollDeduction.getDeductionId());
        if (existing != null) {
            payrollDeduction.setPayrollDeductionId(existing.getPayrollDeductionId());
            update(payrollDeduction);
            return;
        }

        String sql = """
            INSERT INTO payroll_deductions (
                run_detail_id, deduction_id, deduction_amount, remarks
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollDeductionFields(stmt, payrollDeduction);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(PayrollDeduction payrollDeduction) {
        if (payrollDeduction == null) {
            return;
        }

        String sql = """
            UPDATE payroll_deductions SET
                run_detail_id = ?,
                deduction_id = ?,
                deduction_amount = ?,
                remarks = ?
            WHERE payroll_deduction_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                setPayrollDeductionFields(stmt, payrollDeduction);
                stmt.setInt(5, payrollDeduction.getPayrollDeductionId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int payrollDeductionId) {
        String sql = "DELETE FROM payroll_deductions WHERE payroll_deduction_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, payrollDeductionId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PayrollDeduction findByRunDetailAndDeduction(int runDetailId, int deductionId) {
        String sql = """
            SELECT payroll_deduction_id, run_detail_id, deduction_id, deduction_amount, remarks
            FROM payroll_deductions
            WHERE run_detail_id = ?
            AND deduction_id = ?
            """;
        List<PayrollDeduction> payrollDeductions = findMany(sql, runDetailId, deductionId);
        return payrollDeductions.isEmpty() ? null : payrollDeductions.get(0);
    }

    private List<PayrollDeduction> findMany(String sql, int... parameters) {
        List<PayrollDeduction> payrollDeductions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return payrollDeductions;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setInt(i + 1, parameters[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        payrollDeductions.add(new PayrollDeduction(
                                rs.getInt("payroll_deduction_id"),
                                rs.getInt("run_detail_id"),
                                rs.getInt("deduction_id"),
                                rs.getDouble("deduction_amount"),
                                rs.getString("remarks")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return payrollDeductions;
    }

    private void setPayrollDeductionFields(PreparedStatement stmt, PayrollDeduction payrollDeduction)
            throws SQLException {
        stmt.setInt(1, payrollDeduction.getRunDetailId());
        stmt.setInt(2, payrollDeduction.getDeductionId());
        stmt.setDouble(3, payrollDeduction.getDeductionAmount());
        stmt.setString(4, payrollDeduction.getRemarks());
    }
}
