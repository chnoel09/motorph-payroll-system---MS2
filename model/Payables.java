package com.mycompany.oop.model;

/**
 * Defines payroll computations required by payable employee models.
 */
public interface Payables {

    double computeGrossSalary();

    double computeDeductions();

    double computeNetSalary();
}
