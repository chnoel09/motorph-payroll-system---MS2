/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.model;

public class PayrollHistoryRecord {

    private int employeeId;
    private Integer runDetailId;
    private String cutoffPeriod;
    private String periodStart;
    private String periodEnd;
    private double hoursWorked;

    private double gross;
    private double sss;
    private double philhealth;
    private double pagibig;
    private double tax;
    private double totalDeductions;
    private double net;   
    private double basicComponent;
    private double allowanceComponent;

    public PayrollHistoryRecord(
            Integer runDetailId,
            int employeeId,
            String cutoffPeriod,
            String periodStart,
            String periodEnd,
            double hoursWorked,
            double basicComponent,
            double allowanceComponent,
            double gross,
            double sss,
            double philhealth,
            double pagibig,
            double tax,
            double totalDeductions,
            double net
    ) {
        this.runDetailId = runDetailId;
        this.employeeId = employeeId;
        this.cutoffPeriod = cutoffPeriod;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.hoursWorked = hoursWorked;
        this.basicComponent = basicComponent;
        this.allowanceComponent = allowanceComponent;
        this.gross = gross;
        this.sss = sss;
        this.philhealth = philhealth;
        this.pagibig = pagibig;
        this.tax = tax;
        this.totalDeductions = totalDeductions;
        this.net = net;
    }

    public PayrollHistoryRecord(
            int employeeId,
            String cutoffPeriod,
            String periodStart,
            String periodEnd,
            double hoursWorked,
            double basicComponent,
            double allowanceComponent,
            double gross,
            double sss,
            double philhealth,
            double pagibig,
            double tax,
            double totalDeductions,
            double net
    ) {
        this(
                null,
                employeeId,
                cutoffPeriod,
                periodStart,
                periodEnd,
                hoursWorked,
                basicComponent,
                allowanceComponent,
                gross,
                sss,
                philhealth,
                pagibig,
                tax,
                totalDeductions,
                net
        );
    }

    public PayrollHistoryRecord(
            int employeeId,
            String cutoffPeriod,
            double hoursWorked,
            double basicComponent,
            double allowanceComponent,
            double gross,
            double sss,
            double philhealth,
            double pagibig,
            double tax,
            double totalDeductions,
            double net
    ) {
        this(
                null,
                employeeId,
                cutoffPeriod,
                null,
                null,
                hoursWorked,
                basicComponent,
                allowanceComponent,
                gross,
                sss,
                philhealth,
                pagibig,
                tax,
                totalDeductions,
                net
        );
    }

    public PayrollHistoryRecord(
            int employeeId,
            String cutoffPeriod,
            double basicComponent,
            double allowanceComponent,
            double gross,
            double sss,
            double philhealth,
            double pagibig,
            double tax,
            double totalDeductions,
            double net
    ) {
        this(
                employeeId,
                cutoffPeriod,
                0.0,
                basicComponent,
                allowanceComponent,
                gross,
                sss,
                philhealth,
                pagibig,
                tax,
                totalDeductions,
                net
        );
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public Integer getRunDetailId() {
        return runDetailId;
    }

    public void setRunDetailId(Integer runDetailId) {
        this.runDetailId = runDetailId;
    }

    public String getCutoffPeriod() {
        return cutoffPeriod;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public double getGross() {
        return gross;
    }

    public double getSss() {
        return sss;
    }

    public double getPhilhealth() {
        return philhealth;
    }

    public double getPagibig() {
        return pagibig;
    }

    public double getTax() {
        return tax;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNet() {
        return net;
    }
    
    public double getBasicComponent() { 
        return basicComponent; 
    
    }
    public double getAllowanceComponent() { 
        return allowanceComponent; 
    }
    
}
