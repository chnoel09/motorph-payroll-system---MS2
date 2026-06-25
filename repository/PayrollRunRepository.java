package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.PayrollRun;

// Contract prepared for future normalized payroll run management.
public interface PayrollRunRepository {

    PayrollRun findById(int runId);

    PayrollRun findLatestByPeriodId(int periodId);

    List<PayrollRun> findAll();

    void add(PayrollRun run);

    int addAndReturnId(PayrollRun run);

    void update(PayrollRun run);

    void delete(int runId);
}
