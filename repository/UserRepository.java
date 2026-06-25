package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.Permission;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.model.User;

// Contract for normalized authentication/RBAC tables. Employee auth remains as fallback.
public interface UserRepository {

    User findByUsername(String username);

    User findByEmployeeId(int employeeId);

    List<User> findAllUsers();

    List<Role> findRolesByUserId(int userId);

    List<Role> findAllRoles();

    Role findRoleByName(String roleName);

    List<Permission> findPermissionsByUserId(int userId);

    boolean userExists(String username);

    void addUser(User user);

    int addUserAndReturnId(User user);

    void updateUser(User user);

    void updateUserPasswordHash(int userId, String passwordHash);

    void replaceUserRole(int userId, int roleId);
}
