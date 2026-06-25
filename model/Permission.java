package com.mycompany.oop.model;

// Prepared for the future normalized RBAC permission migration.
public class Permission {

    private int permissionId;
    private String permissionName;
    private String moduleName;
    private String action;
    private String description;

    public Permission() {
    }

    public Permission(int permissionId, String permissionName, String moduleName, String action, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.moduleName = moduleName;
        this.action = action;
        this.description = description;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
