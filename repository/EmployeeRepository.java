/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.mycompany.oop.repository;

import com.mycompany.oop.model.Employee;
// We import Employee because this interface will work with Employee objects

import java.util.List;
import java.util.Map;
// We import List because we will return multiple employees


 // This is a CONTRACT.
 // It defines WHAT operations are allowed for employee data.
 // It does NOT define HOW they work.
 // Any class that implements this interface MUST provide
 // the actual logic for these methods.

public interface EmployeeRepository {

    // CREATE
    int addEmployee(Employee employee);

    // UPDATE
    void updateEmployee(Employee employee);

    // DELETE
    void deleteEmployee(int employeeId);

    // READ
    Employee findEmployee(int employeeId);

    List<Employee> getAllEmployees();

    List<Employee> findBySupervisorEmployeeId(int supervisorEmployeeId);

    Map<Integer, String> getDepartments();

    // VALIDATION SUPPORT
    boolean emailExists(String email, int excludeEmployeeId);

    boolean usernameExists(String username, int excludeEmployeeId);
    
    int getNextEmployeeId();
}
