/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.service;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.DashboardSummary;
import com.mycompany.oop.model.TodayAttendanceSummary;

import java.time.LocalDate;
import java.util.List;

public class DashboardService {

    private EmployeeService employeeService;
    private LeaveService leaveService;
    private PayrollProcessor processor;
    private AttendanceService attendanceService;
    private AttendanceAwarenessService attendanceAwarenessService;

    public DashboardService() {
        this.employeeService = new EmployeeService();
        this.leaveService = new LeaveService();
        this.processor = new PayrollProcessor();
        this.attendanceService = new AttendanceService();
        this.attendanceAwarenessService = new AttendanceAwarenessService();
    }

    public DashboardSummary generateSummary() {

        List<Employee> employees = employeeService.getAllEmployees();
        List<Leave> leaves = leaveService.getAllLeaves();

        int totalEmployees = employees.size();

        int activeEmployees = (int) employees.stream()
                .filter(e -> e != null && !"inactive".equals(safe(e.getEmploymentStatus()).toLowerCase()))
                .count();

        int pendingLeaves = (int) leaves.stream()
                .filter(l -> l != null && "pending".equals(safe(l.getStatus()).toLowerCase()))
                .count();

        int adminCount = 0;
        int hrCount = 0;
        int financeCount = 0;
        int employeeCount = 0;
        int itCount = 0;

        double totalGross = 0;
        double totalNet = 0;
        double totalDeductions = 0;
        double totalAllowance = 0;

        for (Employee e : employees) {
            if (e == null) {
                continue;
            }

            switch (safe(e.getRole()).toLowerCase()) {
                case "admin": adminCount++; break;
                case "hr": hrCount++; break;
                case "finance": financeCount++; break;
                case "employee": employeeCount++; break;
                case "it": itCount++; break;
            }

            totalGross += e.computeGrossSalary();
            totalNet += e.computeNetSalary();
            totalDeductions += e.computeDeductions();
            totalAllowance += e.getAllowance();
        }

        return new DashboardSummary(
                totalEmployees,
                activeEmployees,
                pendingLeaves,
                adminCount,
                hrCount,
                financeCount,
                employeeCount,
                itCount,
                totalGross,
                totalNet,
                totalDeductions,
                totalAllowance
        );
    } 

    public TodayAttendanceSummary getTodayAttendanceSummary(Employee employee) {
        LocalDate today = LocalDate.now();
        if (employee == null || employee.getEmployeeId() <= 0) {
            return new TodayAttendanceSummary(today, "Unavailable",
                    "No employee attendance record is available.", "", "", 0.0);
        }

        AttendanceRecord record = attendanceService.getAttendanceForDate(employee.getEmployeeId(), today);
        AttendanceAwareness awareness = attendanceAwarenessService.getDailyAwareness(employee.getEmployeeId(), today);

        String status = awareness == null ? "Unavailable" : awareness.getStatus();
        String message = awareness == null ? "No attendance awareness available." : awareness.getMessage();
        String timeIn = record == null ? "" : safe(record.getTimeIn());
        String timeOut = record == null ? "" : safe(record.getTimeOut());
        double hoursWorked = record == null ? 0.0 : attendanceService.getHoursWorked(record);

        return new TodayAttendanceSummary(today, status, message, timeIn, timeOut, hoursWorked);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
