package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.EmployeeGovernmentId;
import com.mycompany.oop.model.Permission;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.model.User;
import com.mycompany.oop.repository.EmployeeGovernmentIdRepository;
import com.mycompany.oop.repository.EmployeeRepository;
import com.mycompany.oop.repository.UserRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    private static final Logger LOGGER = Logger.getLogger(EmployeeServiceTest.class.getName());

    private EmployeeService service;
    private FakeEmployeeRepository employeeRepository;
    private FakeUserRepository userRepository;
    private FakeGovernmentIdRepository governmentIdRepository;

    @BeforeEach
    void setUp() {
        service = new EmployeeService();
        employeeRepository = new FakeEmployeeRepository();
        userRepository = new FakeUserRepository();
        governmentIdRepository = new FakeGovernmentIdRepository();

        Employee alice = ServiceTestSupport.employee(10008, "Alice", "Reyes", "Employee");
        alice.setEmail("alice.reyes@motorph.test");
        employeeRepository.employees.put(alice.getEmployeeId(), alice);

        Employee hr = ServiceTestSupport.employee(10006, "Andrea", "Valdez", "HR");
        hr.setEmail("andrea.valdez@motorph.test");
        employeeRepository.employees.put(hr.getEmployeeId(), hr);

        userRepository.usersByEmployeeId.put(10008,
                new User(8, 10008, "alice.r", "hash", true));
        userRepository.usersByEmployeeId.put(10006,
                new User(6, 10006, "andreamae.v", "hash", true));
        userRepository.rolesByUserId.put(8, List.of(new Role(1, "Employee")));
        userRepository.rolesByUserId.put(6, List.of(new Role(3, "HR")));

        governmentIdRepository.idsByEmployeeId.put(10008, List.of(
                new EmployeeGovernmentId(1, 10008, "SSS", "3312345678"),
                new EmployeeGovernmentId(2, 10008, "PHILHEALTH", "123456789012"),
                new EmployeeGovernmentId(3, 10008, "TIN", "123456789"),
                new EmployeeGovernmentId(4, 10008, "PAGIBIG", "987654321012")));

        ServiceTestSupport.setField(service, "repository", employeeRepository);
        ServiceTestSupport.setField(service, "userRepository", userRepository);
        ServiceTestSupport.setField(service, "governmentIdRepository", governmentIdRepository);
    }

    @Test
    void getAllEmployeesReturnsEmployeesWithAuthFields() {
        LOGGER.info("Checking employee list auth enrichment.");

        List<Employee> employees = service.getAllEmployees();

        assertFalse(employees.isEmpty());
        Employee alice = employees.stream()
                .filter(employee -> employee.getEmployeeId() == 10008)
                .findFirst()
                .orElse(null);

        assertNotNull(alice);
        assertEquals("alice.r", alice.getUsername());
        assertEquals("Employee", alice.getRole());
    }

    @Test
    void findByIdReturnsEmployeeWithGovernmentIds() {
        LOGGER.info("Checking employee government ID enrichment.");

        Employee employee = service.findById(10008);

        assertNotNull(employee);
        assertEquals("Alice", employee.getFirstName());
        assertEquals("alice.r", employee.getUsername());
        assertEquals("3312345678", employee.getSssNumber());
        assertEquals("123456789012", employee.getPhilhealthNumber());
        assertEquals("123456789", employee.getTinNumber());
        assertEquals("987654321012", employee.getPagibigNumber());
    }

    @Test
    void emailExistsDelegatesToEmployeeRepositoryWithExclusion() {
        LOGGER.info("Checking employee email uniqueness behavior.");

        assertTrue(service.emailExists("alice.reyes@motorph.test", 0));
        assertFalse(service.emailExists("alice.reyes@motorph.test", 10008));
        assertFalse(service.emailExists("new.employee@motorph.test", 0));
    }

    @Test
    void usernameExistsUsesNormalizedUserRepository() {
        LOGGER.info("Checking username uniqueness behavior.");

        assertTrue(service.usernameExists("alice.r", 0));
        assertFalse(service.usernameExists("alice.r", 10008));
        assertFalse(service.usernameExists("", 0));
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
            return employees.values().stream()
                    .anyMatch(employee -> employee.getEmail() != null
                            && employee.getEmail().equalsIgnoreCase(email)
                            && employee.getEmployeeId() != excludeEmployeeId);
        }

        @Override
        public boolean usernameExists(String username, int excludeEmployeeId) {
            return false;
        }

        @Override
        public int getNextEmployeeId() {
            return 10009;
        }
    }

    private static class FakeGovernmentIdRepository implements EmployeeGovernmentIdRepository {
        private final Map<Integer, List<EmployeeGovernmentId>> idsByEmployeeId = new HashMap<>();

        @Override
        public List<EmployeeGovernmentId> findByEmployeeId(int employeeId) {
            return idsByEmployeeId.getOrDefault(employeeId, Collections.emptyList());
        }

        @Override
        public void addGovernmentId(EmployeeGovernmentId governmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateGovernmentId(EmployeeGovernmentId governmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteGovernmentId(int employeeGovId) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Map<Integer, User> usersByEmployeeId = new HashMap<>();
        private final Map<Integer, List<Role>> rolesByUserId = new HashMap<>();

        @Override
        public User findByUsername(String username) {
            return usersByEmployeeId.values().stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByEmployeeId(int employeeId) {
            return usersByEmployeeId.get(employeeId);
        }

        @Override
        public List<User> findAllUsers() {
            return List.copyOf(usersByEmployeeId.values());
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
            return findByUsername(username) != null;
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
