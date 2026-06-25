package com.mycompany.oop.model;

// Prepared for the future employee_government_ids normalization migration.
public class EmployeeGovernmentId {

    private int employeeGovId;
    private int employeeId;
    private String governmentIdType;
    private String governmentIdNumber;

    public EmployeeGovernmentId() {
    }

    public EmployeeGovernmentId(int employeeGovId, int employeeId, String governmentIdType, String governmentIdNumber) {
        this.employeeGovId = employeeGovId;
        this.employeeId = employeeId;
        this.governmentIdType = governmentIdType;
        this.governmentIdNumber = governmentIdNumber;
    }

    public int getEmployeeGovId() {
        return employeeGovId;
    }

    public void setEmployeeGovId(int employeeGovId) {
        this.employeeGovId = employeeGovId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getGovernmentIdType() {
        return governmentIdType;
    }

    public void setGovernmentIdType(String governmentIdType) {
        this.governmentIdType = governmentIdType;
    }

    public String getGovernmentIdNumber() {
        return governmentIdNumber;
    }

    public void setGovernmentIdNumber(String governmentIdNumber) {
        this.governmentIdNumber = governmentIdNumber;
    }
}
