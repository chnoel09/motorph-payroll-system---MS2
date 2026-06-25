package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.Payslip;

// Contract prepared for future normalized payslip management.
public interface PayslipRepository {

    Payslip findById(int payslipId);

    List<Payslip> findAll();

    List<Payslip> findByEmployeeId(int employeeId);

    void add(Payslip payslip);

    void update(Payslip payslip);

    void delete(int payslipId);
}
