package com.mycompany.oop.model;

// Prepared for the future normalized payroll_run_details migration.
public class PayrollRunDetail {

    private int runDetailId;
    private int runId;
    private int employeeId;
    private double hoursWorked;
    private double overtimeHours;
    private double basicComponent;
    private double allowanceComponent;

    public PayrollRunDetail() {
    }

    public PayrollRunDetail(int runDetailId, int runId, int employeeId, double hoursWorked,
            double overtimeHours, double basicComponent, double allowanceComponent) {
        this.runDetailId = runDetailId;
        this.runId = runId;
        this.employeeId = employeeId;
        this.hoursWorked = hoursWorked;
        this.overtimeHours = overtimeHours;
        this.basicComponent = basicComponent;
        this.allowanceComponent = allowanceComponent;
    }

    public int getRunDetailId() {
        return runDetailId;
    }

    public void setRunDetailId(int runDetailId) {
        this.runDetailId = runDetailId;
    }

    public int getRunId() {
        return runId;
    }

    public void setRunId(int runId) {
        this.runId = runId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public double getBasicComponent() {
        return basicComponent;
    }

    public void setBasicComponent(double basicComponent) {
        this.basicComponent = basicComponent;
    }

    public double getAllowanceComponent() {
        return allowanceComponent;
    }

    public void setAllowanceComponent(double allowanceComponent) {
        this.allowanceComponent = allowanceComponent;
    }
}
