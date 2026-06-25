package com.mycompany.oop.service;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollRecord;

public class PayrollProcessor {

    public PayrollRecord processPayroll(Employee emp, double hoursWorked) {

        PayrollRecord record = new PayrollRecord(emp, hoursWorked);

        // ================= GROSS COMPUTATION =================

        if (hoursWorked <= 0) {
            record.setBasicComponent(0);
            record.setAllowanceComponent(0);
            record.setGross(0);
            record.setSss(0);
            record.setPhilhealth(0);
            record.setPagibig(0);
            record.setTax(0);
            record.setTotalDeductions(0);
            record.setNet(0);
            return record;
        }

        double basicHalf = emp.getHourlyRate() * hoursWorked;
        double allowanceHalf = emp.getAllowance() / 2;
        double gross = basicHalf + allowanceHalf;

        record.setBasicComponent(basicHalf);
        record.setAllowanceComponent(allowanceHalf);
        record.setGross(gross);

        // ================= STATUTORY DEDUCTIONS =================

        double monthlyEquivalent = gross * 2;
        double cutoffSss = computeSemiMonthlySss(monthlyEquivalent);
        double cutoffPhilhealth = computeSemiMonthlyPhilhealth(monthlyEquivalent);
        double cutoffPagibig = computeSemiMonthlyPagibig(monthlyEquivalent);

        // ================= WITHHOLDING TAX =================

        double taxableIncome = Math.max(0, gross - cutoffSss - cutoffPhilhealth - cutoffPagibig);
        double cutoffTax = computeSemiMonthlyWithholdingTax(taxableIncome);

        double totalDeductions =
                cutoffSss +
                cutoffPhilhealth +
                cutoffPagibig +
                cutoffTax;

        record.setSss(cutoffSss);
        record.setPhilhealth(cutoffPhilhealth);
        record.setPagibig(cutoffPagibig);
        record.setTax(cutoffTax);
        record.setTotalDeductions(totalDeductions);

        record.setNet(gross - totalDeductions);

        return record;
    }

    private double computeSemiMonthlySss(double monthlyEquivalent) {
        double monthlyContribution;
        if (monthlyEquivalent < 3250) monthlyContribution = 135;
        else if (monthlyEquivalent <= 3749.99) monthlyContribution = 157.5;
        else if (monthlyEquivalent <= 4249.99) monthlyContribution = 180;
        else if (monthlyEquivalent <= 4749.99) monthlyContribution = 202.5;
        else if (monthlyEquivalent <= 5249.99) monthlyContribution = 225;
        else if (monthlyEquivalent <= 5749.99) monthlyContribution = 247.5;
        else if (monthlyEquivalent <= 6249.99) monthlyContribution = 270;
        else if (monthlyEquivalent <= 6749.99) monthlyContribution = 292.5;
        else if (monthlyEquivalent <= 7249.99) monthlyContribution = 315;
        else if (monthlyEquivalent <= 7749.99) monthlyContribution = 337.5;
        else if (monthlyEquivalent <= 8249.99) monthlyContribution = 360;
        else if (monthlyEquivalent <= 8749.99) monthlyContribution = 382.5;
        else if (monthlyEquivalent <= 9249.99) monthlyContribution = 405;
        else if (monthlyEquivalent <= 9749.99) monthlyContribution = 427.5;
        else if (monthlyEquivalent <= 10249.99) monthlyContribution = 450;
        else if (monthlyEquivalent <= 10749.99) monthlyContribution = 472.5;
        else if (monthlyEquivalent <= 11249.99) monthlyContribution = 495;
        else if (monthlyEquivalent <= 11749.99) monthlyContribution = 517.5;
        else if (monthlyEquivalent <= 12249.99) monthlyContribution = 540;
        else if (monthlyEquivalent <= 12749.99) monthlyContribution = 562.5;
        else if (monthlyEquivalent <= 13249.99) monthlyContribution = 585;
        else if (monthlyEquivalent <= 13749.99) monthlyContribution = 607.5;
        else if (monthlyEquivalent <= 14249.99) monthlyContribution = 630;
        else if (monthlyEquivalent <= 14749.99) monthlyContribution = 652.5;
        else if (monthlyEquivalent <= 15249.99) monthlyContribution = 675;
        else if (monthlyEquivalent <= 15749.99) monthlyContribution = 697.5;
        else if (monthlyEquivalent <= 16249.99) monthlyContribution = 720;
        else if (monthlyEquivalent <= 16749.99) monthlyContribution = 742.5;
        else if (monthlyEquivalent <= 17249.99) monthlyContribution = 765;
        else if (monthlyEquivalent <= 17749.99) monthlyContribution = 787.5;
        else if (monthlyEquivalent <= 18249.99) monthlyContribution = 810;
        else if (monthlyEquivalent <= 18749.99) monthlyContribution = 832.5;
        else if (monthlyEquivalent <= 19249.99) monthlyContribution = 855;
        else if (monthlyEquivalent <= 19749.99) monthlyContribution = 877.5;
        else if (monthlyEquivalent <= 20249.99) monthlyContribution = 900;
        else if (monthlyEquivalent <= 20749.99) monthlyContribution = 922.5;
        else if (monthlyEquivalent <= 21249.99) monthlyContribution = 945;
        else if (monthlyEquivalent <= 21749.99) monthlyContribution = 967.5;
        else if (monthlyEquivalent <= 22249.99) monthlyContribution = 990;
        else if (monthlyEquivalent <= 22749.99) monthlyContribution = 1012.5;
        else if (monthlyEquivalent <= 23249.99) monthlyContribution = 1035;
        else if (monthlyEquivalent <= 23749.99) monthlyContribution = 1057.5;
        else if (monthlyEquivalent <= 24249.99) monthlyContribution = 1080;
        else if (monthlyEquivalent <= 24749.99) monthlyContribution = 1102.5;
        else monthlyContribution = 1125;

        return monthlyContribution / 2;
    }

    private double computeSemiMonthlyPhilhealth(double monthlyEquivalent) {
        double monthlyPremium = Math.max(300,
                Math.min(monthlyEquivalent * 0.03, 1800));
        double employeeMonthlyShare = monthlyPremium / 2;

        return employeeMonthlyShare / 2;
    }

    private double computeSemiMonthlyPagibig(double monthlyEquivalent) {
        double pagibig = monthlyEquivalent <= 1500
                ? monthlyEquivalent * 0.01
                : monthlyEquivalent * 0.02;

        return Math.min(pagibig, 100) / 2;
    }

    double computeSemiMonthlyWithholdingTax(double taxableIncome) {
        if (taxableIncome <= 10416.50) {
            return 0;
        }

        if (taxableIncome <= 16666.50) {
            return (taxableIncome - 10416.50) * 0.15;
        }

        if (taxableIncome <= 33333.00) {
            return 937.50 + (taxableIncome - 16666.50) * 0.20;
        }

        if (taxableIncome <= 83333.00) {
            return 4270.83 + (taxableIncome - 33333.00) * 0.25;
        }

        if (taxableIncome <= 333333.00) {
            return 16770.83 + (taxableIncome - 83333.00) * 0.30;
        }

        return 91770.83 + (taxableIncome - 333333.00) * 0.35;
    }
    
    public PayrollRecord processPayroll(Employee emp) {
        double defaultHours = 80;
        return processPayroll(emp, defaultHours);
    }

}
