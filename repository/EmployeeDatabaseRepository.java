package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.RegularEmployee;

public class EmployeeDatabaseRepository implements EmployeeRepository {

    @Override
    public List<Employee> getAllEmployees() {
        List<Employee> employees = new ArrayList<>();

        String sql = "SELECT " + getEmployeeSelectColumns() + " FROM employees";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapRowToEmployee(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return employees;
    }

    @Override
    public Employee findEmployee(int employeeId) {
        String sql = "SELECT " + getEmployeeSelectColumns() + " FROM employees WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEmployee(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int addEmployee(Employee employee) {
        String sql = """
            INSERT INTO employees (
                department_id, first_name, last_name, birthday, address, phone_number, email,
                status, position, supervisor_employee_id,
                basic_salary, rice_subsidy, phone_allowance, clothing_allowance,
                gross_semi_monthly_rate, hourly_rate
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setNullableInteger(stmt, 1, employee.getDepartmentId());
            stmt.setString(2, employee.getFirstName());
            stmt.setString(3, employee.getLastName());
            stmt.setString(4, employee.getBirthday());
            stmt.setString(5, employee.getAddress());
            stmt.setString(6, employee.getPhoneNumber());
            stmt.setString(7, employee.getEmail());
            stmt.setString(8, employee.getEmploymentStatus());
            stmt.setString(9, employee.getPosition());
            setNullableInteger(stmt, 10, employee.getSupervisorEmployeeId());
            int index = 11;
            stmt.setDouble(index++, employee.getBasicSalary());
            stmt.setDouble(index++, employee.getRiceSubsidy());
            stmt.setDouble(index++, employee.getPhoneAllowance());
            stmt.setDouble(index++, employee.getClothingAllowance());
            stmt.setDouble(index++, employee.getGrossSemiMonthlyRate());
            stmt.setDouble(index, employee.getHourlyRate());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            throw new SQLException("Creating employee did not return a generated employee_id.");

        } catch (SQLException e) {
            throw mapEmployeeSaveException(e, "add");
        }
    }
    
    @Override
    public void updateEmployee(Employee employee) {
        String sql = """
            UPDATE employees SET
                department_id = ?,
                first_name = ?,
                last_name = ?,
                birthday = ?,
                address = ?,
                phone_number = ?,
                email = ?,
                status = ?,
                position = ?,
                supervisor_employee_id = ?,
                basic_salary = ?,
                rice_subsidy = ?,
                phone_allowance = ?,
                clothing_allowance = ?,
                gross_semi_monthly_rate = ?,
                hourly_rate = ?
            WHERE employee_id = ?
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableInteger(stmt, 1, employee.getDepartmentId());
            stmt.setString(2, employee.getFirstName());
            stmt.setString(3, employee.getLastName());
            stmt.setString(4, employee.getBirthday());
            stmt.setString(5, employee.getAddress());
            stmt.setString(6, employee.getPhoneNumber());
            stmt.setString(7, employee.getEmail());
            stmt.setString(8, employee.getEmploymentStatus());
            stmt.setString(9, employee.getPosition());
            setNullableInteger(stmt, 10, employee.getSupervisorEmployeeId());
            int index = 11;
            stmt.setDouble(index++, employee.getBasicSalary());
            stmt.setDouble(index++, employee.getRiceSubsidy());
            stmt.setDouble(index++, employee.getPhoneAllowance());
            stmt.setDouble(index++, employee.getClothingAllowance());
            stmt.setDouble(index++, employee.getGrossSemiMonthlyRate());
            stmt.setDouble(index++, employee.getHourlyRate());
            stmt.setInt(index, employee.getEmployeeId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw mapEmployeeSaveException(e, "update");
        }
    }

    @Override
    public void deleteEmployee(int employeeId) {
        String sql = "DELETE FROM employees WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete employee.", e);
        }
    }
    
    @Override
    public int getNextEmployeeId() {

        String sql = """
            SELECT AUTO_INCREMENT
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'employees'
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("AUTO_INCREMENT");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public boolean emailExists(String email, int excludeEmployeeId) {

        String sql = """
            SELECT COUNT(*)
            FROM employees
            WHERE email = ?
            AND employee_id != ?
        """;

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            stmt.setInt(2, excludeEmployeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean usernameExists(String username, int excludeEmployeeId) {
        // Username uniqueness now belongs to users.username. Account management
        // will move this validation to UserDatabaseRepository in the next stage.
        return false;
    }    

    @Override
    public Map<Integer, String> getDepartments() {
        Map<Integer, String> departments = new LinkedHashMap<>();
        String sql = "SELECT department_id, department_name FROM departments ORDER BY department_name";

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                departments.put(rs.getInt("department_id"), rs.getString("department_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return departments;
    }

    @Override
    public List<Employee> findBySupervisorEmployeeId(int supervisorEmployeeId) {
        List<Employee> employees = new ArrayList<>();

        if (!hasColumn("employees", "supervisor_employee_id")) {
            return employees;
        }

        String sql = """
            SELECT 
                %s
            FROM employees
            WHERE supervisor_employee_id = ?
            ORDER BY last_name, first_name
            """.formatted(getEmployeeSelectColumns());

        try (Connection conn = DatabaseConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, supervisorEmployeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(mapRowToEmployee(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return employees;
    }

    private RuntimeException mapEmployeeSaveException(SQLException exception, String action) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        String sqlState = exception.getSQLState() == null ? "" : exception.getSQLState();

        if ("23000".equals(sqlState) || message.contains("duplicate")) {
            if (message.contains("email")) {
                return new RuntimeException("Email already exists.", exception);
            }

            return new RuntimeException("Employee record violates a unique database constraint.", exception);
        }

        if ("add".equals(action)) {
            return new RuntimeException("Failed to add employee. Please check all required fields.", exception);
        }

        return new RuntimeException("Failed to update employee. Please check all required fields.", exception);
    }

    private String getEmployeeSelectColumns() {
        return """
                employee_id,
                department_id,
                supervisor_employee_id,
                first_name,
                last_name,
                birthday,
                address,
                phone_number,
                email,
                status,
                position,
                basic_salary,
                rice_subsidy,
                phone_allowance,
                clothing_allowance,
                gross_semi_monthly_rate,
                hourly_rate
                """;
    }
    
        private Employee mapRowToEmployee(ResultSet rs) throws SQLException {

        double totalAllowance =
                rs.getDouble("rice_subsidy")
              + rs.getDouble("phone_allowance")
              + rs.getDouble("clothing_allowance");

        RegularEmployee employee = new RegularEmployee(
                rs.getInt("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("position"),
                rs.getString("status"),
                rs.getDouble("basic_salary"),
                totalAllowance,
                rs.getDouble("hourly_rate"),
                null,
                null,
                null
        );

        employee.setBirthday(rs.getString("birthday"));
        employee.setAddress(rs.getString("address"));
        employee.setPhoneNumber(rs.getString("phone_number"));
        employee.setEmail(rs.getString("email"));
        employee.setImmediateSupervisor(null);
        if (hasColumn(rs, "department_id")) {
            employee.setDepartmentId(getNullableInteger(rs, "department_id"));
        }
        if (hasColumn(rs, "supervisor_employee_id")) {
            employee.setSupervisorEmployeeId(getNullableInteger(rs, "supervisor_employee_id"));
        }

        employee.setRiceSubsidy(rs.getDouble("rice_subsidy"));
        employee.setPhoneAllowance(rs.getDouble("phone_allowance"));
        employee.setClothingAllowance(rs.getDouble("clothing_allowance"));
        employee.setGrossSemiMonthlyRate(rs.getDouble("gross_semi_monthly_rate"));

        return employee;
    }

    private boolean hasColumn(String tableName, String columnName) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return false;
            }

            String catalog = conn.getCatalog();
            try (ResultSet rs = conn.getMetaData().getColumns(catalog, null, tableName, columnName)) {
                if (rs.next()) {
                    return true;
                }
            }

            try (ResultSet rs = conn.getMetaData().getColumns(catalog, null, tableName, columnName.toUpperCase())) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }
}
