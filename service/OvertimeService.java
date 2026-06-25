package com.mycompany.oop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.OvertimeRequest;
import com.mycompany.oop.repository.OvertimeRequestDatabaseRepository;
import com.mycompany.oop.repository.OvertimeRequestRepository;

// Prepared for the future normalized overtime workflow migration.
// This service is intentionally not wired into the current payroll or attendance flow yet.
public class OvertimeService {

    private OvertimeRequestRepository overtimeRequestRepository;
    private EmployeeService employeeService;
    private AuditService auditService;
    private ApprovalWorkflowService approvalWorkflowService;
    private OperationalEventService operationalEventService;

    public OvertimeService() {
        this.overtimeRequestRepository = new OvertimeRequestDatabaseRepository();
        this.employeeService = new EmployeeService();
        this.auditService = new AuditService();
        this.approvalWorkflowService = new ApprovalWorkflowService();
        this.operationalEventService = new OperationalEventService();
    }

    public OvertimeService(OvertimeRequestRepository overtimeRequestRepository) {
        this.overtimeRequestRepository = overtimeRequestRepository;
        this.employeeService = new EmployeeService();
        this.auditService = new AuditService();
        this.approvalWorkflowService = new ApprovalWorkflowService();
        this.operationalEventService = new OperationalEventService();
    }

    public void submitOvertimeRequest(OvertimeRequest request) {
        validateRequest(request);
        request.setStatus(ApprovalWorkflowService.STATUS_PENDING);
        request.setApprovedBy(null);
        request.setApprovedRole(null);
        request.setApprovedAt(null);

        if (overtimeRequestRepository != null) {
            overtimeRequestRepository.addOvertimeRequest(request);
            auditService.logAction(null,
                    approvalWorkflowService.auditAction(
                            ApprovalWorkflowService.TYPE_OVERTIME,
                            ApprovalWorkflowService.ACTION_SUBMIT),
                    "overtime_requests", String.valueOf(request.getEmployeeId()));
            operationalEventService.recordForRoles(
                    OperationalEventService.OVERTIME_SUBMITTED,
                    "Overtime",
                    com.mycompany.oop.model.OperationalNotification.Severity.WARNING,
                    com.mycompany.oop.model.OperationalNotification.Priority.ACTION_REQUIRED,
                    "overtime_requests",
                    String.valueOf(request.getEmployeeId()),
                    request.getEmployeeId(),
                    "Overtime request submitted",
                    "Employee #" + request.getEmployeeId() + " submitted overtime for "
                            + request.getOvertimeDate() + ".",
                    "hr");
        }
    }

    public OvertimeRequest fileOwnOvertimeRequest(Employee employee, java.time.LocalDate overtimeDate,
            double overtimeHours, String reason) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee is required to file overtime.");
        }

        OvertimeRequest request = new OvertimeRequest(
                0,
                employee.getEmployeeId(),
                overtimeDate,
                overtimeHours,
                reason,
                ApprovalWorkflowService.STATUS_PENDING,
                null,
                null,
                null,
                null
        );
        submitOvertimeRequest(request);
        return request;
    }

    public void approveOvertimeRequest(int overtimeId, int approvedBy, String approvalRole) {
        updateOvertimeStatus(overtimeId, ApprovalWorkflowService.STATUS_APPROVED, approvedBy, approvalRole, null);
    }

    public void rejectOvertimeRequest(int overtimeId, int approvedBy, String approvalRole) {
        updateOvertimeStatus(overtimeId, ApprovalWorkflowService.STATUS_REJECTED, approvedBy, approvalRole, null);
    }

    public void approveOvertimeRequest(int overtimeId, Employee approver, String remarks) {
        validateFinalApprover(approver);
        updateOvertimeStatus(overtimeId, ApprovalWorkflowService.STATUS_APPROVED, approver.getEmployeeId(), approver.getRole(), remarks);
    }

    public void rejectOvertimeRequest(int overtimeId, Employee approver, String remarks) {
        validateFinalApprover(approver);
        updateOvertimeStatus(overtimeId, ApprovalWorkflowService.STATUS_REJECTED, approver.getEmployeeId(), approver.getRole(), remarks);
    }

    public List<OvertimeRequest> getRequestsByEmployee(int employeeId) {
        if (overtimeRequestRepository == null) {
            return List.of();
        }
        return sort(overtimeRequestRepository.findByEmployeeId(employeeId));
    }

    public List<OvertimeRequest> getRequestsByEmployees(List<Integer> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        Set<Integer> allowedIds = Set.copyOf(employeeIds);
        List<OvertimeRequest> requests = new ArrayList<>();
        for (OvertimeRequest request : overtimeRequestRepository.findAll()) {
            if (allowedIds.contains(request.getEmployeeId())) {
                requests.add(request);
            }
        }
        return sort(requests);
    }

    public List<OvertimeRequest> getPendingRequestsForReview() {
        List<OvertimeRequest> requests = new ArrayList<>();
        for (OvertimeRequest request : overtimeRequestRepository.findAll()) {
            if (approvalWorkflowService.isPending(request.getStatus())) {
                requests.add(request);
            }
        }
        return sort(requests);
    }

    public List<OvertimeRequest> getApprovedRequests() {
        List<OvertimeRequest> requests = new ArrayList<>();
        for (OvertimeRequest request : overtimeRequestRepository.findAll()) {
            if (ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(safe(request.getStatus()))) {
                requests.add(request);
            }
        }
        return sort(requests);
    }

    public List<OvertimeRequest> getVisibleRequests(Employee viewer) {
        if (viewer == null) {
            return List.of();
        }

        RoleAccessService roleAccessService = new RoleAccessService();
        String role = viewer.getRole();

        if (roleAccessService.isHR(role)) {
            return sort(overtimeRequestRepository.findAll());
        }

        if (roleAccessService.isFinance(role)) {
            List<OvertimeRequest> visible = new ArrayList<>(getApprovedRequests());
            for (OvertimeRequest ownRequest : getRequestsByEmployee(viewer.getEmployeeId())) {
                boolean alreadyIncluded = visible.stream()
                        .anyMatch(request -> request.getOvertimeId() == ownRequest.getOvertimeId());
                if (!alreadyIncluded) {
                    visible.add(ownRequest);
                }
            }
            return sort(visible);
        }

        if (employeeService.hasAssignedTeam(viewer.getEmployeeId())) {
            List<OvertimeRequest> visible = new ArrayList<>(getRequestsByEmployees(
                    getEmployeeIds(employeeService.getTeamMembersForSupervisor(viewer.getEmployeeId()))));
            for (OvertimeRequest ownRequest : getRequestsByEmployee(viewer.getEmployeeId())) {
                boolean alreadyIncluded = visible.stream()
                        .anyMatch(request -> request.getOvertimeId() == ownRequest.getOvertimeId());
                if (!alreadyIncluded) {
                    visible.add(ownRequest);
                }
            }
            return sort(visible);
        }

        return getRequestsByEmployee(viewer.getEmployeeId());
    }

    private void updateOvertimeStatus(int overtimeId, String status, int approvedBy, String approvalRole, String remarks) {
        if (overtimeRequestRepository == null) {
            return;
        }

        OvertimeRequest request = overtimeRequestRepository.findById(overtimeId);

        if (request == null) {
            throw new IllegalArgumentException("Overtime request not found.");
        }

        approvalWorkflowService.requirePending(request.getStatus(), "overtime requests", "reviewed");

        request.setStatus(status);
        request.setApprovedBy(approvedBy);
        request.setApprovedRole(approvalRole);
        request.setApprovedAt(LocalDateTime.now());
        request.setRemarks(remarks);

        overtimeRequestRepository.updateOvertimeRequest(request);
        auditService.logAction(null,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_OVERTIME,
                        ApprovalWorkflowService.STATUS_APPROVED.equals(status)
                                ? ApprovalWorkflowService.ACTION_APPROVE
                                : ApprovalWorkflowService.ACTION_REJECT),
                "overtime_requests",
                String.valueOf(overtimeId));
        if (ApprovalWorkflowService.STATUS_APPROVED.equals(status)) {
            operationalEventService.recordForEmployee(
                    OperationalEventService.OVERTIME_APPROVED,
                    "Overtime",
                    com.mycompany.oop.model.OperationalNotification.Severity.SUCCESS,
                    com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                    "overtime_requests",
                    String.valueOf(overtimeId),
                    approvedBy,
                    request.getEmployeeId(),
                    "Overtime request approved",
                    "Overtime on " + request.getOvertimeDate() + " was approved.");
            operationalEventService.recordForRoles(
                    OperationalEventService.OVERTIME_APPROVED,
                    "Overtime",
                    com.mycompany.oop.model.OperationalNotification.Severity.INFO,
                    com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                    "overtime_requests",
                    String.valueOf(overtimeId),
                    approvedBy,
                    "Approved overtime visible",
                    "Approved overtime is visible for future payroll readiness only.",
                    "finance");
        } else {
            operationalEventService.recordForEmployee(
                    OperationalEventService.OVERTIME_REJECTED,
                    "Overtime",
                    com.mycompany.oop.model.OperationalNotification.Severity.INFO,
                    com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                    "overtime_requests",
                    String.valueOf(overtimeId),
                    approvedBy,
                    request.getEmployeeId(),
                    "Overtime request rejected",
                    "Overtime on " + request.getOvertimeDate() + " was rejected.");
        }
    }

    private void validateRequest(OvertimeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Overtime request is required.");
        }
        if (request.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Employee is required.");
        }
        if (request.getOvertimeDate() == null) {
            throw new IllegalArgumentException("Overtime date is required.");
        }
        if (request.getOvertimeHours() <= 0) {
            throw new IllegalArgumentException("Overtime hours must be positive.");
        }
        if (request.getOvertimeHours() > 24) {
            throw new IllegalArgumentException("Overtime hours cannot exceed 24.");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required.");
        }
    }

    private void validateFinalApprover(Employee approver) {
        approvalWorkflowService.requireFinalApprover(approver, "overtime requests");
    }

    private List<OvertimeRequest> sort(List<OvertimeRequest> requests) {
        List<OvertimeRequest> sorted = new ArrayList<>(requests == null ? List.of() : requests);
        sorted.sort(Comparator
                .comparing(OvertimeRequest::getOvertimeDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(OvertimeRequest::getOvertimeId)
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
