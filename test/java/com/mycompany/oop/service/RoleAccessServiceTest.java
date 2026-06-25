package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class RoleAccessServiceTest {

    private static final Logger LOGGER = Logger.getLogger(RoleAccessServiceTest.class.getName());

    private final RoleAccessService service = new RoleAccessService();

    @Test
    void adminCanManageUsersButCannotOwnPayrollProcessing() {
        LOGGER.info("Checking Admin dashboard access rules.");

        assertTrue(service.canAccessView("Admin", RoleAccessService.DASHBOARD_VIEW));
        assertTrue(service.canAccessView("Admin", RoleAccessService.USER_MANAGEMENT_VIEW));
        assertTrue(service.canManageUsers("Admin"));
        assertTrue(service.canChangeUserRole("Admin"));
        assertFalse(service.canGeneratePayroll("Admin"));
    }

    @Test
    void hrCanManageWorkforceButNotPayrollProcessing() {
        LOGGER.info("Checking HR dashboard access rules.");

        assertTrue(service.canAccessView("HR", RoleAccessService.EMPLOYEES_VIEW));
        assertTrue(service.canAccessView("HR", RoleAccessService.LEAVE_REVIEW_VIEW));
        assertTrue(service.canAccessView("HR", RoleAccessService.SCHEDULING_VIEW));
        assertTrue(service.canManageEmployees("HR"));
        assertTrue(service.canApproveLeave("HR"));
        assertFalse(service.canGeneratePayroll("HR"));
    }

    @Test
    void financeCanAccessPayrollButNotEmployeeManagement() {
        LOGGER.info("Checking Finance dashboard access rules.");

        assertTrue(service.canAccessView("Finance", RoleAccessService.PAYROLL_VIEW));
        assertTrue(service.canAccessView("Finance", RoleAccessService.PAYROLL_HISTORY_VIEW));
        assertTrue(service.canGeneratePayroll("Finance"));
        assertTrue(service.canAccessPayrollHistory("Finance"));
        assertFalse(service.canManageEmployees("Finance"));
    }

    @Test
    void supervisorKeepsEmployeeWorkspaceByDefault() {
        LOGGER.info("Checking Supervisor default access rules.");

        assertTrue(service.canAccessView("Supervisor", RoleAccessService.DASHBOARD_VIEW));
        assertTrue(service.canAccessView("Supervisor", RoleAccessService.ATTENDANCE_VIEW));
        assertFalse(service.canAccessView("Supervisor", RoleAccessService.PAYROLL_VIEW));
        assertFalse(service.canManageScheduling("Supervisor"));
    }

    @Test
    void employeeAndUnknownRolesOnlyReceiveSelfServiceAccess() {
        LOGGER.info("Checking Employee and fallback access rules.");

        assertTrue(service.canAccessView("Employee", RoleAccessService.PROFILE_VIEW));
        assertTrue(service.canAccessView("Employee", RoleAccessService.PAYSLIP_VIEW));
        assertFalse(service.canAccessView("Employee", RoleAccessService.EMPLOYEES_VIEW));
        assertFalse(service.canAccessView("Employee", RoleAccessService.USER_MANAGEMENT_VIEW));

        assertTrue(service.canAccessView("Unknown", RoleAccessService.PROFILE_VIEW));
        assertFalse(service.canAccessView("Unknown", RoleAccessService.PAYROLL_VIEW));
        assertFalse(service.canAccessView((String) null, ""));
    }
}
