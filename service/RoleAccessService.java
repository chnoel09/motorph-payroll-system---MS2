package com.mycompany.oop.service;

import com.mycompany.oop.model.Employee;
import java.util.ArrayList;
import java.util.List;

// Lightweight runtime access helper using the current Employee.role value as source of truth.
public class RoleAccessService {

    public static final String DASHBOARD_VIEW = "DASH";
    public static final String WORKFORCE_GOVERNANCE_VIEW = "WORKFORCE_GOVERNANCE";
    public static final String EMPLOYEES_VIEW = "EMP";
    public static final String PAYROLL_VIEW = "PAYROLL";
    public static final String PAYROLL_HISTORY_VIEW = "PAYROLL_HISTORY";
    public static final String OPERATIONAL_INBOX_VIEW = "INBOX";
    public static final String LEAVE_REVIEW_VIEW = "LEAVE";
    public static final String SCHEDULING_VIEW = "SCHEDULING";
    public static final String USER_MANAGEMENT_VIEW = "IT";
    public static final String PROFILE_VIEW = "PROFILE";
    public static final String ATTENDANCE_VIEW = "ATTENDANCE";
    public static final String TIMEKEEPING_CALENDAR_VIEW = "TIMEKEEPING";
    public static final String TEAM_OPERATIONS_VIEW = "TEAM_OPERATIONS";
    public static final String PAYSLIP_VIEW = "PAYSLIP";
    public static final String FILE_LEAVE_VIEW = "FILE";
    public static final String OVERTIME_VIEW = "OVERTIME";
    public static final String ATTENDANCE_ADJUSTMENT_VIEW = "ATTENDANCE_ADJUSTMENT";
    public static final String ORG_CHART_VIEW = "ORG_CHART";

    public boolean canAccessView(String role, String viewId) {
        if (viewId == null || viewId.trim().isEmpty()) {
            return false;
        }

        return getAllowedViewIds(role).contains(viewId.trim());
    }

    public boolean canAccessView(Employee employee, String viewId) {
        if (viewId == null || viewId.trim().isEmpty()) {
            return false;
        }

        return getAllowedViewIds(employee).contains(viewId.trim());
    }

    public List<String> getAllowedViewIds(Employee employee) {
        if (employee == null) {
            return getAllowedViewIds("");
        }

        List<String> viewIds = getAllowedViewIds(employee.getRole());
        EmployeeService employeeService = new EmployeeService();
        boolean hasTeamAuthority = employeeService.hasAssignedTeam(employee.getEmployeeId());
        boolean supervisorWorkspaceRole = isSupervisor(employee.getRole());

        if (supervisorWorkspaceRole && hasTeamAuthority && !viewIds.contains(TEAM_OPERATIONS_VIEW)) {
            viewIds.add(TEAM_OPERATIONS_VIEW);
        }
        if (supervisorWorkspaceRole && hasTeamAuthority && !viewIds.contains(SCHEDULING_VIEW)) {
            viewIds.add(SCHEDULING_VIEW);
        }
        if (supervisorWorkspaceRole && hasTeamAuthority && !viewIds.contains(ATTENDANCE_ADJUSTMENT_VIEW)) {
            viewIds.add(ATTENDANCE_ADJUSTMENT_VIEW);
        }

        return viewIds;
    }

    public List<String> getAllowedViewIds(String role) {
        List<String> viewIds = new ArrayList<>();
        addBaseEmployeeViews(viewIds);

        if (isAdmin(role)) {
            viewIds.add(USER_MANAGEMENT_VIEW);
            return viewIds;
        }

        if (isHR(role)) {
            viewIds.add(WORKFORCE_GOVERNANCE_VIEW);
            viewIds.add(EMPLOYEES_VIEW);
            viewIds.add(LEAVE_REVIEW_VIEW);
            viewIds.add(ATTENDANCE_ADJUSTMENT_VIEW);
            viewIds.add(SCHEDULING_VIEW);
            return viewIds;
        }

        if (isFinance(role)) {
            viewIds.add(PAYROLL_VIEW);
            viewIds.add(PAYROLL_HISTORY_VIEW);
            return viewIds;
        }

        if (isIT(role)) {
            viewIds.add(USER_MANAGEMENT_VIEW);
            return viewIds;
        }

        if (isSupervisor(role)) {
            return viewIds;
        }

        if (isEmployee(role)) {
            return viewIds;
        }

        viewIds.add(PROFILE_VIEW);
        return viewIds;
    }

    private void addBaseEmployeeViews(List<String> viewIds) {
        viewIds.add(DASHBOARD_VIEW);
        viewIds.add(PROFILE_VIEW);
        viewIds.add(OPERATIONAL_INBOX_VIEW);
        viewIds.add(ATTENDANCE_VIEW);
        viewIds.add(TIMEKEEPING_CALENDAR_VIEW);
        viewIds.add(ORG_CHART_VIEW);
        viewIds.add(PAYSLIP_VIEW);
        viewIds.add(FILE_LEAVE_VIEW);
        viewIds.add(OVERTIME_VIEW);
    }

    public boolean isAdmin(String role) {
        return "admin".equals(normalizeRole(role));
    }

    public boolean isFinance(String role) {
        return "finance".equals(normalizeRole(role));
    }

    public boolean isHR(String role) {
        return "hr".equals(normalizeRole(role));
    }

    public boolean isEmployee(String role) {
        return "employee".equals(normalizeRole(role));
    }

    public boolean isIT(String role) {
        return "it".equals(normalizeRole(role));
    }

    public boolean isSupervisor(String role) {
        return "supervisor".equals(normalizeRole(role));
    }

    public boolean canGeneratePayroll(String role) {
        // Payroll authority is intentionally restricted to Finance; Admin is system governance, not payroll ownership.
        return isFinance(role);
    }

    public boolean canApproveLeave(String role) {
        return isHR(role);
    }

    public boolean canApproveOvertime(String role) {
        return isHR(role);
    }

    public boolean canProcessAttendanceAdjustments(String role) {
        return isHR(role);
    }

    public boolean canFinalizeWorkforceApprovals(String role) {
        return isHR(role);
    }

    public boolean canManageEmployees(String role) {
        return isHR(role);
    }

    public boolean canAccessPayrollHistory(String role) {
        return isFinance(role);
    }

    public boolean canManageUsers(String role) {
        return isAdmin(role) || isIT(role);
    }

    public boolean canManageScheduling(String role) {
        return isHR(role);
    }

    public boolean canResetUserPassword(String role) {
        return isAdmin(role) || isIT(role);
    }

    public boolean canChangeUserRole(String role) {
        return isAdmin(role);
    }

    public boolean isElevatedRole(String role) {
        return isAdmin(role) || isIT(role);
    }

    public String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase();
    }
}
