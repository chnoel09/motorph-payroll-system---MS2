package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.PayrollRunDetail;

// Contract prepared for future normalized payroll run detail management.
public interface PayrollRunDetailRepository {

    PayrollRunDetail findById(int runDetailId);

    List<PayrollRunDetail> findAll();

    List<PayrollRunDetail> findByRunId(int runId);

    List<PayrollRunDetail> findByEmployeeId(int employeeId);

    void add(PayrollRunDetail detail);

    int addAndReturnId(PayrollRunDetail detail);

    void update(PayrollRunDetail detail);

    void delete(int runDetailId);
}
