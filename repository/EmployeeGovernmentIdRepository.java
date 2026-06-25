package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.EmployeeGovernmentId;

// Contract prepared for future employee government ID normalization.
public interface EmployeeGovernmentIdRepository {

    List<EmployeeGovernmentId> findByEmployeeId(int employeeId);

    void addGovernmentId(EmployeeGovernmentId governmentId);

    void updateGovernmentId(EmployeeGovernmentId governmentId);

    void deleteGovernmentId(int employeeGovId);
}
