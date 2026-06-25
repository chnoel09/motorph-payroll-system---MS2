package com.mycompany.oop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RegularEmployeeTest {

    @Test
    void constructorStoresCoreEmployeeFields() {
        RegularEmployee employee = new RegularEmployee(
                10003,
                "Ana",
                "Reyes",
                "HR Officer",
                "Active",
                28000,
                1200,
                95,
                "ana",
                "hashed-password",
                "HR");

        assertEquals(10003, employee.getEmployeeId());
        assertEquals("Ana", employee.getFirstName());
        assertEquals("Reyes", employee.getLastName());
        assertEquals("HR Officer", employee.getPosition());
        assertEquals("Active", employee.getEmploymentStatus());
        assertEquals("ana", employee.getUsername());
        assertEquals("HR", employee.getRole());
    }

    @Test
    void payrollTemplateMethodsUseRegularEmployeeDefaults() {
        RegularEmployee employee = new RegularEmployee(
                10004,
                "Ben",
                "Garcia",
                "Technician",
                "Active",
                20000,
                1000,
                85,
                "ben",
                "hashed-password",
                "Employee");

        assertEquals(21000.00, employee.computeGrossSalary(), 0.001);
        assertEquals(0.00, employee.computeDeductions(), 0.001);
        assertEquals(21000.00, employee.computeNetSalary(), 0.001);
    }

    @Test
    void negativeSalaryAllowanceAndRateAreNormalizedToZero() {
        RegularEmployee employee = new RegularEmployee(
                10005,
                "Cara",
                "Lim",
                "Clerk",
                "Active",
                -1,
                -1,
                -1,
                "cara",
                "hashed-password",
                "Employee");

        assertEquals(0.00, employee.getBasicSalary(), 0.001);
        assertEquals(0.00, employee.getAllowance(), 0.001);
        assertEquals(0.00, employee.getHourlyRate(), 0.001);
        assertEquals(0.00, employee.computeGrossSalary(), 0.001);
    }
}
