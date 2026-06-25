/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.oop.repository;

import com.mycompany.oop.model.AttendanceRecord;
import java.util.List;

public interface AttendanceRepository {

    void saveAttendance(AttendanceRecord record);

    void updateAttendance(AttendanceRecord record);

    List<AttendanceRecord> findAll();

    List<AttendanceRecord> findByEmployeeId(int employeeId);

    AttendanceRecord findByEmployeeAndDate(int employeeId, String date);
}