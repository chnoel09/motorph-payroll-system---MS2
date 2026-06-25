/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.EmployeeGovernmentId;
import com.mycompany.oop.model.GovernmentIdType;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.model.User;
import com.mycompany.oop.repository.EmployeeDatabaseRepository;
import com.mycompany.oop.repository.EmployeeGovernmentIdDatabaseRepository;
import com.mycompany.oop.repository.EmployeeGovernmentIdRepository;
import com.mycompany.oop.repository.EmployeeRepository;
import com.mycompany.oop.repository.UserDatabaseRepository;
import com.mycompany.oop.repository.UserRepository;
import com.mycompany.oop.util.PasswordUtil;

public class EmployeeService {

    private EmployeeRepository repository;
    private EmployeeGovernmentIdRepository governmentIdRepository;
    private UserRepository userRepository;
    private AuditService auditService;

    public EmployeeService() {
         this.repository = new EmployeeDatabaseRepository();
         this.governmentIdRepository = new EmployeeGovernmentIdDatabaseRepository();
         this.userRepository = new UserDatabaseRepository();
         this.auditService = new AuditService();
    }

    public List<Employee> getAllEmployees() {
        List<Employee> employees = repository.getAllEmployees();
        for (Employee employee : employees) {
            enrichWithAuth(employee);
        }
        return employees;
    }

    public int alignReportingHierarchyFromLegacy() {
        List<Employee> employees = repository.getAllEmployees();
        Map<String, Employee> employeesByLegacyName = buildUniqueEmployeeNameIndex(employees);
        int updated = 0;

        for (Employee employee : employees) {
            if (employee == null) {
                continue;
            }

            Integer resolvedSupervisorId = resolveSupervisorEmployeeId(employee, employeesByLegacyName);
            String resolvedSupervisorName = resolveSupervisorName(resolvedSupervisorId, employees);
            String desiredLegacyName = resolvedSupervisorName == null ? "N/A" : resolvedSupervisorName;

            boolean supervisorChanged = !Objects.equals(employee.getSupervisorEmployeeId(), resolvedSupervisorId);
            boolean legacyNameChanged = !Objects.equals(normalizeLegacyName(employee.getImmediateSupervisor()),
                    normalizeLegacyName(desiredLegacyName));

            if (supervisorChanged || legacyNameChanged) {
                employee.setSupervisorEmployeeId(resolvedSupervisorId);
                employee.setImmediateSupervisor(desiredLegacyName);
                repository.updateEmployee(employee);
                updated++;
            }
        }

        return updated;
    }

    public Integer resolveSupervisorEmployeeId(Employee employee) {
        return resolveSupervisorEmployeeId(employee, buildUniqueEmployeeNameIndex(getAllEmployees()));
    }

    public List<Employee> getTeamMembersForSupervisor(int supervisorEmployeeId) {
        if (supervisorEmployeeId <= 0) {
            return java.util.Collections.emptyList();
        }

        return repository.findBySupervisorEmployeeId(supervisorEmployeeId);
    }

    public Employee findSupervisorFor(Employee employee, List<Employee> employees) {
        if (employee == null) {
            return null;
        }

        Integer supervisorEmployeeId = employee.getSupervisorEmployeeId();
        if (supervisorEmployeeId == null) {
            supervisorEmployeeId = resolveSupervisorEmployeeId(employee, safeEmployees(employees));
        }

        if (supervisorEmployeeId == null) {
            return null;
        }

        for (Employee candidate : safeEmployees(employees)) {
            if (candidate != null && candidate.getEmployeeId() == supervisorEmployeeId) {
                return candidate;
            }
        }

        return enrichWithAuth(repository.findEmployee(supervisorEmployeeId));
    }

    public List<Employee> findDirectReports(Employee supervisor, List<Employee> employees) {
        List<Employee> reports = new ArrayList<>();
        if (supervisor == null) {
            return reports;
        }

        for (Employee employee : safeEmployees(employees)) {
            Integer supervisorId = employee == null ? null : employee.getSupervisorEmployeeId();
            if (supervisorId != null && supervisorId == supervisor.getEmployeeId() && isActiveEmployee(employee)) {
                reports.add(employee);
            }
        }

        reports.sort(Comparator
                .comparing(Employee::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Employee::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return reports;
    }

    public String getDepartmentName(Employee employee) {
        if (employee == null || employee.getDepartmentId() == null) {
            return "Not assigned";
        }
        return getDepartments().getOrDefault(employee.getDepartmentId(), "Not assigned");
    }

    public Map<Integer, String> getDepartments() {
        return repository.getDepartments();
    }

    public boolean hasAssignedTeam(int supervisorEmployeeId) {
        if (supervisorEmployeeId <= 0) {
            return false;
        }

        for (Employee employee : repository.findBySupervisorEmployeeId(supervisorEmployeeId)) {
            if (isActiveEmployee(employee)) {
                return true;
            }
        }
        return false;
    }

    public List<Employee> getTeamOperationsEmployees(Employee viewer) {
        if (viewer == null) {
            return java.util.Collections.emptyList();
        }

        RoleAccessService roleAccessService = new RoleAccessService();
        String role = viewer.getRole();

        if (roleAccessService.isHR(role)) {
            return getAllEmployees();
        }

        List<Employee> teamMembers = getTeamMembersForSupervisor(viewer.getEmployeeId());
        for (Employee employee : teamMembers) {
            if (isActiveEmployee(employee)) {
                return teamMembers;
            }
        }

        return java.util.Collections.emptyList();
    }

    public List<Employee> getSupervisorCandidates(int excludeEmployeeId) {
        List<Employee> candidates = new ArrayList<>();

        for (Employee employee : getAllEmployees()) {
            if (employee == null || employee.getEmployeeId() == excludeEmployeeId) {
                continue;
            }

            if (isSupervisorCandidate(employee)) {
                candidates.add(employee);
            }
        }

        candidates.sort(Comparator
                .comparing(Employee::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Employee::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return candidates;
    }

    public Employee findById(int id) {
        return enrichWithGovernmentIds(enrichWithAuth(repository.findEmployee(id)));
    }

    public Employee findById(String idText) {
        return findById(Integer.parseInt(idText));
    }

    public void addEmployee(Employee employee) {
        syncSupervisorFields(employee);
        validateEmployeeForSave(employee, 0);
        int newEmployeeId = repository.addEmployee(employee);
        if (newEmployeeId <= 0) {
            throw new IllegalStateException("Employee was not created because the database did not return an employee ID.");
        }

        syncGovernmentIds(employee, newEmployeeId);
        int newUserId = createUserAccount(employee, newEmployeeId);
        assignUserRole(newUserId, employee.getRole());

        auditService.logAction(null, "EMPLOYEE_CREATED", "employees", String.valueOf(newEmployeeId));
        if (employee.getSupervisorEmployeeId() != null) {
            auditService.logAction(null, AuditService.SUPERVISOR_ASSIGNED, "employees", String.valueOf(newEmployeeId));
        }
    }

    public void updateEmployee(Employee employee) {
        Employee existingEmployee = employee == null ? null : repository.findEmployee(employee.getEmployeeId());
        Integer previousSupervisorId = existingEmployee == null ? null : existingEmployee.getSupervisorEmployeeId();

        syncSupervisorFields(employee);
        validateEmployeeForSave(employee, employee == null ? 0 : employee.getEmployeeId());
        repository.updateEmployee(employee);
        syncGovernmentIds(employee);
        auditService.logAction(null, "EMPLOYEE_UPDATED", "employees", String.valueOf(employee.getEmployeeId()));
        logSupervisorAssignmentChange(employee.getEmployeeId(), previousSupervisorId, employee.getSupervisorEmployeeId());
    }

    public Employee enrichWithGovernmentIds(Employee employee) {
        if (employee == null || employee.getEmployeeId() <= 0) {
            return employee;
        }

        for (EmployeeGovernmentId governmentId : governmentIdRepository.findByEmployeeId(employee.getEmployeeId())) {
            if (governmentId == null) {
                continue;
            }

            String type = normalizeGovernmentIdType(governmentId.getGovernmentIdType());
            String number = governmentId.getGovernmentIdNumber();

            switch (type) {
                case "SSS" -> employee.setSssNumber(number);
                case "PHILHEALTH" -> employee.setPhilhealthNumber(number);
                case "TIN" -> employee.setTinNumber(number);
                case "PAGIBIG" -> employee.setPagibigNumber(number);
                default -> {
                    // Ignore unknown government ID types so future IDs do not break employee editing.
                }
            }
        }

        return employee;
    }

    private Employee enrichWithAuth(Employee employee) {
        if (employee == null || employee.getEmployeeId() <= 0) {
            return employee;
        }

        User user = userRepository.findByEmployeeId(employee.getEmployeeId());
        if (user == null) {
            return employee;
        }

        employee.setUsername(user.getUsername());
        employee.setPassword(user.getPasswordHash());

        List<Role> roles = userRepository.findRolesByUserId(user.getUserId());
        if (!roles.isEmpty()) {
            employee.setRole(roles.get(0).getRoleName());
        }

        return employee;
    }

    public void deleteEmployee(int id) {
        repository.deleteEmployee(id);
        auditService.logAction(null, "EMPLOYEE_DELETED", "employees", String.valueOf(id));
    }
    
    public java.util.Map<String, Integer> getUserRoleCounts() {
        java.util.Map<String, Integer> roleCounts = new java.util.HashMap<>();

        for (User user : userRepository.findAllUsers()) {
            List<Role> roles = userRepository.findRolesByUserId(user.getUserId());
            if (roles.isEmpty()) {
                continue;
            }
            String role = roles.get(0).getRoleName();
            roleCounts.put(role, roleCounts.getOrDefault(role, 0) + 1);
        }

        return roleCounts;
    }
    
        public int getTotalUsers() {
            return userRepository.findAllUsers().size();
        }

        public boolean changeRole(int id, String newRole) {
            User user = userRepository.findByEmployeeId(id);
            Role role = userRepository.findRoleByName(newRole);

            if (user == null || role == null) {
                return false;
            }

            userRepository.replaceUserRole(user.getUserId(), role.getRoleId());
            auditService.logAction(null, "USER_ROLE_CHANGED", "users", String.valueOf(user.getUserId()));
            return true;
        }

        public boolean resetPassword(int id, String newPass) {
            User user = userRepository.findByEmployeeId(id);

            if (user == null || newPass == null || newPass.trim().isEmpty()) {
                return false;
            }

            userRepository.updateUserPasswordHash(user.getUserId(), PasswordUtil.hash(newPass.trim()));
            auditService.logAction(null, "PASSWORD_RESET", "users", String.valueOf(user.getUserId()));
            return true;
        }
        
        public int getNextEmployeeId() {
            return repository.getNextEmployeeId();
        }
        
        public boolean emailExists(String email, int excludeEmployeeId) {
            return repository.emailExists(email, excludeEmployeeId);
        }

        public boolean usernameExists(String username, int excludeEmployeeId) {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }

            User user = userRepository.findByUsername(username.trim());
            return user != null && user.getEmployeeId() != excludeEmployeeId;
        }

        private void syncGovernmentIds(Employee employee) {
            if (employee == null || employee.getEmployeeId() <= 0) {
                return;
            }

            syncGovernmentIds(employee, employee.getEmployeeId());
        }

        private void syncGovernmentIds(Employee employee, int employeeId) {
            if (employee == null || employeeId <= 0) {
                return;
            }

            Map<String, EmployeeGovernmentId> existingByType = new HashMap<>();
            for (EmployeeGovernmentId governmentId : governmentIdRepository.findByEmployeeId(employeeId)) {
                if (governmentId == null) {
                    continue;
                }
                existingByType.put(normalizeGovernmentIdType(governmentId.getGovernmentIdType()), governmentId);
            }

            syncGovernmentId(existingByType, employeeId, GovernmentIdType.SSS, employee.getSssNumber());
            syncGovernmentId(existingByType, employeeId, GovernmentIdType.PHILHEALTH, employee.getPhilhealthNumber());
            syncGovernmentId(existingByType, employeeId, GovernmentIdType.TIN, employee.getTinNumber());
            syncGovernmentId(existingByType, employeeId, GovernmentIdType.PAGIBIG, employee.getPagibigNumber());
        }

        private int createUserAccount(Employee employee, int employeeId) {
            User user = new User(
                    0,
                    employeeId,
                    safeText(employee.getUsername()),
                    employee.getPassword(),
                    true
            );

            int userId = userRepository.addUserAndReturnId(user);
            if (userId <= 0) {
                throw new IllegalStateException("Employee account was not created.");
            }
            return userId;
        }

        private void assignUserRole(int userId, String roleName) {
            Role role = userRepository.findRoleByName(roleName);
            if (role == null) {
                throw new IllegalArgumentException("Role does not exist in normalized RBAC: " + safeText(roleName));
            }

            userRepository.replaceUserRole(userId, role.getRoleId());
        }

        private void syncGovernmentId(Map<String, EmployeeGovernmentId> existingByType,
                                      int employeeId,
                                      GovernmentIdType type,
                                      String number) {
            String normalizedType = normalizeGovernmentIdType(type.name());
            EmployeeGovernmentId existing = existingByType.get(normalizedType);
            String cleanNumber = safeText(number);

            if (cleanNumber.isEmpty()) {
                if (existing != null) {
                    governmentIdRepository.deleteGovernmentId(existing.getEmployeeGovId());
                }
                return;
            }

            if (existing == null) {
                governmentIdRepository.addGovernmentId(new EmployeeGovernmentId(
                        0,
                        employeeId,
                        type.name(),
                        cleanNumber
                ));
                return;
            }

            existing.setEmployeeId(employeeId);
            existing.setGovernmentIdType(type.name());
            existing.setGovernmentIdNumber(cleanNumber);
            governmentIdRepository.updateGovernmentId(existing);
        }

        private String normalizeGovernmentIdType(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        }

        private void validateEmployeeForSave(Employee employee, int excludeEmployeeId) {
            if (employee == null) {
                throw new IllegalArgumentException("Employee record is required.");
            }

            validateRequired(employee.getFirstName(), "First name");
            validateRequired(employee.getLastName(), "Last name");
            validateRequired(employee.getPosition(), "Position");
            validateRequired(employee.getEmploymentStatus(), "Employment status");
            validateRequired(employee.getBirthday(), "Birthday");
            validateRequired(employee.getAddress(), "Address");
            validateRequired(employee.getPhoneNumber(), "Phone number");
            validateRequired(employee.getEmail(), "Email");
            validateRequired(employee.getUsername(), "Username");
            validateRequired(employee.getPassword(), "Password");
            validateRequired(employee.getRole(), "Role");

            validateBirthday(employee.getBirthday());
            validateEmail(employee.getEmail());
            validatePhoneNumber(employee.getPhoneNumber());
            validateUsername(employee.getUsername());
            validateRole(employee.getRole());

            validateDigitsOnly(employee.getSssNumber(), "SSS Number");
            validateDigitsOnly(employee.getPhilhealthNumber(), "PhilHealth Number");
            validateDigitsOnly(employee.getTinNumber(), "TIN Number");
            validateDigitsOnly(employee.getPagibigNumber(), "Pag-IBIG Number");

            validateNonNegative(employee.getBasicSalary(), "Basic salary");
            validateNonNegative(employee.getAllowance(), "Allowance");
            validateNonNegative(employee.getHourlyRate(), "Hourly rate");
            validateNonNegative(employee.getRiceSubsidy(), "Rice subsidy");
            validateNonNegative(employee.getPhoneAllowance(), "Phone allowance");
            validateNonNegative(employee.getClothingAllowance(), "Clothing allowance");
            validateNonNegative(employee.getGrossSemiMonthlyRate(), "Gross semi-monthly rate");

            if (employee.getBasicSalary() > 1_000_000) {
                throw new IllegalArgumentException("Basic salary exceeds the allowed maximum.");
            }

            if (emailExists(employee.getEmail(), excludeEmployeeId)) {
                throw new IllegalArgumentException("Email already exists.");
            }

            if (usernameExists(employee.getUsername(), excludeEmployeeId)) {
                throw new IllegalArgumentException("Username already exists.");
            }

            validateSupervisorAssignment(employee, excludeEmployeeId);
        }

        private void validateRequired(String value, String fieldName) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + " is required.");
            }
        }

        private void validateBirthday(String value) {
            if (!value.trim().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                throw new IllegalArgumentException("Birthday must follow YYYY-MM-DD format.");
            }

            try {
                LocalDate birthday = LocalDate.parse(value.trim());

                if (birthday.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Birthday cannot be in the future.");
                }
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Birthday must be a real calendar date.");
            }
        }

        private void validateEmail(String email) {
            if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("Please enter a valid email address.");
            }
        }

        private void validatePhoneNumber(String phoneNumber) {
            if (!phoneNumber.trim().matches("^(09\\d{9}|63\\d{10})$")) {
                throw new IllegalArgumentException("Phone number must be in 09XXXXXXXXX or 63XXXXXXXXXX format.");
            }
        }

        private void validateUsername(String username) {
            if (!username.trim().matches("^[A-Za-z0-9._-]{3,100}$")) {
                throw new IllegalArgumentException(
                        "Username must be 3-100 characters and may contain letters, numbers, dots, underscores, or hyphens.");
            }
        }

        private void validateRole(String role) {
            Set<String> allowedRoles = Set.of("admin", "hr", "finance", "employee", "it", "supervisor", "manager");

            if (!allowedRoles.contains(role.trim().toLowerCase())) {
                throw new IllegalArgumentException("Role must be Admin, HR, Finance, Employee, IT, Supervisor, or Manager.");
            }
        }

        private void validateSupervisorAssignment(Employee employee, int excludeEmployeeId) {
            Integer supervisorEmployeeId = employee.getSupervisorEmployeeId();

            if (supervisorEmployeeId == null) {
                return;
            }

            if (supervisorEmployeeId <= 0) {
                throw new IllegalArgumentException("Reports To must be a valid active supervisor.");
            }

            if (supervisorEmployeeId == excludeEmployeeId
                    || supervisorEmployeeId == employee.getEmployeeId()) {
                throw new IllegalArgumentException("An employee cannot report to themselves.");
            }

            Employee supervisor = enrichWithAuth(repository.findEmployee(supervisorEmployeeId));

            if (!isSupervisorCandidate(supervisor)) {
                logSupervisorValidationFailure(employee, supervisorEmployeeId, supervisor);
                throw new IllegalArgumentException("Reports To must be an active Supervisor, Manager, Team Lead, HR, or Admin employee.");
            }

            int employeeId = excludeEmployeeId > 0 ? excludeEmployeeId : employee.getEmployeeId();
            if (employeeId > 0 && wouldCreateReportingCycle(employeeId, supervisorEmployeeId)) {
                throw new IllegalArgumentException("Invalid reporting assignment: this would create a circular reporting relationship.");
            }
        }

        public boolean wouldCreateReportingCycle(int employeeId, Integer proposedSupervisorEmployeeId) {
            if (employeeId <= 0 || proposedSupervisorEmployeeId == null) {
                return false;
            }
            if (proposedSupervisorEmployeeId == employeeId) {
                return true;
            }

            Set<Integer> visited = new java.util.HashSet<>();
            Integer currentSupervisorId = proposedSupervisorEmployeeId;
            while (currentSupervisorId != null) {
                if (!visited.add(currentSupervisorId)) {
                    return true;
                }
                if (currentSupervisorId == employeeId) {
                    return true;
                }

                Employee supervisor = repository.findEmployee(currentSupervisorId);
                if (supervisor == null) {
                    return false;
                }
                currentSupervisorId = supervisor.getSupervisorEmployeeId();
            }
            return false;
        }

        private boolean isSupervisorCandidate(Employee employee) {
            if (employee == null || !isActiveEmployee(employee)) {
                return false;
            }

            return hasOperationalSupervisorRole(employee.getRole());
        }

        private boolean hasOperationalSupervisorRole(String role) {
            String normalizedRole = normalizeOperationalRole(role);
            return Set.of("supervisor", "manager", "hr", "admin", "teamlead").contains(normalizedRole);
        }

        private String normalizeOperationalRole(String role) {
            if (role == null) {
                return "";
            }
            return role.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        private void logSupervisorValidationFailure(Employee employee, Integer supervisorEmployeeId, Employee supervisor) {
            System.err.println("[EmployeeService] Supervisor validation failed"
                    + " employeeId=" + (employee == null ? "unknown" : employee.getEmployeeId())
                    + " supervisorId=" + supervisorEmployeeId
                    + " supervisorName=" + (supervisor == null ? "not found" : fullName(supervisor))
                    + " supervisorStatus=" + (supervisor == null ? "n/a" : safeText(supervisor.getEmploymentStatus()))
                    + " supervisorRole=" + (supervisor == null ? "n/a" : safeText(supervisor.getRole())));
        }

        private boolean isActiveEmployee(Employee employee) {
            String status = employee.getEmploymentStatus();

            if (status == null || status.trim().isEmpty()) {
                return false;
            }

            String normalizedStatus = status.trim().toLowerCase();
            return !normalizedStatus.contains("inactive")
                    && !normalizedStatus.contains("terminated")
                    && !normalizedStatus.contains("resigned");
        }

        private void logSupervisorAssignmentChange(int employeeId, Integer previousSupervisorId, Integer newSupervisorId) {
            if (Objects.equals(previousSupervisorId, newSupervisorId)) {
                return;
            }

            String action;
            if (previousSupervisorId == null && newSupervisorId != null) {
                action = AuditService.SUPERVISOR_ASSIGNED;
            } else if (previousSupervisorId != null && newSupervisorId == null) {
                action = AuditService.SUPERVISOR_REMOVED;
            } else {
                action = AuditService.SUPERVISOR_CHANGED;
            }

            auditService.logAction(null, action, "employees", String.valueOf(employeeId));
        }

        private List<Employee> safeEmployees(List<Employee> employees) {
            return employees == null ? java.util.Collections.emptyList() : employees;
        }

        private void syncSupervisorFields(Employee employee) {
            if (employee == null) {
                return;
            }

            Integer supervisorEmployeeId = employee.getSupervisorEmployeeId();
            if (supervisorEmployeeId == null) {
                supervisorEmployeeId = resolveSupervisorEmployeeId(employee);
                employee.setSupervisorEmployeeId(supervisorEmployeeId);
            }

            String supervisorName = resolveSupervisorName(supervisorEmployeeId, getAllEmployees());
            employee.setImmediateSupervisor(supervisorName == null ? "N/A" : supervisorName);
        }

        private Integer resolveSupervisorEmployeeId(Employee employee, Map<String, Employee> employeesByLegacyName) {
            if (employee == null) {
                return null;
            }

            Integer supervisorEmployeeId = employee.getSupervisorEmployeeId();
            if (supervisorEmployeeId != null) {
                if (supervisorEmployeeId == employee.getEmployeeId()) {
                    return null;
                }
                return supervisorEmployeeId;
            }

            String legacyName = normalizeLegacyName(employee.getImmediateSupervisor());
            if (legacyName.isBlank() || "n/a".equals(legacyName) || "none".equals(legacyName)
                    || "no assigned supervisor".equals(legacyName)) {
                return null;
            }

            Employee match = employeesByLegacyName.get(legacyName);
            if (match == null || match.getEmployeeId() == employee.getEmployeeId() || !isSupervisorCandidate(match)) {
                return null;
            }
            return match.getEmployeeId();
        }

        private Integer resolveSupervisorEmployeeId(Employee employee, List<Employee> employees) {
            return resolveSupervisorEmployeeId(employee, buildUniqueEmployeeNameIndex(employees));
        }

        private Map<String, Employee> buildUniqueEmployeeNameIndex(List<Employee> employees) {
            Map<String, Employee> byName = new HashMap<>();
            Set<String> ambiguous = new java.util.HashSet<>();

            for (Employee employee : safeEmployees(employees)) {
                if (employee == null) {
                    continue;
                }

                addUniqueName(byName, ambiguous, fullName(employee), employee);
                addUniqueName(byName, ambiguous, lastFirstName(employee), employee);
            }

            for (String key : ambiguous) {
                byName.remove(key);
            }
            return byName;
        }

        private void addUniqueName(Map<String, Employee> byName, Set<String> ambiguous, String name, Employee employee) {
            String normalizedName = normalizeLegacyName(name);
            if (normalizedName.isBlank()) {
                return;
            }

            Employee existing = byName.putIfAbsent(normalizedName, employee);
            if (existing != null && existing.getEmployeeId() != employee.getEmployeeId()) {
                ambiguous.add(normalizedName);
            }
        }

        private String resolveSupervisorName(Integer supervisorEmployeeId, List<Employee> employees) {
            if (supervisorEmployeeId == null) {
                return null;
            }

            for (Employee employee : safeEmployees(employees)) {
                if (employee != null && employee.getEmployeeId() == supervisorEmployeeId) {
                    return fullName(employee);
                }
            }

            Employee supervisor = repository.findEmployee(supervisorEmployeeId);
            return supervisor == null ? null : fullName(supervisor);
        }

        private String fullName(Employee employee) {
            return (safeText(employee.getFirstName()) + " " + safeText(employee.getLastName())).trim();
        }

        private String lastFirstName(Employee employee) {
            return (safeText(employee.getLastName()) + ", " + safeText(employee.getFirstName())).trim();
        }

        private String safeText(String value) {
            return value == null ? "" : value.trim();
        }

        private String normalizeLegacyName(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().replaceAll("\\s+", " ").toLowerCase();
        }

        private void validateDigitsOnly(String value, String fieldName) {
            if (value == null || !value.trim().matches("\\d+")) {
                throw new IllegalArgumentException(fieldName + " must contain digits only.");
            }
        }

        private void validateNonNegative(double value, String fieldName) {
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
        }
    }
