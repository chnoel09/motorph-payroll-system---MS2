package com.mycompany.oop.service;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.model.Leave;

// Lightweight workflow visibility helper. It only reads existing services and does not change business rules.
public class WorkflowAwarenessService {

    private LeaveService leaveService;
    private PayrollService payrollService;

    public WorkflowAwarenessService() {
        this.leaveService = new LeaveService();
        this.payrollService = new PayrollService();
    }

    public int getPendingLeaveCount() {
        List<Leave> leaves = leaveService.getAllLeaves();
        int pendingLeaveCount = 0;

        for (Leave leave : leaves) {
            if (leave.getStatus() != null && leave.getStatus().equalsIgnoreCase("PENDING")) {
                pendingLeaveCount++;
            }
        }

        return pendingLeaveCount;
    }

    public String getLeaveReviewIndicator() {
        int pendingLeaveCount = getPendingLeaveCount();
        return pendingLeaveCount > 0 ? "Action Required: " + pendingLeaveCount + " pending leave request(s)" : "No pending leave approvals";
    }

    public int getPendingLeaveCountForEmployee(int employeeId) {
        List<Leave> leaves = leaveService.getLeavesByEmployee(employeeId);
        int pendingLeaveCount = 0;

        for (Leave leave : leaves) {
            if (leave.getStatus() != null && leave.getStatus().equalsIgnoreCase("PENDING")) {
                pendingLeaveCount++;
            }
        }

        return pendingLeaveCount;
    }

    public String getEmployeeLeaveIndicator(int employeeId) {
        int pendingLeaveCount = getPendingLeaveCountForEmployee(employeeId);
        return pendingLeaveCount > 0 ? "You have " + pendingLeaveCount + " pending leave request(s)" : "No pending leave requests";
    }

    public String getPayrollPendingIndicator() {
        List<String> processedCutoffs = payrollService.getProcessedCutoffs();
        return processedCutoffs.isEmpty() ? "Pending Payroll: no saved payroll history yet" : "Payroll history available";
    }

    public String getLastPayrollGenerated() {
        List<String> processedCutoffs = payrollService.getProcessedCutoffs();

        if (processedCutoffs.isEmpty()) {
            return "Last Payroll Generated: none";
        }

        return "Last Payroll Generated: " + processedCutoffs.get(processedCutoffs.size() - 1);
    }

    public List<String> getWorkflowSummaryItems() {
        List<String> items = new ArrayList<>();

        items.add(getLeaveReviewIndicator());
        items.add(getLastPayrollGenerated());
        items.add(getPayrollPendingIndicator());

        return items;
    }

    public List<String> getEmployeeWorkflowSummaryItems(int employeeId) {
        List<String> items = new ArrayList<>();

        items.add(getEmployeeLeaveIndicator(employeeId));
        items.add("Self-service workspace ready");

        return items;
    }
}
