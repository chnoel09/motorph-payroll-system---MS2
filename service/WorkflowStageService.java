package com.mycompany.oop.service;

import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.model.OvertimeRequest;

import java.util.Locale;

public class WorkflowStageService {

    private final RoleAccessService roleAccessService;

    public WorkflowStageService() {
        this.roleAccessService = new RoleAccessService();
    }

    public String leaveStage(Leave leave, Employee viewer) {
        String status = normalize(leave == null ? null : leave.getStatus());
        if (ApprovalWorkflowService.STATUS_APPROVED.equals(status)) {
            return "Approved";
        }
        if (ApprovalWorkflowService.STATUS_REJECTED.equals(status)) {
            return "Rejected by HR";
        }
        if ("RECOMMENDED".equals(status) || "SUPERVISOR_APPROVED".equals(status)) {
            return "Pending HR Approval";
        }
        if (isOwnRecord(leave == null ? null : leave.getEmployeeId(), viewer)) {
            return "Pending Supervisor Approval";
        }
        if (roleAccessService.isHR(role(viewer))) {
            return "Pending Final Workforce Approval";
        }
        if (roleAccessService.isSupervisor(role(viewer))) {
            return "Pending Supervisor Review";
        }
        return "Pending Supervisor Approval";
    }

    public String overtimeStage(OvertimeRequest request, Employee viewer) {
        String status = normalize(request == null ? null : request.getStatus());
        if (ApprovalWorkflowService.STATUS_APPROVED.equals(status)) {
            return "Approved for Payroll Visibility";
        }
        if (ApprovalWorkflowService.STATUS_REJECTED.equals(status)) {
            return "Rejected by " + approverOwner(request == null ? null : request.getApprovedRole());
        }
        if ("RECOMMENDED".equals(status) || "SUPERVISOR_APPROVED".equals(status)) {
            return "Pending HR Approval";
        }
        if (isOwnRecord(request == null ? null : request.getEmployeeId(), viewer)) {
            return "Pending Supervisor Approval";
        }
        if (roleAccessService.isHR(role(viewer))) {
            return "Pending Final Workforce Approval";
        }
        if (roleAccessService.isSupervisor(role(viewer))) {
            return "Pending Supervisor Review";
        }
        return "Pending Supervisor Approval";
    }

    public String attendanceAdjustmentStage(AttendanceAdjustment adjustment, Employee viewer) {
        if (adjustment != null && adjustment.isResolved()) {
            return "Corrected";
        }
        if (isOwnRecord(adjustment == null ? null : adjustment.getEmployeeId(), viewer)) {
            return "Pending Supervisor Review";
        }
        if (roleAccessService.isHR(role(viewer))) {
            return "Pending HR Correction Approval";
        }
        if (roleAccessService.isSupervisor(role(viewer))) {
            return "Pending Supervisor Review";
        }
        return "Pending Supervisor Review";
    }

    public String ownerForStage(String stage) {
        String normalized = stage == null ? "" : stage.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("supervisor")) {
            return "Supervisor";
        }
        if (normalized.contains("hr") || normalized.contains("workforce")) {
            return "HR";
        }
        if (normalized.contains("payroll")) {
            return "Finance visibility";
        }
        if (normalized.contains("corrected") || normalized.contains("approved")) {
            return "Complete";
        }
        if (normalized.contains("rejected")) {
            return "Decision recorded";
        }
        return "Workflow";
    }

    public String nextActionForStage(String stage) {
        String normalized = stage == null ? "" : stage.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("supervisor")) {
            return "Supervisor coordination";
        }
        if (normalized.contains("hr") || normalized.contains("workforce")) {
            return "HR decision";
        }
        if (normalized.contains("payroll")) {
            return "Visible to Finance";
        }
        if (normalized.contains("corrected") || normalized.contains("approved")) {
            return "No action needed";
        }
        if (normalized.contains("rejected")) {
            return "Review remarks";
        }
        return "View details";
    }

    public boolean isActionableForViewer(String stage, Employee viewer) {
        String normalized = stage == null ? "" : stage.toLowerCase(Locale.ENGLISH);
        String role = role(viewer);
        return ((normalized.contains("hr") || normalized.contains("workforce"))
                && roleAccessService.isHR(role));
    }

    private String approverOwner(String approvedRole) {
        String role = normalize(approvedRole);
        if ("SUPERVISOR".equals(role) || "MANAGER".equals(role)) {
            return "Supervisor";
        }
        if ("HR".equals(role)) {
            return "HR";
        }
        return "HR";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private String role(Employee viewer) {
        return viewer == null ? "" : viewer.getRole();
    }

    private boolean isOwnRecord(Integer employeeId, Employee viewer) {
        return employeeId != null && viewer != null && employeeId == viewer.getEmployeeId();
    }
}
