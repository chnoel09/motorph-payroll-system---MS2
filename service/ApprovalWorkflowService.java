package com.mycompany.oop.service;

import com.mycompany.oop.model.Employee;

// Shared runtime approval helpers for leave, overtime, and attendance adjustment workflows.
// Persistence remains module-specific until a generic workflow_actions table is introduced.
public class ApprovalWorkflowService {

    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_OVERTIME = "OVERTIME";
    public static final String TYPE_ATTENDANCE_ADJUSTMENT = "ATTENDANCE_ADJUSTMENT";

    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_REVIEW = "REVIEW";
    public static final String ACTION_CORRECT = "CORRECT";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_REVIEWED = "REVIEWED";
    public static final String STATUS_CORRECTED = "CORRECTED";

    private final RoleAccessService roleAccessService;

    public ApprovalWorkflowService() {
        this.roleAccessService = new RoleAccessService();
    }

    public boolean isPending(String status) {
        return STATUS_PENDING.equalsIgnoreCase(safe(status));
    }

    public void requirePending(String status, String itemLabel, String actionLabel) {
        if (!isPending(status)) {
            throw new IllegalStateException("Only pending " + itemLabel + " can be " + actionLabel + ".");
        }
    }

    public void requireFinalApprover(Employee approver, String itemLabel) {
        if (approver == null || !roleAccessService.canFinalizeWorkforceApprovals(approver.getRole())) {
            throw new IllegalArgumentException("Only HR users can approve or reject " + itemLabel + ".");
        }
    }

    public void requireProcessor(Employee processor, String itemLabel) {
        if (processor == null || !roleAccessService.canFinalizeWorkforceApprovals(processor.getRole())) {
            throw new IllegalArgumentException("Only HR users can process " + itemLabel + ".");
        }
    }

    public String statusLabel(String status) {
        String normalized = safe(status).toUpperCase();
        return switch (normalized) {
            case STATUS_APPROVED -> "Approved";
            case STATUS_REJECTED -> "Rejected";
            case STATUS_REVIEWED -> "Reviewed";
            case STATUS_CORRECTED -> "Corrected";
            case STATUS_PENDING -> "Pending";
            default -> normalized.isEmpty() ? "Pending" : titleCase(normalized);
        };
    }

    public String actionLabel(String action) {
        String normalized = safe(action).toUpperCase();
        return switch (normalized) {
            case ACTION_APPROVE -> "approved";
            case ACTION_REJECT -> "rejected";
            case ACTION_REVIEW -> "reviewed";
            case ACTION_CORRECT -> "corrected";
            case ACTION_SUBMIT -> "submitted";
            default -> normalized.isEmpty() ? "updated" : titleCase(normalized).toLowerCase();
        };
    }

    public String auditAction(String workflowType, String action) {
        String type = safe(workflowType).toUpperCase();
        String normalizedAction = safe(action).toUpperCase();

        if (TYPE_LEAVE.equals(type)) {
            if (ACTION_APPROVE.equals(normalizedAction)) return AuditService.LEAVE_APPROVED;
            if (ACTION_REJECT.equals(normalizedAction)) return AuditService.LEAVE_REJECTED;
            if (ACTION_SUBMIT.equals(normalizedAction)) return AuditService.LEAVE_REQUEST_SUBMITTED;
        }

        if (TYPE_OVERTIME.equals(type)) {
            if (ACTION_APPROVE.equals(normalizedAction)) return AuditService.OVERTIME_APPROVED;
            if (ACTION_REJECT.equals(normalizedAction)) return AuditService.OVERTIME_REJECTED;
            if (ACTION_SUBMIT.equals(normalizedAction)) return AuditService.OVERTIME_REQUEST_SUBMITTED;
        }

        if (TYPE_ATTENDANCE_ADJUSTMENT.equals(type)) {
            if (ACTION_REVIEW.equals(normalizedAction)) return AuditService.ATTENDANCE_ADJUSTMENT_REVIEWED;
            if (ACTION_CORRECT.equals(normalizedAction)) return AuditService.ATTENDANCE_ADJUSTMENT_CORRECTED;
            if (ACTION_SUBMIT.equals(normalizedAction)) return AuditService.ATTENDANCE_ADJUSTMENT_REQUESTED;
        }

        return type + "_" + normalizedAction;
    }

    private String titleCase(String value) {
        String lower = value.toLowerCase().replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String part : lower.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
