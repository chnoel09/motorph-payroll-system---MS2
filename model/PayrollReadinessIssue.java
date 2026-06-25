package com.mycompany.oop.model;

public class PayrollReadinessIssue {

    public enum Severity {
        INFO,
        NEEDS_REVIEW,
        BLOCKED
    }

    private final int employeeId;
    private final String employeeName;
    private final String issue;
    private final Severity severity;
    private final String recommendedAction;

    public PayrollReadinessIssue(int employeeId, String employeeName, String issue,
            Severity severity, String recommendedAction) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.issue = issue;
        this.severity = severity;
        this.recommendedAction = recommendedAction;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getIssue() {
        return issue;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }
}
