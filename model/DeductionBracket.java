package com.mycompany.oop.model;

import java.time.LocalDate;

// Prepared for the future normalized deduction_brackets migration.
public class DeductionBracket {

    private int deductionBracketId;
    private int deductionId;
    private double salaryMin;
    private double salaryMax;
    private double employeeShare;
    private double employerShare;
    private double rate;
    private LocalDate effectiveDate;

    public DeductionBracket() {
    }

    public DeductionBracket(int deductionBracketId, int deductionId, double salaryMin,
            double salaryMax, double employeeShare, double employerShare, double rate,
            LocalDate effectiveDate) {
        this.deductionBracketId = deductionBracketId;
        this.deductionId = deductionId;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.employeeShare = employeeShare;
        this.employerShare = employerShare;
        this.rate = rate;
        this.effectiveDate = effectiveDate;
    }

    public int getDeductionBracketId() {
        return deductionBracketId;
    }

    public void setDeductionBracketId(int deductionBracketId) {
        this.deductionBracketId = deductionBracketId;
    }

    public int getDeductionId() {
        return deductionId;
    }

    public void setDeductionId(int deductionId) {
        this.deductionId = deductionId;
    }

    public double getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(double salaryMin) {
        this.salaryMin = salaryMin;
    }

    public double getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(double salaryMax) {
        this.salaryMax = salaryMax;
    }

    public double getEmployeeShare() {
        return employeeShare;
    }

    public void setEmployeeShare(double employeeShare) {
        this.employeeShare = employeeShare;
    }

    public double getEmployerShare() {
        return employerShare;
    }

    public void setEmployerShare(double employerShare) {
        this.employerShare = employerShare;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
