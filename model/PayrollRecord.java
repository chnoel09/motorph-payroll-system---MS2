/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.model;

public class PayrollRecord {

    private Employee employee;
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

    public PayrollRecord(Employee employee, double hoursWorked) {
        this.employee = employee;
        this.hoursWorked = hoursWorked;
    }

    public Employee getEmployee() { return employee; }
    public double getHoursWorked() { return hoursWorked; }

    public double getGross() { return gross; }
    public void setGross(double gross) { this.gross = gross; }

    public double getSss() { return sss; }
    public void setSss(double sss) { this.sss = sss; }

    public double getPhilhealth() { return philhealth; }
    public void setPhilhealth(double philhealth) { this.philhealth = philhealth; }

    public double getPagibig() { return pagibig; }
    public void setPagibig(double pagibig) { this.pagibig = pagibig; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(double totalDeductions) { this.totalDeductions = totalDeductions; }

    public double getNet() { return net; }
    public void setNet(double net) { this.net = net; }
    
    public double getBasicComponent() {
        return basicComponent; }   
    
    public void setBasicComponent(double basicComponent) {
        this.basicComponent = basicComponent; }

    public double getAllowanceComponent() {
        return allowanceComponent; }

    public void setAllowanceComponent(double allowanceComponent) {
        this.allowanceComponent = allowanceComponent; }
}