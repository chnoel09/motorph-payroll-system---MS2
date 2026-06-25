package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.AuditLog;

// Contract prepared for future normalized audit logging.
public interface AuditLogRepository {

    List<AuditLog> findAll();

    List<AuditLog> findByUserId(int userId);

    void addAuditLog(AuditLog auditLog);
}
