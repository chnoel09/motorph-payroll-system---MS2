/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.service;

import java.time.LocalDate;
import java.util.List;

import com.mycompany.oop.model.Leave;
import com.mycompany.oop.repository.LeaveDatabaseRepository;
import com.mycompany.oop.repository.LeaveRepository;

public class LeaveService {

    private LeaveRepository repository;
    private AuditService auditService;
    private ApprovalWorkflowService approvalWorkflowService;
    private OperationalEventService operationalEventService;

    public LeaveService() {
        this.repository = new LeaveDatabaseRepository();
        this.auditService = new AuditService();
        this.approvalWorkflowService = new ApprovalWorkflowService();
        this.operationalEventService = new OperationalEventService();
    }

    // ================= FILE LEAVE =================
    
    public Leave fileLeave(int employeeId,
                           String type,
                           String start,
                           String end,
                           String reason) {

        validateLeaveInput(type, start, end, reason);

        int newId = getNextLeaveId();

        Leave leave = new Leave(
                newId,
                employeeId,
                type,
                start,
                end,
                reason,
                ApprovalWorkflowService.STATUS_PENDING
        );

        repository.addLeave(leave);
        auditService.logAction(employeeId,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_LEAVE,
                        ApprovalWorkflowService.ACTION_SUBMIT),
                "leaves",
                String.valueOf(newId));
        operationalEventService.recordForRoles(
                OperationalEventService.LEAVE_SUBMITTED,
                "Leave",
                com.mycompany.oop.model.OperationalNotification.Severity.WARNING,
                com.mycompany.oop.model.OperationalNotification.Priority.ACTION_REQUIRED,
                "leaves",
                String.valueOf(newId),
                employeeId,
                "Leave request submitted",
                "Employee #" + employeeId + " submitted " + type + " leave from " + start + " to " + end + ".",
                "hr");

        return leave;
        
    }   

    private int getNextLeaveId() {
        int maxId = 0;
        for (Leave leave : repository.getAllLeaves()) {
            if (leave != null && leave.getLeaveId() > maxId) {
                maxId = leave.getLeaveId();
            }
        }
        return maxId + 1;
    }

    // ================= VALIDATION =================
    private void validateLeaveInput(String type,
                                    String start,
                                    String end,
                                    String reason) {
                                    
        if (type == null || type.isEmpty()
                || start == null || start.isEmpty()
                || end == null || end.isEmpty()
                || reason == null || reason.isEmpty()) {

            throw new IllegalArgumentException(
                    "All fields are required.");
        }

        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(start);
            endDate = LocalDate.parse(end);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid date format. Use YYYY-MM-DD.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date.");
        }
    }

    // ================= APPROVAL =================
    public void approveLeave(int leaveId) {

        Leave leave = repository.findLeave(leaveId);

        if (leave == null) {
            throw new IllegalArgumentException(
                    "Leave not found.");
        }

        approvalWorkflowService.requirePending(leave.getStatus(), "leaves", "approved");

        leave.setStatus(ApprovalWorkflowService.STATUS_APPROVED);
        repository.updateLeave(leave);
        auditService.logAction(null,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_LEAVE,
                        ApprovalWorkflowService.ACTION_APPROVE),
                "leaves",
                String.valueOf(leaveId));
        operationalEventService.recordForEmployee(
                OperationalEventService.LEAVE_APPROVED,
                "Leave",
                com.mycompany.oop.model.OperationalNotification.Severity.SUCCESS,
                com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                "leaves",
                String.valueOf(leaveId),
                null,
                leave.getEmployeeId(),
                "Leave request approved",
                leave.getLeaveType() + " leave from " + leave.getStartDate() + " to "
                        + leave.getEndDate() + " was approved.");
    }

    // ================= REJECTION =================
    public void rejectLeave(int leaveId) {

        Leave leave = repository.findLeave(leaveId);

        if (leave == null) {
            throw new IllegalArgumentException(
                    "Leave not found.");
        }

        approvalWorkflowService.requirePending(leave.getStatus(), "leaves", "rejected");

        leave.setStatus(ApprovalWorkflowService.STATUS_REJECTED);
        repository.updateLeave(leave);
        auditService.logAction(null,
                approvalWorkflowService.auditAction(
                        ApprovalWorkflowService.TYPE_LEAVE,
                        ApprovalWorkflowService.ACTION_REJECT),
                "leaves",
                String.valueOf(leaveId));
        operationalEventService.recordForEmployee(
                OperationalEventService.LEAVE_REJECTED,
                "Leave",
                com.mycompany.oop.model.OperationalNotification.Severity.INFO,
                com.mycompany.oop.model.OperationalNotification.Priority.INFORMATIONAL,
                "leaves",
                String.valueOf(leaveId),
                null,
                leave.getEmployeeId(),
                "Leave request rejected",
                leave.getLeaveType() + " leave from " + leave.getStartDate() + " to "
                        + leave.getEndDate() + " was rejected.");
    }

    // ================= FETCH METHODS =================
    public List<Leave> getAllLeaves() {
        return repository.getAllLeaves();
    }

    public List<Leave> getLeavesByEmployee(int employeeId) {
        return repository.getLeavesByEmployee(employeeId);
    }

    public List<Leave> getLeavesByEmployees(List<Integer> employeeIds) {
        List<Leave> leaves = new java.util.ArrayList<>();
        if (employeeIds == null || employeeIds.isEmpty()) {
            return leaves;
        }

        for (Integer employeeId : employeeIds) {
            if (employeeId != null && employeeId > 0) {
                leaves.addAll(getLeavesByEmployee(employeeId));
            }
        }

        return leaves;
    }
}





