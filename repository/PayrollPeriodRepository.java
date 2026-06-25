package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.PayrollPeriod;

// Contract prepared for future normalized payroll period management.
public interface PayrollPeriodRepository {

    PayrollPeriod findById(int periodId);

    PayrollPeriod findByCutoffAndRange(String cutoffPeriod, String periodStart, String periodEnd);

    List<PayrollPeriod> findAll();

    void add(PayrollPeriod period);

    int addAndReturnId(PayrollPeriod period);

    void update(PayrollPeriod period);

    void delete(int periodId);
}
