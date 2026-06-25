package com.mycompany.oop.model;

import java.time.LocalDateTime;

// Prepared for the future normalized audit_logs migration.
public class AuditLog {

    private int auditId;
    private Integer userId;
    private String action;
    private String tableName;
    private String recordId;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(int auditId, Integer userId, String action, String tableName,
            String recordId, LocalDateTime createdAt) {
        this.auditId = auditId;
        this.userId = userId;
        this.action = action;
        this.tableName = tableName;
        this.recordId = recordId;
        this.createdAt = createdAt;
    }

    public int getAuditId() {
        return auditId;
    }

    public void setAuditId(int auditId) {
        this.auditId = auditId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
