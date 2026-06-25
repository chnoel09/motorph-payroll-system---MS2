package com.mycompany.oop.service;

import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.repository.AttendanceAdjustmentDatabaseRepository;
import com.mycompany.oop.repository.AttendanceAdjustmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AttendanceAdjustmentService {

    private final AttendanceAdjustmentRepository repository;
    private final EmployeeService employeeService;
    private final AuditService auditService;
    private final RoleAccessService roleAccessService;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final OperationalEventService operationalEventService;

    public AttendanceAdjustmentService() {
        this.repository = new AttendanceAdjustmentDatabaseRepository();
        this.employeeService = new EmployeeService();
        this.auditService = new AuditService();
        this.roleAccessService = new RoleAccessService();
        this.approvalWorkflowService = new ApprovalWorkflowService();
        this.operationalEventService = new OperationalEventService();
    }

    public boolean isAvailable() {
        return repository.isAvailable();
    }

    public AttendanceAdjustment requestOwnAdjustment(Employee employee, LocalDate attendanceDate,
            String adjustmentType, String remarks) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee is required.");
        }

        AttendanceAdjustment adjustment = new AttendanceAdjustment(
                0,
                employee.getEmployeeId(),
                attendanceDate,
                adjustmentType,
                remarks,
                null,
                null
        );
        validateAdjustment(adjustment);
        repository.addAttendanceAdjustment(adjustment);
        auditService.logAction(null,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_ATTENDANCE_ADJUSTMENT,
                        ApprovalWorkflowService.ACTION_SUBMIT),
                "attendance_adjustments", String.valueOf(employee.getEmployeeId()));
        operationalEventService.recordForRoles(
                OperationalEventService.ATTENDANCE_ADJUSTMENT_REQUESTED,
                "Attendance",
                com.mycompany.oop.model.OperationalNotification.Severity.WARNING,
                com.mycompany.oop.model.OperationalNotification.Priority.ACTION_REQUIRED,
                "attendance_adjustments",
                String.valueOf(employee.getEmployeeId()),
                employee.getEmployeeId(),
                "Attendance correction requested",
                "Employee #" + employee.getEmployeeId() + " requested an attendance correction for "
                        + attendanceDate + ".",
                "hr");
        return adjustment;
    }

    public void markReviewed(int adjustmentId, Employee reviewer) {
        validateProcessor(reviewer);
        AttendanceAdjustment adjustment = getRequired(adjustmentId);
        auditService.logAction(null,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_ATTENDANCE_ADJUSTMENT,
                        ApprovalWorkflowService.ACTION_REVIEW),
                "attendance_adjustments", String.valueOf(adjustment.getAdjustmentId()));
        operationalEventService.recordForEmployee(
                OperationalEventService.ATTENDANCE_ADJUSTMENT_REVIEWED,
                "Attendance",
                com.mycompany.oop.model.OperationalNotification.Severity.INFO,
                com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                "attendance_adjustments",
                String.valueOf(adjustment.getAdjustmentId()),
                reviewer.getEmployeeId(),
                adjustment.getEmployeeId(),
                "Attendance correction reviewed",
                "Your attendance correction for " + adjustment.getAttendanceDate() + " was reviewed.");
    }

    public void markCorrected(int adjustmentId, Employee processor) {
        validateProcessor(processor);
        AttendanceAdjustment adjustment = getRequired(adjustmentId);
        adjustment.setAdjustedBy(processor.getEmployeeId());
        adjustment.setAdjustedAt(LocalDateTime.now());
        repository.updateAttendanceAdjustment(adjustment);
        auditService.logAction(null,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_ATTENDANCE_ADJUSTMENT,
                        ApprovalWorkflowService.ACTION_CORRECT),
                "attendance_adjustments", String.valueOf(adjustment.getAdjustmentId()));
        operationalEventService.recordForEmployee(
                OperationalEventService.ATTENDANCE_ADJUSTMENT_CORRECTED,
                "Attendance",
                com.mycompany.oop.model.OperationalNotification.Severity.SUCCESS,
                com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                "attendance_adjustments",
                String.valueOf(adjustment.getAdjustmentId()),
                processor.getEmployeeId(),
                adjustment.getEmployeeId(),
                "Attendance correction recorded",
                "Your attendance correction for " + adjustment.getAttendanceDate() + " was marked corrected.");
    }

    public List<AttendanceAdjustment> getRequestsByEmployee(int employeeId) {
        return sort(repository.findByEmployeeId(employeeId));
    }

    public List<AttendanceAdjustment> getRequestsByEmployees(List<Integer> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        Set<Integer> allowedIds = Set.copyOf(employeeIds);
        List<AttendanceAdjustment> scoped = new ArrayList<>();
        for (AttendanceAdjustment adjustment : repository.findAll()) {
            if (allowedIds.contains(adjustment.getEmployeeId())) {
                scoped.add(adjustment);
            }
        }
        return sort(scoped);
    }

    public List<AttendanceAdjustment> getVisibleRequests(Employee viewer) {
        if (viewer == null) {
            return List.of();
        }

        String role = viewer.getRole();
        if (roleAccessService.isHR(role)) {
            return sort(repository.findAll());
        }
        if (employeeService.hasAssignedTeam(viewer.getEmployeeId())) {
            return getRequestsByEmployees(getEmployeeIds(
                    employeeService.getTeamMembersForSupervisor(viewer.getEmployeeId())));
        }
        return getRequestsByEmployee(viewer.getEmployeeId());
    }

    public List<AttendanceAdjustment> getPendingRequestsForReview() {
        List<AttendanceAdjustment> pending = new ArrayList<>();
        for (AttendanceAdjustment adjustment : repository.findAll()) {
            if (!adjustment.isResolved()) {
                pending.add(adjustment);
            }
        }
        return sort(pending);
    }

    public List<AttendanceAdjustment> getPendingRequestsForEmployeeInRange(int employeeId,
            LocalDate periodStart, LocalDate periodEnd) {
        List<AttendanceAdjustment> pending = new ArrayList<>();
        if (periodStart == null || periodEnd == null) {
            return pending;
        }

        for (AttendanceAdjustment adjustment : repository.findByEmployeeId(employeeId)) {
            LocalDate date = adjustment.getAttendanceDate();
            if (!adjustment.isResolved()
                    && date != null
                    && !date.isBefore(periodStart)
                    && !date.isAfter(periodEnd)) {
                pending.add(adjustment);
            }
        }
        return sort(pending);
    }

    private AttendanceAdjustment getRequired(int adjustmentId) {
        AttendanceAdjustment adjustment = repository.findById(adjustmentId);
        if (adjustment == null) {
            throw new IllegalArgumentException("Attendance adjustment request not found.");
        }
        return adjustment;
    }

    private void validateAdjustment(AttendanceAdjustment adjustment) {
        if (adjustment.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (adjustment.getAttendanceDate() == null) {
            throw new IllegalArgumentException("Attendance date is required.");
        }
        if (isBlank(adjustment.getAdjustmentType())) {
            throw new IllegalArgumentException("Adjustment type is required.");
        }
        if (isBlank(adjustment.getRemarks())) {
            throw new IllegalArgumentException("Remarks are required.");
        }
    }

    private void validateProcessor(Employee employee) {
        approvalWorkflowService.requireProcessor(employee, "attendance adjustments");
    }

    private List<AttendanceAdjustment> sort(List<AttendanceAdjustment> adjustments) {
        List<AttendanceAdjustment> sorted = new ArrayList<>(adjustments == null ? List.of() : adjustments);
        sorted.sort(Comparator
                .comparing(AttendanceAdjustment::getAttendanceDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AttendanceAdjustment::getAdjustmentId)
                .reversed());
        return sorted;
    }

    private List<Integer> getEmployeeIds(List<Employee> employees) {
        List<Integer> ids = new ArrayList<>();
        for (Employee employee : employees) {
            ids.add(employee.getEmployeeId());
        }
        return ids;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
