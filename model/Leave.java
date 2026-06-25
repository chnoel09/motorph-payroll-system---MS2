/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.model;

/*
 * Leave represents a leave request made by an employee.
 * This is a MODEL class.
 * Its responsibility is to store leave data only.
 * No business logic should be placed here.
 */

public class Leave {

    private int leaveId;
    private int employeeId;
    private String leaveType;
    private String startDate;
    private String endDate;
    private String reason;
    private String status;

    public Leave(int leaveId,
                 int employeeId,
                 String leaveType,
                 String startDate,
                 String endDate,
                 String reason,
                 String status) {

        this.leaveId = leaveId;
        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }

    public int getLeaveId() { return leaveId; }
    public int getEmployeeId() { return employeeId; }
    public String getLeaveType() { return leaveType; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
    }
}

