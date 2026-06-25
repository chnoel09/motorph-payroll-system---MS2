package com.mycompany.oop.model;

// Prepared for the future normalized RBAC authentication migration.
public class User {

    private int userId;
    private int employeeId;
    private String username;
    private String passwordHash;
    private boolean active;

    public User() {
    }

    public User(int userId, int employeeId, String username, String passwordHash, boolean active) {
        this.userId = userId;
        this.employeeId = employeeId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
