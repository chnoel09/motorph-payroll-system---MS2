package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.EmployeeShift;

// Contract prepared for future normalized employee shift scheduling.
public interface EmployeeShiftRepository {

    EmployeeShift findById(int shiftId);

    List<EmployeeShift> findAll();

    void add(EmployeeShift shift);

    void update(EmployeeShift shift);

    void delete(int shiftId);
}
