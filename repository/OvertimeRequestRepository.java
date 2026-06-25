package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.OvertimeRequest;

// Contract prepared for future normalized overtime request workflow.
public interface OvertimeRequestRepository {

    OvertimeRequest findById(int overtimeId);

    List<OvertimeRequest> findByEmployeeId(int employeeId);

    List<OvertimeRequest> findAll();

    void addOvertimeRequest(OvertimeRequest request);

    void updateOvertimeRequest(OvertimeRequest request);
}
