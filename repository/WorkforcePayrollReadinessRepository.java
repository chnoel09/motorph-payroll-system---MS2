package com.mycompany.oop.repository;

import com.mycompany.oop.model.WorkforcePayrollReadiness;

import java.util.List;

public interface WorkforcePayrollReadinessRepository {

    boolean isAvailable();

    WorkforcePayrollReadiness findById(int readinessId);

    WorkforcePayrollReadiness findByPeriodAndEmployee(int payrollPeriodId, int employeeId);

    List<WorkforcePayrollReadiness> findByPayrollPeriodId(int payrollPeriodId);

    List<WorkforcePayrollReadiness> findBySupervisor(int payrollPeriodId, int supervisorEmployeeId);

    void upsertDerivedState(WorkforcePayrollReadiness readiness);

    void update(WorkforcePayrollReadiness readiness);
}
