package com.mycompany.oop.service;

import java.util.List;

import com.mycompany.oop.model.Permission;
import com.mycompany.oop.model.Role;
import com.mycompany.oop.repository.UserRepository;

// Permission helper for normalized RBAC checks. RoleAccessService still preserves
// employee-role compatibility while auth fallback is active.
public class AccessControlService {

    private UserRepository userRepository;

    public AccessControlService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean hasPermission(int userId, String permissionName) {
        if (userRepository == null || permissionName == null || permissionName.trim().isEmpty()) {
            return false;
        }

        try {
            List<Permission> permissions = userRepository.findPermissionsByUserId(userId);

            for (Permission permission : permissions) {
                String storedPermissionName = permission.getPermissionName();

                if (storedPermissionName != null
                        && storedPermissionName.equalsIgnoreCase(permissionName.trim())) {
                    return true;
                }
            }

        } catch (RuntimeException e) {
            return false;
        }

        return false;
    }

    public boolean hasRole(int userId, String roleName) {
        if (userRepository == null || roleName == null || roleName.trim().isEmpty()) {
            return false;
        }

        try {
            List<Role> roles = userRepository.findRolesByUserId(userId);

            for (Role role : roles) {
                String storedRoleName = role.getRoleName();

                if (storedRoleName != null && storedRoleName.equalsIgnoreCase(roleName.trim())) {
                    return true;
                }
            }

        } catch (RuntimeException e) {
            return false;
        }

        return false;
    }
}
