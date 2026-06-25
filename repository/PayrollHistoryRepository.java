package com.mycompany.oop.repository;

import com.mycompany.oop.model.PayrollHistoryRecord;
import java.util.List;

public interface PayrollHistoryRepository {

    void savePayrollRecord(PayrollHistoryRecord record);

    List<PayrollHistoryRecord> findAll();

    List<PayrollHistoryRecord> findByCutoff(String cutoffPeriod);

    List<PayrollHistoryRecord> findByEmployeeId(int employeeId);

    boolean existsByCutoff(String cutoffPeriod);

    void deleteByCutoff(String cutoffPeriod);
}
