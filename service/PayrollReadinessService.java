package com.mycompany.oop.service;

import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.model.PayrollReadinessIssue;
import com.mycompany.oop.model.PayrollReadinessReport;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Visibility layer only. This service does not change payroll computation or approval rules.
public class PayrollReadinessService {

    private final EmployeeService employeeService;
    private final AttendanceAwarenessService attendanceAwarenessService;
    private final LeaveService leaveService;
    private final ScheduleService scheduleService;
    private final AttendanceAdjustmentService attendanceAdjustmentService;

    public PayrollReadinessService() {
        this.employeeService = new EmployeeService();
        this.attendanceAwarenessService = new AttendanceAwarenessService();
        this.leaveService = new LeaveService();
        this.scheduleService = new ScheduleService();
        this.attendanceAdjustmentService = new AttendanceAdjustmentService();
    }

    public PayrollReadinessReport evaluateReadiness(LocalDate periodStart, LocalDate periodEnd) {
        PayrollReadinessReport report = new PayrollReadinessReport();
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            report.addIssue(new PayrollReadinessIssue(
                    0,
                    "Payroll Period",
                    "Invalid payroll period",
                    PayrollReadinessIssue.Severity.BLOCKED,
                    "Select a valid payroll date range."
            ));
            return report;
        }

        boolean scheduleDataAvailable = scheduleService.isSchedulingSchemaAvailable();
        List<Employee> employees = employeeService.getAllEmployees();
        for (Employee employee : employees) {
            addEmployeeRecordIssues(report, employee);
            addAttendanceIssues(report, employee, periodStart, periodEnd, scheduleDataAvailable);
            addPendingAdjustmentIssues(report, employee, periodStart, periodEnd);
            addPendingLeaveIssues(report, employee, periodStart, periodEnd);
        }

        return report;
    }

    private void addEmployeeRecordIssues(PayrollReadinessReport report, Employee employee) {
        if (employee == null) {
            return;
        }

        Set<String> missingFields = new HashSet<>();
        if (isBlank(employee.getFirstName())) missingFields.add("first name");
        if (isBlank(employee.getLastName())) missingFields.add("last name");
        if (isBlank(employee.getPosition())) missingFields.add("position");
        if (isBlank(employee.getEmploymentStatus())) missingFields.add("employment status");
        if (employee.getBasicSalary() < 0 || employee.getHourlyRate() < 0) missingFields.add("salary/rate");

        if (!missingFields.isEmpty()) {
            report.addIssue(new PayrollReadinessIssue(
                    employee.getEmployeeId(),
                    fullName(employee),
                    "Incomplete employee record: " + String.join(", ", missingFields),
                    PayrollReadinessIssue.Severity.BLOCKED,
                    "Complete the employee profile before payroll final review."
            ));
        }
    }

    private void addAttendanceIssues(PayrollReadinessReport report, Employee employee,
            LocalDate periodStart, LocalDate periodEnd, boolean scheduleDataAvailable) {
        if (employee == null) {
            return;
        }

        for (AttendanceAwareness awareness : attendanceAwarenessService.getEmployeeAwareness(
                employee.getEmployeeId(), periodStart, periodEnd)) {

            if (!awareness.requiresReview()) {
                continue;
            }

            String status = awareness.getStatus();
            if ("No Assigned Schedule".equals(status) && !scheduleDataAvailable) {
                continue;
            }

            PayrollReadinessIssue.Severity severity = isBlockedAttendanceStatus(status)
                    ? PayrollReadinessIssue.Severity.BLOCKED
                    : PayrollReadinessIssue.Severity.NEEDS_REVIEW;

            report.addIssue(new PayrollReadinessIssue(
                    employee.getEmployeeId(),
                    fullName(employee),
                    awareness.getDate() + ": " + status,
                    severity,
                    getAttendanceRecommendedAction(status)
            ));
        }
    }

    private void addPendingLeaveIssues(PayrollReadinessReport report, Employee employee,
            LocalDate periodStart, LocalDate periodEnd) {
        if (employee == null) {
            return;
        }

        for (Leave leave : leaveService.getLeavesByEmployee(employee.getEmployeeId())) {
            if (!"PENDING".equalsIgnoreCase(safe(leave.getStatus()))) {
                continue;
            }

            try {
                LocalDate leaveStart = LocalDate.parse(leave.getStartDate());
                LocalDate leaveEnd = LocalDate.parse(leave.getEndDate());
                if (!leaveEnd.isBefore(periodStart) && !leaveStart.isAfter(periodEnd)) {
                    report.addIssue(new PayrollReadinessIssue(
                            employee.getEmployeeId(),
                            fullName(employee),
                            "Pending leave overlaps payroll period",
                            PayrollReadinessIssue.Severity.NEEDS_REVIEW,
                            "Review leave status before processing payroll."
                    ));
                }
            } catch (Exception ignored) {
                report.addIssue(new PayrollReadinessIssue(
                        employee.getEmployeeId(),
                        fullName(employee),
                        "Leave date needs review",
                        PayrollReadinessIssue.Severity.NEEDS_REVIEW,
                        "Check leave request dates before payroll review."
                ));
            }
        }
    }

    private void addPendingAdjustmentIssues(PayrollReadinessReport report, Employee employee,
            LocalDate periodStart, LocalDate periodEnd) {
        if (employee == null || !attendanceAdjustmentService.isAvailable()) {
            return;
        }

        for (AttendanceAdjustment adjustment : attendanceAdjustmentService.getPendingRequestsForEmployeeInRange(
                employee.getEmployeeId(), periodStart, periodEnd)) {
            report.addIssue(new PayrollReadinessIssue(
                    employee.getEmployeeId(),
                    fullName(employee),
                    adjustment.getAttendanceDate() + ": unresolved attendance adjustment",
                    PayrollReadinessIssue.Severity.NEEDS_REVIEW,
                    "Review attendance adjustment before payroll finalization."
            ));
        }
    }

    private boolean isBlockedAttendanceStatus(String status) {
        return "Missing Time In".equals(status)
                || "Missing Time Out".equals(status)
                || "Incomplete Attendance".equals(status);
    }

    private String getAttendanceRecommendedAction(String status) {
        return switch (status) {
            case "Missing Time In", "Missing Time Out", "Incomplete Attendance" ->
                    "Review and resolve the incomplete attendance log.";
            case "Late", "Undertime" ->
                    "Review the attendance exception before payroll finalization.";
            case "No Assigned Schedule" ->
                    "Confirm schedule assignment or validate attendance exception.";
            default -> "Review workforce data before payroll finalization.";
        };
    }

    private String fullName(Employee employee) {
        String name = safe(employee.getFirstName()) + " " + safe(employee.getLastName());
        return name.isBlank() ? "Employee #" + employee.getEmployeeId() : name.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
