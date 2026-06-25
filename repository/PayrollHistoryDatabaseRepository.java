package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.PayrollHistoryRecord;

public class PayrollHistoryDatabaseRepository implements PayrollHistoryRepository {

    @Override
    public void savePayrollRecord(PayrollHistoryRecord record) {
        String sql = """
            INSERT INTO payroll_history (
                run_detail_id,
                employee_id,
                cutoff_period,
                period_start,
                period_end,
                hours_worked,
                basic_component,
                allowance_component,
                gross,
                sss,
                philhealth,
                pagibig,
                tax,
                total_deductions,
                net
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                run_detail_id = VALUES(run_detail_id),
                period_start = VALUES(period_start),
                period_end = VALUES(period_end),
                hours_worked = VALUES(hours_worked),
                basic_component = VALUES(basic_component),
                allowance_component = VALUES(allowance_component),
                gross = VALUES(gross),
                sss = VALUES(sss),
                philhealth = VALUES(philhealth),
                pagibig = VALUES(pagibig),
                tax = VALUES(tax),
                total_deductions = VALUES(total_deductions),
                net = VALUES(net)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableInteger(stmt, 1, record.getRunDetailId());
            stmt.setInt(2, record.getEmployeeId());
            stmt.setString(3, record.getCutoffPeriod());
            setNullableDate(stmt, 4, record.getPeriodStart());
            setNullableDate(stmt, 5, record.getPeriodEnd());
            stmt.setDouble(6, record.getHoursWorked());
            stmt.setDouble(7, record.getBasicComponent());
            stmt.setDouble(8, record.getAllowanceComponent());
            stmt.setDouble(9, record.getGross());
            stmt.setDouble(10, record.getSss());
            stmt.setDouble(11, record.getPhilhealth());
            stmt.setDouble(12, record.getPagibig());
            stmt.setDouble(13, record.getTax());
            stmt.setDouble(14, record.getTotalDeductions());
            stmt.setDouble(15, record.getNet());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PayrollHistoryRecord> findAll() {
        List<PayrollHistoryRecord> records = new ArrayList<>();

        String sql = """
            SELECT run_detail_id, employee_id, cutoff_period, period_start, period_end,
                   hours_worked, basic_component, allowance_component,
                   gross, sss, philhealth, pagibig, tax, total_deductions, net
            FROM payroll_history
            ORDER BY cutoff_period, employee_id
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                records.add(mapRowToPayrollHistoryRecord(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public List<PayrollHistoryRecord> findByEmployeeId(int employeeId) {
        List<PayrollHistoryRecord> records = new ArrayList<>();

        String sql = """
            SELECT run_detail_id, employee_id, cutoff_period, period_start, period_end,
                   hours_worked, basic_component, allowance_component,
                   gross, sss, philhealth, pagibig, tax, total_deductions, net
            FROM payroll_history
            WHERE employee_id = ?
            ORDER BY cutoff_period
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRowToPayrollHistoryRecord(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public List<PayrollHistoryRecord> findByCutoff(String cutoffPeriod) {
        List<PayrollHistoryRecord> records = new ArrayList<>();

        if (cutoffPeriod == null || cutoffPeriod.trim().isEmpty()) {
            return records;
        }

        String sql = """
            SELECT run_detail_id, employee_id, cutoff_period, period_start, period_end,
                   hours_worked, basic_component, allowance_component,
                   gross, sss, philhealth, pagibig, tax, total_deductions, net
            FROM payroll_history
            WHERE LOWER(cutoff_period) = LOWER(?)
            ORDER BY employee_id
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cutoffPeriod.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRowToPayrollHistoryRecord(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public boolean existsByCutoff(String cutoffPeriod) {
        String sql = """
            SELECT COUNT(*) AS record_count
            FROM payroll_history
            WHERE LOWER(cutoff_period) = LOWER(?)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cutoffPeriod);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("record_count") > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void deleteByCutoff(String cutoffPeriod) {
        String sql = """
            DELETE FROM payroll_history
            WHERE LOWER(cutoff_period) = LOWER(?)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cutoffPeriod);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PayrollHistoryRecord mapRowToPayrollHistoryRecord(ResultSet rs) throws SQLException {
        return new PayrollHistoryRecord(
                getNullableInteger(rs, "run_detail_id"),
                rs.getInt("employee_id"),
                rs.getString("cutoff_period"),
                getDateString(rs, "period_start"),
                getDateString(rs, "period_end"),
                rs.getDouble("hours_worked"),
                rs.getDouble("basic_component"),
                rs.getDouble("allowance_component"),
                rs.getDouble("gross"),
                rs.getDouble("sss"),
                rs.getDouble("philhealth"),
                rs.getDouble("pagibig"),
                rs.getDouble("tax"),
                rs.getDouble("total_deductions"),
                rs.getDouble("net")
        );
    }

    private void setNullableDate(PreparedStatement stmt, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            stmt.setNull(index, Types.DATE);
            return;
        }

        stmt.setDate(index, Date.valueOf(value.trim()));
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }
        stmt.setInt(index, value);
    }

    private String getDateString(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toString();
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
