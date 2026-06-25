package com.mycompany.oop.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.Permission;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.model.User;

// Repository for normalized RBAC login and access management.
public class UserDatabaseRepository implements UserRepository {

    @Override
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT
                user_id,
                employee_id,
                username,
                password_hash,
                active
            FROM users
            WHERE username = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username.trim());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToUser(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public User findByEmployeeId(int employeeId) {
        String sql = """
            SELECT
                user_id,
                employee_id,
                username,
                password_hash,
                active
            FROM users
            WHERE employee_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapRowToUser(rs);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();

        String sql = """
            SELECT
                user_id,
                employee_id,
                username,
                password_hash,
                active
            FROM users
            ORDER BY username
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return users;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    @Override
    public List<Role> findRolesByUserId(int userId) {
        List<Role> roles = new ArrayList<>();

        String sql = """
            SELECT
                r.role_id,
                r.role_name
            FROM roles r
            INNER JOIN user_roles ur ON r.role_id = ur.role_id
            WHERE ur.user_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return roles;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        roles.add(new Role(
                                rs.getInt("role_id"),
                                rs.getString("role_name")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return roles;
    }

    @Override
    public List<Role> findAllRoles() {
        List<Role> roles = new ArrayList<>();

        String sql = """
            SELECT role_id, role_name
            FROM roles
            ORDER BY role_name
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return roles;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(new Role(
                            rs.getInt("role_id"),
                            rs.getString("role_name")
                    ));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return roles;
    }

    @Override
    public Role findRoleByName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return null;
        }

        String sql = """
            SELECT role_id, role_name
            FROM roles
            WHERE LOWER(role_name) = LOWER(?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return null;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, roleName.trim());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new Role(
                                rs.getInt("role_id"),
                                rs.getString("role_name")
                        );
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public List<Permission> findPermissionsByUserId(int userId) {
        List<Permission> permissions = new ArrayList<>();

        String sql = """
            SELECT DISTINCT
                p.permission_id,
                p.permission_name,
                p.module_name,
                p.action,
                p.description
            FROM permissions p
            INNER JOIN role_permissions rp ON p.permission_id = rp.permission_id
            INNER JOIN user_roles ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return permissions;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        permissions.add(new Permission(
                                rs.getInt("permission_id"),
                                rs.getString("permission_name"),
                                rs.getString("module_name"),
                                rs.getString("action"),
                                rs.getString("description")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return permissions;
    }

    @Override
    public boolean userExists(String username) {
        return findByUsername(username) != null;
    }

    @Override
    public void addUser(User user) {
        addUserAndReturnId(user);
    }

    @Override
    public int addUserAndReturnId(User user) {
        if (user == null) {
            return 0;
        }

        String sql = """
            INSERT INTO users (
                employee_id,
                username,
                password_hash,
                active
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return 0;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, user.getEmployeeId());
                stmt.setString(2, user.getUsername());
                stmt.setString(3, user.getPasswordHash());
                stmt.setBoolean(4, user.isActive());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void updateUser(User user) {
        if (user == null) {
            return;
        }

        String sql = """
            UPDATE users SET
                employee_id = ?,
                username = ?,
                password_hash = ?,
                active = ?
            WHERE user_id = ?
            """;

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getEmployeeId());
                stmt.setString(2, user.getUsername());
                stmt.setString(3, user.getPasswordHash());
                stmt.setBoolean(4, user.isActive());
                stmt.setInt(5, user.getUserId());
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateUserPasswordHash(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, passwordHash);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void replaceUserRole(int userId, int roleId) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return;
            }

            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM user_roles WHERE user_id = ?");
                 PreparedStatement insertStmt = conn.prepareStatement(
                         "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                deleteStmt.setInt(1, userId);
                deleteStmt.executeUpdate();

                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, roleId);
                insertStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getInt("employee_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getBoolean("active")
        );
    }
}
