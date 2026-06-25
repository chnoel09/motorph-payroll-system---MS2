package com.mycompany.oop.service;

import java.util.List;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.model.User;
import com.mycompany.oop.repository.EmployeeDatabaseRepository;
import com.mycompany.oop.repository.EmployeeRepository;
import com.mycompany.oop.repository.UserDatabaseRepository;
import com.mycompany.oop.repository.UserRepository;
import com.mycompany.oop.util.PasswordUtil;

public class LoginService {

    private EmployeeRepository repository;
    private UserRepository userRepository;

    public LoginService() {
        this.repository = new EmployeeDatabaseRepository();
        this.userRepository = new UserDatabaseRepository();
    }

    // Default login (no role restriction)
    public Employee login(String username, String password) {
        return login(username, password, null);
    }

    // Login with optional role validation
    public Employee login(String username, String password, String requiredRole) {

        if (username == null || password == null) {
            return null;
        }

        username = username.trim().toLowerCase();
        password = password.trim();

        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }

        return loginWithNormalizedUser(username, password, requiredRole);
    }

    private Employee loginWithNormalizedUser(String username, String password, String requiredRole) {
        if (userRepository == null) {
            return null;
        }

        try {
            User user = userRepository.findByUsername(username);
            if (user == null || !user.isActive()) {
                return null;
            }

            if (!PasswordUtil.verify(password, user.getPasswordHash())) {
                return null;
            }

            Employee employee = findEmployeeById(user.getEmployeeId());
            if (employee == null) {
                return null;
            }

            String normalizedRole = resolvePrimaryRole(user.getUserId());
            if (normalizedRole == null || normalizedRole.isBlank()) {
                return null;
            }

            employee.setUsername(user.getUsername());
            employee.setRole(normalizedRole);

            if (requiredRole == null || roleMatches(employee.getRole(), requiredRole)) {
                return employee;
            }

        } catch (RuntimeException e) {
            return null;
        }

        return null;
    }

    private Employee findEmployeeById(int employeeId) {
        return repository.findEmployee(employeeId);
    }

    private String resolvePrimaryRole(int userId) {
        List<Role> roles = userRepository.findRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return roles.get(0).getRoleName();
    }

    private boolean roleMatches(String actualRole, String expectedRole) {
        return actualRole != null && expectedRole != null
                && actualRole.trim().equalsIgnoreCase(expectedRole.trim());
    }
}
