package com.mycompany.oop.model;

import java.time.LocalDateTime;

public class WorkforcePayrollReadiness {

    public static final String STATUS_OPEN_WORKFORCE_ISSUES = "OPEN_WORKFORCE_ISSUES";
    public static final String STATUS_PENDING_SUPERVISOR_REVIEW = "PENDING_SUPERVISOR_REVIEW";
    public static final String STATUS_RETURNED_TO_SUPERVISOR = "RETURNED_TO_SUPERVISOR";
    public static final String STATUS_SUPERVISOR_CLEARED = "SUPERVISOR_CLEARED";
    public static final String STATUS_PENDING_HR_VALIDATION = "PENDING_HR_VALIDATION";
    public static final String STATUS_HR_VALIDATED = "HR_VALIDATED";
    public static final String STATUS_ENDORSED_TO_FINANCE = "ENDORSED_TO_FINANCE";
    public static final String STATUS_FINANCE_VALIDATED = "FINANCE_VALIDATED";
    public static final String STATUS_PAYROLL_PROCESSED = "PAYROLL_PROCESSED";
    public static final String STATUS_PAYROLL_LOCKED = "PAYROLL_LOCKED";

    public static final String OWNER_SUPERVISOR = "SUPERVISOR";
    public static final String OWNER_HR = "HR";
    public static final String OWNER_FINANCE = "FINANCE";

    private int readinessId;
    private int payrollPeriodId;
    private int employeeId;
    private String readinessStatus;
    private String currentOwnerRole;
    private Integer supervisorEmployeeId;
    private Integer supervisorClearedBy;
    private LocalDateTime supervisorClearedAt;
    private Integer hrValidatedBy;
    private LocalDateTime hrValidatedAt;
    private String hrRemarks;
    private Integer financeValidatedBy;
    private LocalDateTime financeValidatedAt;
    private String financeRemarks;
    private Integer returnedBy;
    private String returnedToRole;
    private LocalDateTime returnedAt;
    private String returnReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WorkforcePayrollReadiness() {
    }

    public WorkforcePayrollReadiness(int readinessId, int payrollPeriodId, int employeeId,
            String readinessStatus, String currentOwnerRole, Integer supervisorEmployeeId) {
        this.readinessId = readinessId;
        this.payrollPeriodId = payrollPeriodId;
        this.employeeId = employeeId;
        this.readinessStatus = readinessStatus;
        this.currentOwnerRole = currentOwnerRole;
        this.supervisorEmployeeId = supervisorEmployeeId;
    }

    public int getReadinessId() {
        return readinessId;
    }

    public void setReadinessId(int readinessId) {
        this.readinessId = readinessId;
    }

    public int getPayrollPeriodId() {
        return payrollPeriodId;
    }

    public void setPayrollPeriodId(int payrollPeriodId) {
        this.payrollPeriodId = payrollPeriodId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public String getCurrentOwnerRole() {
        return currentOwnerRole;
    }

    public void setCurrentOwnerRole(String currentOwnerRole) {
        this.currentOwnerRole = currentOwnerRole;
    }

    public Integer getSupervisorEmployeeId() {
        return supervisorEmployeeId;
    }

    public void setSupervisorEmployeeId(Integer supervisorEmployeeId) {
        this.supervisorEmployeeId = supervisorEmployeeId;
    }

    public Integer getSupervisorClearedBy() {
        return supervisorClearedBy;
    }

    public void setSupervisorClearedBy(Integer supervisorClearedBy) {
        this.supervisorClearedBy = supervisorClearedBy;
    }

    public LocalDateTime getSupervisorClearedAt() {
        return supervisorClearedAt;
    }

    public void setSupervisorClearedAt(LocalDateTime supervisorClearedAt) {
        this.supervisorClearedAt = supervisorClearedAt;
    }

    public Integer getHrValidatedBy() {
        return hrValidatedBy;
    }

    public void setHrValidatedBy(Integer hrValidatedBy) {
        this.hrValidatedBy = hrValidatedBy;
    }

    public LocalDateTime getHrValidatedAt() {
        return hrValidatedAt;
    }

    public void setHrValidatedAt(LocalDateTime hrValidatedAt) {
        this.hrValidatedAt = hrValidatedAt;
    }

    public String getHrRemarks() {
        return hrRemarks;
    }

    public void setHrRemarks(String hrRemarks) {
        this.hrRemarks = hrRemarks;
    }

    public Integer getFinanceValidatedBy() {
        return financeValidatedBy;
    }

    public void setFinanceValidatedBy(Integer financeValidatedBy) {
        this.financeValidatedBy = financeValidatedBy;
    }

    public LocalDateTime getFinanceValidatedAt() {
        return financeValidatedAt;
    }

    public void setFinanceValidatedAt(LocalDateTime financeValidatedAt) {
        this.financeValidatedAt = financeValidatedAt;
    }

    public String getFinanceRemarks() {
        return financeRemarks;
    }

    public void setFinanceRemarks(String financeRemarks) {
        this.financeRemarks = financeRemarks;
    }

    public Integer getReturnedBy() {
        return returnedBy;
    }

    public void setReturnedBy(Integer returnedBy) {
        this.returnedBy = returnedBy;
    }

    public String getReturnedToRole() {
        return returnedToRole;
    }

    public void setReturnedToRole(String returnedToRole) {
        this.returnedToRole = returnedToRole;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
