package com.mycompany.oop.service;

import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.model.OperationalNotification;
import com.mycompany.oop.model.OvertimeRequest;
import com.mycompany.oop.model.PayrollLifecycleStatus;
import com.mycompany.oop.model.PayrollReadinessReport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationalInboxService {

    private static final long CACHE_TTL_MS = 30_000L;
    private static final int MAX_NOTIFICATIONS = 24;
    private static final Map<String, CacheEntry> CACHE = new HashMap<>();

    private final RoleAccessService roleAccessService;
    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final AttendanceAwarenessService attendanceAwarenessService;
    private final AttendanceAdjustmentService attendanceAdjustmentService;
    private final OvertimeService overtimeService;
    private final PayrollService payrollService;
    private final PayrollReadinessService payrollReadinessService;
    private final PayrollLifecycleService payrollLifecycleService;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final OperationalEventService operationalEventService;
    private final WorkflowStageService workflowStageService;

    public OperationalInboxService() {
        this.roleAccessService = new RoleAccessService();
        this.employeeService = new EmployeeService();
        this.leaveService = new LeaveService();
        this.attendanceAwarenessService = new AttendanceAwarenessService();
        this.attendanceAdjustmentService = new AttendanceAdjustmentService();
        this.overtimeService = new OvertimeService();
        this.payrollService = new PayrollService();
        this.payrollReadinessService = new PayrollReadinessService();
        this.payrollLifecycleService = new PayrollLifecycleService();
        this.approvalWorkflowService = new ApprovalWorkflowService();
        this.operationalEventService = new OperationalEventService();
        this.workflowStageService = new WorkflowStageService();
    }

    public List<OperationalNotification> getNotifications(Employee viewer) {
        if (viewer == null) {
            return List.of();
        }

        String key = viewer.getEmployeeId() + ":" + roleAccessService.normalizeRole(viewer.getRole());
        long now = System.currentTimeMillis();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && now - cached.createdAtMs < CACHE_TTL_MS) {
            return new ArrayList<>(cached.notifications);
        }

        List<OperationalNotification> notifications = new ArrayList<>();
        if (!roleAccessService.isAdmin(viewer.getRole())) {
            notifications.addAll(operationalEventService.getPersistentNotifications(
                    viewer.getEmployeeId(), viewer.getRole(), MAX_NOTIFICATIONS));
        }
        notifications.addAll(buildNotifications(viewer));
        notifications = dedupeNotifications(notifications);
        notifications.sort(Comparator
                .comparingInt((OperationalNotification notification) -> priorityRank(notification.getPriority()))
                .thenComparing(OperationalNotification::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());

        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = new ArrayList<>(notifications.subList(0, MAX_NOTIFICATIONS));
        }

        CACHE.put(key, new CacheEntry(now, notifications));
        return new ArrayList<>(notifications);
    }

    public static void invalidateCache() {
        CACHE.clear();
    }

    public static void invalidateCache(Employee viewer) {
        if (viewer == null) {
            invalidateCache();
            return;
        }
        CACHE.keySet().removeIf(key -> key.startsWith(viewer.getEmployeeId() + ":"));
    }

    public int getActiveCount(Employee viewer) {
        return getUnreadPersistentCount(viewer);
    }

    public int getUnreadPersistentCount(Employee viewer) {
        if (viewer == null) {
            return 0;
        }
        return operationalEventService.countUnreadNotifications(viewer.getEmployeeId(), viewer.getRole());
    }

    public int markVisibleNotificationsRead(Employee viewer) {
        if (viewer == null) {
            return 0;
        }
        int updated = operationalEventService.markActiveNotificationsRead(viewer.getEmployeeId(), viewer.getRole());
        invalidateCache(viewer);
        return updated;
    }

    private List<OperationalNotification> buildNotifications(Employee viewer) {
        List<OperationalNotification> notifications = new ArrayList<>();
        String role = viewer.getRole();
        addEmployeeNotifications(notifications, viewer);

        if (roleAccessService.isEmployee(role)) {
        } else if (roleAccessService.isSupervisor(role)) {
            addTeamNotifications(notifications, viewer);
        } else if (roleAccessService.isAdmin(role)) {
            addAdminNotifications(notifications, viewer);
        } else if (roleAccessService.isHR(role)) {
            addHrNotifications(notifications, viewer);
        } else if (roleAccessService.isFinance(role)) {
            addPayrollNotifications(notifications);
        } else if (roleAccessService.isIT(role)) {
            notifications.add(new OperationalNotification(
                    "Access operations available",
                    "Access Management is available for user and role support.",
                    "System",
                    OperationalNotification.Severity.INFO,
                    LocalDateTime.now(),
                    "Info"
            ));
        }

        if (notifications.isEmpty()) {
            notifications.add(new OperationalNotification(
                    getEmptyStateTitle(viewer),
                    getEmptyStateMessage(viewer),
                    "Inbox",
                    OperationalNotification.Severity.SUCCESS,
                    OperationalNotification.Priority.INFORMATIONAL,
                    LocalDateTime.now(),
                    "Clear"
            ));
        }

        return notifications;
    }

    public void markRead(int notificationId) {
        operationalEventService.markRead(notificationId);
    }

    public void acknowledge(int notificationId) {
        operationalEventService.acknowledge(notificationId);
    }

    private void addEmployeeNotifications(List<OperationalNotification> notifications, Employee employee) {
        LocalDate today = LocalDate.now();
        AttendanceAwareness awareness = attendanceAwarenessService.getDailyAwareness(employee.getEmployeeId(), today);
        if (awareness.requiresReview()) {
            notifications.add(new OperationalNotification(
                    awareness.getStatus(),
                    awareness.getMessage(),
                    "Attendance",
                    toSeverity(awareness),
                    toPriority(awareness),
                    LocalDateTime.now(),
                    "Requires Review"
            ));
        }

        for (Leave leave : leaveService.getLeavesByEmployee(employee.getEmployeeId())) {
            String status = safe(leave.getStatus());
            String stage = workflowStageService.leaveStage(leave, employee);
            if (approvalWorkflowService.isPending(status)) {
                notifications.add(new OperationalNotification(
                        "Leave request",
                        leave.getLeaveType() + " leave from " + leave.getStartDate() + " to " + leave.getEndDate()
                                + " is at stage: " + stage + ". Current owner: "
                                + workflowStageService.ownerForStage(stage) + ".",
                        "Leave",
                        OperationalNotification.Severity.WARNING,
                        OperationalNotification.Priority.REVIEW,
                        LocalDateTime.now(),
                        stage
                ));
            } else if (ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status)
                    || ApprovalWorkflowService.STATUS_REJECTED.equalsIgnoreCase(status)) {
                notifications.add(new OperationalNotification(
                        "Leave request " + stage.toLowerCase(),
                        leave.getLeaveType() + " leave from " + leave.getStartDate() + " to " + leave.getEndDate()
                                + " is now: " + stage + ".",
                        "Leave",
                        ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status)
                                ? OperationalNotification.Severity.SUCCESS
                                : OperationalNotification.Severity.INFO,
                        OperationalNotification.Priority.INFORMATIONAL,
                        LocalDateTime.now(),
                        stage
                ));
            }
        }

        for (OvertimeRequest overtime : overtimeService.getRequestsByEmployee(employee.getEmployeeId())) {
            String status = safe(overtime.getStatus());
            String stage = workflowStageService.overtimeStage(overtime, employee);
            if (approvalWorkflowService.isPending(status)) {
                notifications.add(new OperationalNotification(
                        "Overtime request",
                        "Overtime on " + overtime.getOvertimeDate() + " for " + overtime.getOvertimeHours()
                                + " hour(s) is at stage: " + stage + ". Current owner: "
                                + workflowStageService.ownerForStage(stage) + ".",
                        "Overtime",
                        OperationalNotification.Severity.WARNING,
                        OperationalNotification.Priority.REVIEW,
                        LocalDateTime.now(),
                        stage
                ));
            } else if (ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status)
                    || ApprovalWorkflowService.STATUS_REJECTED.equalsIgnoreCase(status)) {
                notifications.add(new OperationalNotification(
                        "Overtime request " + stage.toLowerCase(),
                        "Overtime on " + overtime.getOvertimeDate() + " is now: " + stage + ".",
                        "Overtime",
                        ApprovalWorkflowService.STATUS_APPROVED.equalsIgnoreCase(status)
                                ? OperationalNotification.Severity.SUCCESS
                                : OperationalNotification.Severity.INFO,
                        OperationalNotification.Priority.INFORMATIONAL,
                        LocalDateTime.now(),
                        stage
                ));
            }
        }

        for (AttendanceAdjustment adjustment : attendanceAdjustmentService.getRequestsByEmployee(employee.getEmployeeId())) {
            if (!adjustment.isResolved()) {
                String stage = workflowStageService.attendanceAdjustmentStage(adjustment, employee);
                notifications.add(new OperationalNotification(
                        "Attendance correction",
                        "Correction request for " + adjustment.getAttendanceDate() + " is at stage: "
                                + stage + ". Current owner: " + workflowStageService.ownerForStage(stage) + ".",
                        "Attendance",
                        OperationalNotification.Severity.WARNING,
                        OperationalNotification.Priority.REVIEW,
                        LocalDateTime.now(),
                        stage
                ));
            }
        }
    }

    private void addTeamNotifications(List<OperationalNotification> notifications, Employee viewer) {
        List<Employee> teamMembers = employeeService.getTeamOperationsEmployees(viewer);
        if (teamMembers.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        int attendanceIssues = 0;
        for (AttendanceAwareness awareness : attendanceAwarenessService.getTeamAwareness(teamMembers, today, today)) {
            if (awareness.requiresReview()) {
                attendanceIssues++;
            }
        }

        if (attendanceIssues > 0) {
            notifications.add(new OperationalNotification(
                    "Team attendance issue detected",
                    attendanceIssues + " team attendance concern" + plural(attendanceIssues)
                            + (attendanceIssues == 1 ? " requires" : " require") + " review today.",
                    "Team Operations",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.ACTION_REQUIRED,
                    LocalDateTime.now(),
                    "Active"
            ));
        }

        int pendingLeaves = 0;
        for (Integer employeeId : getEmployeeIds(teamMembers)) {
            for (Leave leave : leaveService.getLeavesByEmployee(employeeId)) {
                if (approvalWorkflowService.isPending(leave.getStatus())) {
                    pendingLeaves++;
                }
            }
        }

        if (pendingLeaves > 0) {
            notifications.add(new OperationalNotification(
                    "Team leave requests pending",
                    pendingLeaves + " team leave request" + plural(pendingLeaves)
                            + " are awaiting supervisor review before HR final approval.",
                    "Leave",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.REVIEW,
                    LocalDateTime.now(),
                    "Pending Supervisor Approval"
            ));
        }

        int pendingOvertime = 0;
        for (OvertimeRequest overtime : overtimeService.getRequestsByEmployees(getEmployeeIds(teamMembers))) {
            if (approvalWorkflowService.isPending(overtime.getStatus())) {
                pendingOvertime++;
            }
        }

        if (pendingOvertime > 0) {
            notifications.add(new OperationalNotification(
                    "Team overtime requests visible",
                    pendingOvertime + " team overtime request" + plural(pendingOvertime)
                            + " are awaiting supervisor review before HR final approval.",
                    "Overtime",
                    OperationalNotification.Severity.INFO,
                    OperationalNotification.Priority.REVIEW,
                    LocalDateTime.now(),
                    "Pending Supervisor Approval"
            ));
        }

        int pendingAdjustments = 0;
        for (AttendanceAdjustment adjustment : attendanceAdjustmentService.getRequestsByEmployees(getEmployeeIds(teamMembers))) {
            if (!adjustment.isResolved()) {
                pendingAdjustments++;
            }
        }
        if (pendingAdjustments > 0) {
            notifications.add(new OperationalNotification(
                    "Team attendance corrections pending",
                    pendingAdjustments + " team correction request" + plural(pendingAdjustments)
                            + " are awaiting supervisor review before HR correction approval.",
                    "Attendance",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.REVIEW,
                    LocalDateTime.now(),
                    "Pending Supervisor Review"
            ));
        }
    }

    private void addHrNotifications(List<OperationalNotification> notifications, Employee viewer) {
        int pendingLeaves = new WorkflowAwarenessService().getPendingLeaveCount();
        if (pendingLeaves > 0) {
            notifications.add(new OperationalNotification(
                    "Leave requests require review",
                    pendingLeaves + " leave request" + plural(pendingLeaves)
                            + " require final workforce governance approval.",
                    "Leave Review",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.ACTION_REQUIRED,
                    LocalDateTime.now(),
                    "Pending Final Workforce Approval"
            ));
        }

        List<Employee> workforce = employeeService.getAllEmployees();
        int attendanceIssues = 0;
        for (AttendanceAwareness awareness : attendanceAwarenessService.getTeamAwareness(
                workforce, LocalDate.now(), LocalDate.now())) {
            if (awareness.requiresReview()) {
                attendanceIssues++;
            }
        }
        if (attendanceIssues > 0) {
            notifications.add(new OperationalNotification(
                    "Attendance review items",
                    attendanceIssues + " workforce attendance concern" + plural(attendanceIssues)
                            + (attendanceIssues == 1 ? " needs" : " need") + " workforce governance coordination today.",
                    "Attendance",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.REVIEW,
                    LocalDateTime.now(),
                    "Pending HR Correction Approval"
            ));
        }

        addOvertimeReviewNotification(notifications, "Overtime requests require review");
        addAttendanceAdjustmentReviewNotification(notifications);
    }

    private void addAdminNotifications(List<OperationalNotification> notifications, Employee viewer) {
        notifications.add(new OperationalNotification(
                "Access governance available",
                "Admin authority is focused on Access Management and system governance. Payroll and workforce approvals remain with Finance and HR.",
                "System",
                OperationalNotification.Severity.INFO,
                OperationalNotification.Priority.INFORMATIONAL,
                LocalDateTime.now(),
                "System Governance"
        ));
    }

    private void addPayrollNotifications(List<OperationalNotification> notifications) {
        List<String> cutoffs = payrollService.getAvailableCutoffs();
        if (!cutoffs.isEmpty()) {
            String cutoff = cutoffs.get(cutoffs.size() - 1);
            try {
                LocalDate start = LocalDate.parse(payrollService.getCutoffStartDate(cutoff));
                LocalDate end = LocalDate.parse(payrollService.getCutoffEndDate(cutoff));
                PayrollReadinessReport report = payrollReadinessService.evaluateReadiness(start, end);
                if (report.getStatus() != PayrollReadinessReport.Status.READY) {
                    notifications.add(new OperationalNotification(
                            "Payroll readiness requires review",
                            "Latest cutoff " + cutoff + " has " + report.getIssueCount()
                                    + " readiness item" + plural(report.getIssueCount()) + ".",
                    "Payroll",
                    report.getBlockedCount() > 0
                            ? OperationalNotification.Severity.CRITICAL
                            : OperationalNotification.Severity.WARNING,
                    report.getBlockedCount() > 0
                            ? OperationalNotification.Priority.CRITICAL
                            : OperationalNotification.Priority.ACTION_REQUIRED,
                    LocalDateTime.now(),
                    "Needs Review"
            ));
                }

                PayrollLifecycleStatus lifecycle = payrollLifecycleService.getLifecycleStatus(
                        cutoff, start.toString(), end.toString());
                if (lifecycle.isProcessed()) {
                    notifications.add(new OperationalNotification(
                            "Payroll cutoff already processed",
                            cutoff + " has saved payroll history records.",
                            "Payroll",
                            OperationalNotification.Severity.INFO,
                            OperationalNotification.Priority.INFORMATIONAL,
                            LocalDateTime.now(),
                            "Processed"
                    ));
                }
            } catch (Exception ignored) {
            }
        }

        int approvedOvertime = overtimeService.getApprovedRequests().size();
        if (approvedOvertime > 0) {
            notifications.add(new OperationalNotification(
                    "Approved overtime visible",
                    approvedOvertime + " approved overtime request" + plural(approvedOvertime)
                            + " are visible for future payroll readiness only.",
                    "Overtime",
                    OperationalNotification.Severity.INFO,
                    OperationalNotification.Priority.INFORMATIONAL,
                    LocalDateTime.now(),
                    "Approved"
            ));
        }
    }

    private void addOvertimeReviewNotification(List<OperationalNotification> notifications, String title) {
        int pendingOvertime = overtimeService.getPendingRequestsForReview().size();
        if (pendingOvertime > 0) {
            notifications.add(new OperationalNotification(
                    title,
                    pendingOvertime + " overtime request" + plural(pendingOvertime)
                            + (pendingOvertime == 1 ? " is" : " are") + " pending final HR approval.",
                    "Overtime",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.ACTION_REQUIRED,
                    LocalDateTime.now(),
                    "Pending HR Approval"
            ));
        }
    }

    private void addAttendanceAdjustmentReviewNotification(List<OperationalNotification> notifications) {
        int pendingAdjustments = attendanceAdjustmentService.getPendingRequestsForReview().size();
        if (pendingAdjustments > 0) {
            notifications.add(new OperationalNotification(
                    "Attendance corrections require review",
                    pendingAdjustments + " attendance correction request" + plural(pendingAdjustments)
                            + (pendingAdjustments == 1 ? " is" : " are") + " pending HR correction approval.",
                    "Attendance",
                    OperationalNotification.Severity.WARNING,
                    OperationalNotification.Priority.ACTION_REQUIRED,
                    LocalDateTime.now(),
                    "Pending HR Correction Approval"
            ));
        }
    }

    private List<Integer> getEmployeeIds(List<Employee> employees) {
        List<Integer> ids = new ArrayList<>();
        for (Employee employee : employees) {
            ids.add(employee.getEmployeeId());
        }
        return ids;
    }

    private OperationalNotification.Severity toSeverity(AttendanceAwareness awareness) {
        return switch (awareness.getSeverity()) {
            case CRITICAL -> OperationalNotification.Severity.CRITICAL;
            case WARNING -> OperationalNotification.Severity.WARNING;
            case INFO -> OperationalNotification.Severity.INFO;
            default -> OperationalNotification.Severity.SUCCESS;
        };
    }

    private OperationalNotification.Priority toPriority(AttendanceAwareness awareness) {
        return switch (awareness.getSeverity()) {
            case CRITICAL -> OperationalNotification.Priority.CRITICAL;
            case WARNING -> OperationalNotification.Priority.ACTION_REQUIRED;
            case INFO -> OperationalNotification.Priority.REVIEW;
            default -> OperationalNotification.Priority.INFORMATIONAL;
        };
    }

    private int priorityRank(OperationalNotification.Priority priority) {
        if (priority == null) {
            return 0;
        }

        return switch (priority) {
            case CRITICAL -> 3;
            case ACTION_REQUIRED -> 2;
            case REVIEW -> 1;
            case INFORMATIONAL -> 0;
        };
    }

    private List<OperationalNotification> dedupeNotifications(List<OperationalNotification> notifications) {
        Map<String, OperationalNotification> unique = new LinkedHashMap<>();
        for (OperationalNotification notification : notifications) {
            String key = safe(notification.getCategory()) + "|"
                    + safe(notification.getTitle()) + "|"
                    + safe(notification.getMessage());
            unique.putIfAbsent(key, notification);
        }
        return new ArrayList<>(unique.values());
    }

    private String getEmptyStateTitle(Employee viewer) {
        String role = viewer == null ? "" : viewer.getRole();
        if (roleAccessService.isHR(role)) return "No workforce issues require HR review";
        if (roleAccessService.isSupervisor(role)) return "Your team operations are currently stable";
        if (roleAccessService.isFinance(role)) return "No payroll readiness issues detected";
        if (roleAccessService.isAdmin(role)) return "No access or system governance alerts";
        if (roleAccessService.isIT(role)) return "No access or system alerts";
        return "Your workforce updates are clear";
    }

    private String getEmptyStateMessage(Employee viewer) {
        String role = viewer == null ? "" : viewer.getRole();
        if (roleAccessService.isHR(role)) return "Leave, attendance, and workforce coordination queues are clear.";
        if (roleAccessService.isSupervisor(role)) return "No team attendance, schedule, or leave follow-ups are active.";
        if (roleAccessService.isFinance(role)) return "Payroll readiness and lifecycle checks are clear.";
        if (roleAccessService.isAdmin(role)) return "Access operations are stable. Workforce approvals and payroll remain with HR and Finance.";
        if (roleAccessService.isIT(role)) return "Access operations are stable.";
        return "No leave, attendance, or schedule updates need your attention.";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String plural(int count) {
        return count == 1 ? "" : "s";
    }

    private static class CacheEntry {
        private final long createdAtMs;
        private final List<OperationalNotification> notifications;

        private CacheEntry(long createdAtMs, List<OperationalNotification> notifications) {
            this.createdAtMs = createdAtMs;
            this.notifications = new ArrayList<>(notifications);
        }
    }
}
