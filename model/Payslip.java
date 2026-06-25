package com.mycompany.oop.model;

import java.time.LocalDateTime;

// Prepared for the future normalized payslips migration.
public class Payslip {

    private int payslipId;
    private int runDetailId;
    private int employeeId;
    private LocalDateTime generatedAt;
    private String filePath;

    public Payslip() {
    }

    public Payslip(int payslipId, int runDetailId, int employeeId,
            LocalDateTime generatedAt, String filePath) {
        this.payslipId = payslipId;
        this.runDetailId = runDetailId;
        this.employeeId = employeeId;
        this.generatedAt = generatedAt;
        this.filePath = filePath;
    }

    public int getPayslipId() {
        return payslipId;
    }

    public void setPayslipId(int payslipId) {
        this.payslipId = payslipId;
    }

    public int getRunDetailId() {
        return runDetailId;
    }

    public void setRunDetailId(int runDetailId) {
        this.runDetailId = runDetailId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
