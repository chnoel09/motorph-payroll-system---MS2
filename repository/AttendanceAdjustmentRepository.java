package com.mycompany.oop.repository;

import com.mycompany.oop.model.AttendanceAdjustment;

import java.util.List;

public interface AttendanceAdjustmentRepository {

    boolean isAvailable();

    AttendanceAdjustment findById(int adjustmentId);

    List<AttendanceAdjustment> findAll();

    List<AttendanceAdjustment> findByEmployeeId(int employeeId);

    void addAttendanceAdjustment(AttendanceAdjustment adjustment);

    void updateAttendanceAdjustment(AttendanceAdjustment adjustment);
}
