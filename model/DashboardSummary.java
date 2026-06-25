package com.mycompany.oop.model;

public class DashboardSummary {

    private int totalEmployees;
    private int activeEmployees;
    private int pendingLeaves;

    private int adminCount;
    private int hrCount;
    private int financeCount;
    private int employeeCount;
    private int itCount;

    private double totalGross;
    private double totalNet;
    private double totalDeductions;
    private double totalAllowance;

    public DashboardSummary(
            int totalEmployees,
            int activeEmployees,
            int pendingLeaves,
            int adminCount,
            int hrCount,
            int financeCount,
            int employeeCount,
            int itCount,
            double totalGross,
            double totalNet,
            double totalDeductions,
            double totalAllowance
    ) {
        this.totalEmployees = totalEmployees;
        this.activeEmployees = activeEmployees;
        this.pendingLeaves = pendingLeaves;
        this.adminCount = adminCount;
        this.hrCount = hrCount;
        this.financeCount = financeCount;
        this.employeeCount = employeeCount;
        this.itCount = itCount;
        this.totalGross = totalGross;
        this.totalNet = totalNet;
        this.totalDeductions = totalDeductions;
        this.totalAllowance = totalAllowance;
    }

    public int getTotalEmployees() { return totalEmployees; }
    public int getActiveEmployees() { return activeEmployees; }
    public int getPendingLeaves() { return pendingLeaves; }

    public int getAdminCount() { return adminCount; }
    public int getHrCount() { return hrCount; }
    public int getFinanceCount() { return financeCount; }
    public int getEmployeeCount() { return employeeCount; }
    public int getItCount() { return itCount; }

    public double getTotalGross() { return totalGross; }
    public double getTotalNet() { return totalNet; }
    public double getTotalDeductions() { return totalDeductions; }
    public double getTotalAllowance() { return totalAllowance; }
}
