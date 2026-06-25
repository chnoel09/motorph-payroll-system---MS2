/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.model;

public class PayrollSummary {

    private double totalGross;
    private double totalDeductions;
    private double totalNet;
    private int employeeCount;

    private double totalSSS;
    private double totalPhilhealth;
    private double totalPagibig;
    private double totalTax;

    public PayrollSummary(
            double totalGross,
            double totalDeductions,
            double totalNet,
            int employeeCount,
            double totalSSS,
            double totalPhilhealth,
            double totalPagibig,
            double totalTax
    ) {
        this.totalGross = totalGross;
        this.totalDeductions = totalDeductions;
        this.totalNet = totalNet;
        this.employeeCount = employeeCount;
        this.totalSSS = totalSSS;
        this.totalPhilhealth = totalPhilhealth;
        this.totalPagibig = totalPagibig;
        this.totalTax = totalTax;
    }

    public double getTotalGross() { return totalGross; }
    public double getTotalDeductions() { return totalDeductions; }
    public double getTotalNet() { return totalNet; }
    public int getEmployeeCount() { return employeeCount; }

    public double getTotalSSS() { return totalSSS; }
    public double getTotalPhilhealth() { return totalPhilhealth; }
    public double getTotalPagibig() { return totalPagibig; }
    public double getTotalTax() { return totalTax; }
}
