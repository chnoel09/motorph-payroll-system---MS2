/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.oop.repository;

import java.util.List;
import com.mycompany.oop.model.Leave;

public interface LeaveRepository {

    void addLeave(Leave leave);

    void updateLeave(Leave leave);

    Leave findLeave(int leaveId);

    List<Leave> getAllLeaves();

    List<Leave> getLeavesByEmployee(int employeeId);
}
