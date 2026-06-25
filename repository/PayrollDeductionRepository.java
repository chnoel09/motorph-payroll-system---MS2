package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.PayrollDeduction;

// Contract prepared for future normalized payroll deduction management.
public interface PayrollDeductionRepository {

    PayrollDeduction findById(int payrollDeductionId);

    List<PayrollDeduction> findAll();

    List<PayrollDeduction> findByRunDetailId(int runDetailId);

    void add(PayrollDeduction payrollDeduction);

    void update(PayrollDeduction payrollDeduction);

    void delete(int payrollDeductionId);
}
