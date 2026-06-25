package com.mycompany.oop.service;

import com.mycompany.oop.DatabaseConnection;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class ReportService {

    private static final String LOGO_RESOURCE = "/images/motorph_logo.png";

    public void generatePayslip(int employeeId, String outputPath) {
        generatePayslip(employeeId, null, outputPath);
    }

    public void generatePayslip(int employeeId, String cutoffPeriod, String outputPath) {
        try (Connection conn = DatabaseConnection.connect()) {

            InputStream reportStream = getClass()
                    .getResourceAsStream("/reports/payslip.jrxml");

            if (reportStream == null) {
                throw new RuntimeException("Payslip report template not found.");
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("EMPLOYEE_ID", employeeId);
            parameters.put("CUTOFF_PERIOD", cutoffPeriod == null || cutoffPeriod.isBlank() ? null : cutoffPeriod.trim());
            addLogoParameter(parameters);

            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    conn
            );

            JasperExportManager.exportReportToPdfFile(print, outputPath);

            System.out.println("Payslip generated at: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate payslip report.", e);
        }
    }

    public void generateTimecard(int employeeId, LocalDate startDate, LocalDate endDate,
            String periodLabel, String outputPath) {

        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("A valid timecard date range is required.");
        }

        try (Connection conn = DatabaseConnection.connect()) {

            InputStream reportStream = getClass()
                    .getResourceAsStream("/reports/timecard.jrxml");

            if (reportStream == null) {
                throw new RuntimeException("Timecard report template not found.");
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("EMPLOYEE_ID", employeeId);
            parameters.put("START_DATE", Date.valueOf(startDate));
            parameters.put("END_DATE", Date.valueOf(endDate));
            parameters.put("PERIOD_LABEL", periodLabel == null || periodLabel.isBlank()
                    ? startDate + " to " + endDate
                    : periodLabel.trim());
            addLogoParameter(parameters);

            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    conn
            );

            JasperExportManager.exportReportToPdfFile(print, outputPath);

            System.out.println("Timecard generated at: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate timecard report.", e);
        }
    }

    public void generatePayrollSummary(String cutoffPeriod, LocalDate periodStart, LocalDate periodEnd,
            String outputPath) {

        if (cutoffPeriod == null || cutoffPeriod.isBlank()) {
            throw new IllegalArgumentException("A payroll cutoff period is required.");
        }
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("A valid payroll period date range is required.");
        }

        try (Connection conn = DatabaseConnection.connect()) {

            InputStream reportStream = getClass()
                    .getResourceAsStream("/reports/payroll_summary.jrxml");

            if (reportStream == null) {
                throw new RuntimeException("Payroll summary report template not found.");
            }

            JasperReport report = JasperCompileManager.compileReport(reportStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("CUTOFF_PERIOD", cutoffPeriod.trim());
            parameters.put("PERIOD_START", Date.valueOf(periodStart));
            parameters.put("PERIOD_END", Date.valueOf(periodEnd));
            addLogoParameter(parameters);

            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parameters,
                    conn
            );

            JasperExportManager.exportReportToPdfFile(print, outputPath);

            System.out.println("Payroll summary generated at: " + outputPath);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate payroll summary report.", e);
        }
    }

    private void addLogoParameter(Map<String, Object> parameters) {
        URL logoUrl = getClass().getResource(LOGO_RESOURCE);
        parameters.put("LOGO_URL", logoUrl);
    }
}
