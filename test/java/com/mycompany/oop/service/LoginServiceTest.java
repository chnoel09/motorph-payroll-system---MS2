package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Permission;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.model.User;
import com.mycompany.oop.repository.EmployeeRepository;
import com.mycompany.oop.repository.UserRepository;
import com.mycompany.oop.util.PasswordUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private static final Logger LOGGER = Logger.getLogger(LoginServiceTest.class.getName());

    private LoginService service;

    @BeforeEach
    void setUp() {
        service = new LoginService();

        FakeEmployeeRepository employeeRepository = new FakeEmployeeRepository();
        employeeRepository.employees.put(10001, ServiceTestSupport.employee(10001, "Manuel", "Garcia", ""));
        employeeRepository.employees.put(10002, ServiceTestSupport.employee(10002, "Inactive", "User", ""));

        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.usersByUsername.put("manuel.g",
                new User(1, 10001, "manuel.g", PasswordUtil.hash("1234"), true));
        userRepository.rolesByUserId.put(1, List.of(new Role(5, "Admin")));
        userRepository.usersByUsername.put("inactive.user",
                new User(2, 10002, "inactive.user", PasswordUtil.hash("1234"), false));
        userRepository.rolesByUserId.put(2, List.of(new Role(1, "Employee")));

        ServiceTestSupport.setField(service, "repository", employeeRepository);
        ServiceTestSupport.setField(service, "userRepository", userRepository);
    }

    @Test
    void validLoginReturnsEmployeeWithResolvedRole() {
        LOGGER.info("Checking successful normalized login.");

        Employee employee = service.login(" Manuel.G ", " 1234 ");

        assertEquals(10001, employee.getEmployeeId());
        assertEquals("manuel.g", employee.getUsername());
        assertEquals("Admin", employee.getRole());
    }

    @Test
    void loginRejectsInvalidPassword() {
        LOGGER.info("Checking invalid password login rejection.");

        assertNull(service.login("manuel.g", "wrong-password"));
    }

    @Test
    void loginRejectsMissingOrInactiveUser() {
        LOGGER.info("Checking missing and inactive login rejection.");

        assertNull(service.login("missing.user", "1234"));
        assertNull(service.login("inactive.user", "1234"));
    }

    @Test
    void loginHonorsRequiredRoleWhenProvided() {
        LOGGER.info("Checking role-restricted login.");

        assertEquals("Admin", service.login("manuel.g", "1234", "admin").getRole());
        assertNull(service.login("manuel.g", "1234", "finance"));
    }

    private static class FakeEmployeeRepository implements EmployeeRepository {
        private final Map<Integer, Employee> employees = new HashMap<>();

        @Override
        public int addEmployee(Employee employee) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateEmployee(Employee employee) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteEmployee(int employeeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Employee findEmployee(int employeeId) {
            return employees.get(employeeId);
        }

        @Override
        public List<Employee> getAllEmployees() {
            return List.copyOf(employees.values());
        }

        @Override
        public List<Employee> findBySupervisorEmployeeId(int supervisorEmployeeId) {
            return Collections.emptyList();
        }

        @Override
        public Map<Integer, String> getDepartments() {
            return Collections.emptyMap();
        }

        @Override
        public boolean emailExists(String email, int excludeEmployeeId) {
            return false;
        }

        @Override
        public boolean usernameExists(String username, int excludeEmployeeId) {
            return false;
        }

        @Override
        public int getNextEmployeeId() {
            return 10003;
        }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Map<String, User> usersByUsername = new HashMap<>();
        private final Map<Integer, List<Role>> rolesByUserId = new HashMap<>();

        @Override
        public User findByUsername(String username) {
            return usersByUsername.get(username);
        }

        @Override
        public User findByEmployeeId(int employeeId) {
            return usersByUsername.values().stream()
                    .filter(user -> user.getEmployeeId() == employeeId)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<User> findAllUsers() {
            return List.copyOf(usersByUsername.values());
        }

        @Override
        public List<Role> findRolesByUserId(int userId) {
            return rolesByUserId.getOrDefault(userId, Collections.emptyList());
        }

        @Override
        public List<Role> findAllRoles() {
            return Collections.emptyList();
        }

        @Override
        public Role findRoleByName(String roleName) {
            return null;
        }

        @Override
        public List<Permission> findPermissionsByUserId(int userId) {
            return Collections.emptyList();
        }

        @Override
        public boolean userExists(String username) {
            return usersByUsername.containsKey(username);
        }

        @Override
        public void addUser(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int addUserAndReturnId(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateUser(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateUserPasswordHash(int userId, String passwordHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceUserRole(int userId, int roleId) {
            throw new UnsupportedOperationException();
        }
    }
}
