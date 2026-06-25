package com.mycompany.oop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mycompany.oop.model.AuditLog;
import com.mycompany.oop.repository.AuditLogDatabaseRepository;
import com.mycompany.oop.repository.AuditLogRepository;

// Prepared for the future normalized audit logging migration.
// This service is intentionally not wired into the current runtime flow yet.
public class AuditService {

    public static final String SHIFT_CREATED = "SHIFT_CREATED";
    public static final String SHIFT_UPDATED = "SHIFT_UPDATED";
    public static final String SHIFT_DELETED = "SHIFT_DELETED";
    public static final String HOLIDAY_CREATED = "HOLIDAY_CREATED";
    public static final String HOLIDAY_UPDATED = "HOLIDAY_UPDATED";
    public static final String HOLIDAY_DELETED = "HOLIDAY_DELETED";
    public static final String SCHEDULE_BATCH_CREATED = "SCHEDULE_BATCH_CREATED";
    public static final String EMPLOYEE_SCHEDULE_ASSIGNED = "EMPLOYEE_SCHEDULE_ASSIGNED";
    public static final String EMPLOYEE_SCHEDULE_UPDATED = "EMPLOYEE_SCHEDULE_UPDATED";
    public static final String EMPLOYEE_SCHEDULE_DELETED = "EMPLOYEE_SCHEDULE_DELETED";
    public static final String LEAVE_REQUEST_SUBMITTED = "LEAVE_REQUEST_SUBMITTED";
    public static final String SUPERVISOR_ASSIGNED = "SUPERVISOR_ASSIGNED";
    public static final String SUPERVISOR_CHANGED = "SUPERVISOR_CHANGED";
    public static final String SUPERVISOR_REMOVED = "SUPERVISOR_REMOVED";
    public static final String LEAVE_APPROVED = "LEAVE_APPROVED";
    public static final String LEAVE_REJECTED = "LEAVE_REJECTED";
    public static final String OVERTIME_REQUEST_SUBMITTED = "OVERTIME_REQUEST_SUBMITTED";
    public static final String OVERTIME_APPROVED = "OVERTIME_APPROVED";
    public static final String OVERTIME_REJECTED = "OVERTIME_REJECTED";
    public static final String ATTENDANCE_ADJUSTMENT_REQUESTED = "ATTENDANCE_ADJUSTMENT_REQUESTED";
    public static final String ATTENDANCE_ADJUSTMENT_REVIEWED = "ATTENDANCE_ADJUSTMENT_REVIEWED";
    public static final String ATTENDANCE_ADJUSTMENT_CORRECTED = "ATTENDANCE_ADJUSTMENT_CORRECTED";

    private AuditLogRepository auditLogRepository;

    public AuditService() {
        this.auditLogRepository = new AuditLogDatabaseRepository();
    }

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(Integer userId, String action, String tableName, String recordId) {
        try {
            if (auditLogRepository == null) {
                return;
            }

            AuditLog auditLog = new AuditLog(
                    0,
                    userId,
                    action,
                    tableName,
                    recordId,
                    LocalDateTime.now()
            );

            auditLogRepository.addAuditLog(auditLog);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<AuditLog> getAuditLogs() {
        if (auditLogRepository == null) {
            return new ArrayList<>();
        }

        return auditLogRepository.findAll();
    }

    public List<AuditLog> getRecentAuditLogs(int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<AuditLog> auditLogs = new ArrayList<>(getAuditLogs());

        auditLogs.sort(Comparator
                .comparing(AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AuditLog::getAuditId)
                .reversed());

        if (auditLogs.size() <= limit) {
            return auditLogs;
        }

        return new ArrayList<>(auditLogs.subList(0, limit));
    }

    public List<AuditLog> getRecentOperationalAuditLogs(int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<AuditLog> recentLogs = getRecentAuditLogs(Math.max(limit, 12));
        List<AuditLog> operationalLogs = new ArrayList<>();

        for (AuditLog auditLog : recentLogs) {
            if (!"LOGIN_SUCCESS".equals(auditLog.getAction())) {
                operationalLogs.add(auditLog);
            }

            if (operationalLogs.size() == limit) {
                return operationalLogs;
            }
        }

        for (AuditLog auditLog : recentLogs) {
            if (!operationalLogs.contains(auditLog)) {
                operationalLogs.add(auditLog);
            }

            if (operationalLogs.size() == limit) {
                break;
            }
        }

        return operationalLogs;
    }

    public List<AuditLog> getRecentAuditLogsForEmployee(int employeeId, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }

        List<AuditLog> recentLogs = getRecentAuditLogs(25);
        List<AuditLog> employeeLogs = new ArrayList<>();
        String employeeRecordId = String.valueOf(employeeId);

        for (AuditLog auditLog : recentLogs) {
            if (isEmployeeScopedAuditLog(auditLog, employeeRecordId)) {
                employeeLogs.add(auditLog);
            }

            if (employeeLogs.size() == limit) {
                break;
            }
        }

        return employeeLogs;
    }

    private boolean isEmployeeScopedAuditLog(AuditLog auditLog, String employeeRecordId) {
        if (auditLog == null || employeeRecordId == null) {
            return false;
        }

        String action = auditLog.getAction();
        String tableName = auditLog.getTableName();
        String recordId = auditLog.getRecordId();

        if (recordId == null || !recordId.equals(employeeRecordId)) {
            return false;
        }

        return "employees".equalsIgnoreCase(tableName)
                && ("LOGIN_SUCCESS".equals(action)
                || "EMPLOYEE_CREATED".equals(action)
                || "EMPLOYEE_UPDATED".equals(action)
                || SUPERVISOR_ASSIGNED.equals(action)
                || SUPERVISOR_CHANGED.equals(action)
                || SUPERVISOR_REMOVED.equals(action));
    }
}
