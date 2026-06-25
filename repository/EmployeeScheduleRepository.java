package com.mycompany.oop.repository;

import java.time.LocalDate;
import java.util.List;

import com.mycompany.oop.model.EmployeeSchedule;

// Contract prepared for future normalized employee schedule assignments.
public interface EmployeeScheduleRepository {

    EmployeeSchedule findById(int scheduleId);

    List<EmployeeSchedule> findAll();

    List<EmployeeSchedule> findByEmployeeId(int employeeId);

    List<EmployeeSchedule> findByDateRange(int employeeId, LocalDate startDate, LocalDate endDate);

    void add(EmployeeSchedule schedule);

    void update(EmployeeSchedule schedule);

    void delete(int scheduleId);
}
