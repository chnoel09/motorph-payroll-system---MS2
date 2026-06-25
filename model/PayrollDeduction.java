package com.mycompany.oop.model;

// Prepared for the future normalized payroll_deductions migration.
public class PayrollDeduction {

    private int payrollDeductionId;
    private int runDetailId;
    private int deductionId;
    private double deductionAmount;
    private String remarks;

    public PayrollDeduction() {
    }

    public PayrollDeduction(int payrollDeductionId, int runDetailId, int deductionId,
            double deductionAmount, String remarks) {
        this.payrollDeductionId = payrollDeductionId;
        this.runDetailId = runDetailId;
        this.deductionId = deductionId;
        this.deductionAmount = deductionAmount;
        this.remarks = remarks;
    }

    public int getPayrollDeductionId() {
        return payrollDeductionId;
    }

    public void setPayrollDeductionId(int payrollDeductionId) {
        this.payrollDeductionId = payrollDeductionId;
    }

    public int getRunDetailId() {
        return runDetailId;
    }

    public void setRunDetailId(int runDetailId) {
        this.runDetailId = runDetailId;
    }

    public int getDeductionId() {
        return deductionId;
    }

    public void setDeductionId(int deductionId) {
        this.deductionId = deductionId;
    }

    public double getDeductionAmount() {
        return deductionAmount;
    }

    public void setDeductionAmount(double deductionAmount) {
        this.deductionAmount = deductionAmount;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
