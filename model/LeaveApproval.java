package com.mycompany.oop.model;

import java.time.LocalDateTime;

// Prepared for the future normalized leave_approvals workflow migration.
public class LeaveApproval {

    private int approvalId;
    private int leaveId;
    private int approvedBy;
    private String approvalRole;
    private String action;
    private LocalDateTime approvalDate;
    private String remarks;

    public LeaveApproval() {
    }

    public LeaveApproval(int approvalId, int leaveId, int approvedBy, String approvalRole,
            String action, LocalDateTime approvalDate, String remarks) {
        this.approvalId = approvalId;
        this.leaveId = leaveId;
        this.approvedBy = approvedBy;
        this.approvalRole = approvalRole;
        this.action = action;
        this.approvalDate = approvalDate;
        this.remarks = remarks;
    }

    public int getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(int approvalId) {
        this.approvalId = approvalId;
    }

    public int getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(int leaveId) {
        this.leaveId = leaveId;
    }

    public int getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(int approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovalRole() {
        return approvalRole;
    }

    public void setApprovalRole(String approvalRole) {
        this.approvalRole = approvalRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
