package com.mycompany.oop.model;

import java.util.ArrayList;
import java.util.List;

public class PayrollReadinessReport {

    public enum Status {
        READY,
        NEEDS_REVIEW,
        BLOCKED_INCOMPLETE
    }

    private final List<PayrollReadinessIssue> issues = new ArrayList<>();

    public void addIssue(PayrollReadinessIssue issue) {
        if (issue != null) {
            issues.add(issue);
        }
    }

    public List<PayrollReadinessIssue> getIssues() {
        return new ArrayList<>(issues);
    }

    public Status getStatus() {
        boolean hasBlocked = issues.stream()
                .anyMatch(issue -> issue.getSeverity() == PayrollReadinessIssue.Severity.BLOCKED);
        if (hasBlocked) {
            return Status.BLOCKED_INCOMPLETE;
        }

        boolean needsReview = issues.stream()
                .anyMatch(issue -> issue.getSeverity() == PayrollReadinessIssue.Severity.NEEDS_REVIEW);
        return needsReview ? Status.NEEDS_REVIEW : Status.READY;
    }

    public int getIssueCount() {
        return issues.size();
    }

    public int getBlockedCount() {
        return (int) issues.stream()
                .filter(issue -> issue.getSeverity() == PayrollReadinessIssue.Severity.BLOCKED)
                .count();
    }

    public int getNeedsReviewCount() {
        return (int) issues.stream()
                .filter(issue -> issue.getSeverity() == PayrollReadinessIssue.Severity.NEEDS_REVIEW)
                .count();
    }
}
