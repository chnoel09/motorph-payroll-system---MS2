package com.mycompany.oop.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Employee implements Payables {

    private int employeeId;          
    private String firstName;        
    private String lastName;         
    private String position;         
    private String employmentStatus; 
    private double basicSalary;      
    private double allowance;        
    private double hourlyRate;       
    private String username;         
    private String password;         
    private String role;    
    
    // Profile fields loaded from normalized employee-related tables.
    private String birthday;
    private String address;
    private String phoneNumber;
    private String email;
    private String immediateSupervisor;
    private Integer departmentId;
    private Integer supervisorEmployeeId;

    private double riceSubsidy;
    private double phoneAllowance;
    private double clothingAllowance;

    private double grossSemiMonthlyRate;

    private String sssNumber;
    private String philhealthNumber;
    private String tinNumber;
    private String pagibigNumber;
    
    // Constructor
    public Employee(
        int employeeId,
        String firstName,
        String lastName,
        String position,
        String employmentStatus,
        double basicSalary,
        double allowance,
        double hourlyRate,
        String username,
        String password,
        String role
    ){
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;

        this.position = position;
        this.employmentStatus = employmentStatus;

        this.basicSalary = basicSalary >= 0 ? basicSalary : 0;
        this.allowance = allowance >= 0 ? allowance : 0;
        this.hourlyRate = hourlyRate >= 0 ? hourlyRate : 0;

        this.username = username;
        this.password = password;
        this.role = role;
    }

    // ================= GETTERS =================

    public int getEmployeeId() {
        return employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPosition() {
        return position;
    }    

    public String getEmploymentStatus() {
        return employmentStatus;
    } 

    public double getBasicSalary() {
        return basicSalary;
    } 

    public double getAllowance() {
        return allowance;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }
    
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
    
    // Extended getters
    public String getBirthday() { 
        return birthday; 
    }

    public String getAddress() { 
        return address; 
    }

    public String getPhoneNumber() { 
        return phoneNumber; 
    }

    public String getEmail() { 
        return email; 
    }

    public String getImmediateSupervisor() { 
        return immediateSupervisor; 
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public Integer getSupervisorEmployeeId() {
        return supervisorEmployeeId;
    }

    public double getRiceSubsidy() { 
        return riceSubsidy; 
    }

    public double getPhoneAllowance() { 
        return phoneAllowance; 
    }

    public double getClothingAllowance() { 
        return clothingAllowance; 
    }

    public double getGrossSemiMonthlyRate() { 
        return grossSemiMonthlyRate; 
    }

    public String getSssNumber() { 
        return sssNumber; 
    }

    public String getPhilhealthNumber() { 
        return philhealthNumber; 
    }

    public String getTinNumber() { 
        return tinNumber; 
    }

    public String getPagibigNumber() { 
        return pagibigNumber; 
    }

    public List<EmployeeGovernmentId> getGovernmentIds() {
        List<EmployeeGovernmentId> governmentIds = new ArrayList<>(4);
        addGovernmentId(governmentIds, GovernmentIdType.SSS, sssNumber);
        addGovernmentId(governmentIds, GovernmentIdType.PHILHEALTH, philhealthNumber);
        addGovernmentId(governmentIds, GovernmentIdType.TIN, tinNumber);
        addGovernmentId(governmentIds, GovernmentIdType.PAGIBIG, pagibigNumber);
        return governmentIds;
    }

    private void addGovernmentId(List<EmployeeGovernmentId> governmentIds, GovernmentIdType type, String number) {
        if (number != null && !number.isBlank()) {
            governmentIds.add(new EmployeeGovernmentId(0, employeeId, type.name(), number));
        }
    }
   

    // ================= SETTERS =================

    public void setPosition(String position) {
        if (position != null && !position.isEmpty()) {
            this.position = position;
        }
    }

    public void setEmploymentStatus(String employmentStatus) {
        if (employmentStatus != null && !employmentStatus.isEmpty()) {
            this.employmentStatus = employmentStatus;
        }
    }

    public void setBasicSalary(Double basicSalary) {
        if (basicSalary != null && basicSalary >= 0) {
            this.basicSalary = basicSalary;
        }
    }

    public void setAllowance(Double allowance) {
        if (allowance != null && allowance >= 0) {
            this.allowance = allowance;
        }
    }
    
    public void setHourlyRate(Double hourlyRate) {
        if (hourlyRate != null && hourlyRate >= 0) {
            this.hourlyRate = hourlyRate;
        }
    }

    public void setUsername(String username) {
        if (username != null && !username.isEmpty()) {
            this.username = username;
        }
    }

    public void setPassword(String password) {
        if (password != null && !password.isEmpty()) {
            this.password = password;
        }
    }       
    
    public void setRole(String role) {
        if (role != null && !role.isEmpty()) {
            this.role = role;
        }
    }

    // Extended setters
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setImmediateSupervisor(String immediateSupervisor) {
        this.immediateSupervisor = immediateSupervisor;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public void setSupervisorEmployeeId(Integer supervisorEmployeeId) {
        this.supervisorEmployeeId = supervisorEmployeeId;
    }

    public void setRiceSubsidy(double riceSubsidy) {
        this.riceSubsidy = riceSubsidy;
    }

    public void setPhoneAllowance(double phoneAllowance) {
        this.phoneAllowance = phoneAllowance;
    }

    public void setClothingAllowance(double clothingAllowance) {
        this.clothingAllowance = clothingAllowance;
    }

    public void setGrossSemiMonthlyRate(double grossSemiMonthlyRate) {
        this.grossSemiMonthlyRate = grossSemiMonthlyRate;
    }

    public void setSssNumber(String sssNumber) {
        this.sssNumber = sssNumber;
    }

    public void setPhilhealthNumber(String philhealthNumber) {
        this.philhealthNumber = philhealthNumber;
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = tinNumber;
    }

    public void setPagibigNumber(String pagibigNumber) {
        this.pagibigNumber = pagibigNumber;
    }

    // ================= PAYROLL TEMPLATE METHODS =================

    @Override
    public abstract double computeGrossSalary();

    @Override
    public abstract double computeDeductions();

    @Override
    public double computeNetSalary() {
        return computeGrossSalary() - computeDeductions();
    }
}
