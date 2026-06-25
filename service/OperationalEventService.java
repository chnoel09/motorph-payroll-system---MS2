package com.mycompany.oop.service;

import com.mycompany.oop.model.OperationalEvent;
import com.mycompany.oop.model.OperationalNotification;
import com.mycompany.oop.model.PersistentNotification;
import com.mycompany.oop.model.User;
import com.mycompany.oop.repository.NotificationDatabaseRepository;
import com.mycompany.oop.repository.NotificationRepository;
import com.mycompany.oop.repository.OperationalEventDatabaseRepository;
import com.mycompany.oop.repository.OperationalEventRepository;
import com.mycompany.oop.repository.UserDatabaseRepository;
import com.mycompany.oop.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OperationalEventService {

    public static final String LEAVE_SUBMITTED = "LEAVE_SUBMITTED";
    public static final String LEAVE_APPROVED = "LEAVE_APPROVED";
    public static final String LEAVE_REJECTED = "LEAVE_REJECTED";
    public static final String OVERTIME_SUBMITTED = "OVERTIME_SUBMITTED";
    public static final String OVERTIME_APPROVED = "OVERTIME_APPROVED";
    public static final String OVERTIME_REJECTED = "OVERTIME_REJECTED";
    public static final String ATTENDANCE_ADJUSTMENT_REQUESTED = "ATTENDANCE_ADJUSTMENT_REQUESTED";
    public static final String ATTENDANCE_ADJUSTMENT_REVIEWED = "ATTENDANCE_ADJUSTMENT_REVIEWED";
    public static final String ATTENDANCE_ADJUSTMENT_CORRECTED = "ATTENDANCE_ADJUSTMENT_CORRECTED";
    public static final String PAYROLL_PERIOD_CREATED = "PAYROLL_PERIOD_CREATED";
    public static final String WORKFORCE_REVIEW_OPENED = "WORKFORCE_REVIEW_OPENED";
    public static final String WORKFORCE_READY_FOR_HR = "WORKFORCE_READY_FOR_HR";
    public static final String WORKFORCE_REVIEW_PENDING = "WORKFORCE_REVIEW_PENDING";
    public static final String WORKFORCE_HR_VALIDATED = "WORKFORCE_HR_VALIDATED";
    public static final String WORKFORCE_RETURNED_TO_SUPERVISOR = "WORKFORCE_RETURNED_TO_SUPERVISOR";
    public static final String WORKFORCE_ENDORSED_TO_FINANCE = "WORKFORCE_ENDORSED_TO_FINANCE";
    public static final String PAYROLL_READINESS_CONFIRMED = "PAYROLL_READINESS_CONFIRMED";
    public static final String PAYROLL_PROCESSED = "PAYROLL_PROCESSED";

    private final OperationalEventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final RoleAccessService roleAccessService;
    private final UserRepository userRepository;

    public OperationalEventService() {
        this.eventRepository = new OperationalEventDatabaseRepository();
        this.notificationRepository = new NotificationDatabaseRepository();
        this.roleAccessService = new RoleAccessService();
        this.userRepository = new UserDatabaseRepository();
    }

    public void recordForRoles(String eventType, String category, OperationalNotification.Severity severity,
            OperationalNotification.Priority priority, String referenceTable, String referenceId,
            Integer actorEmployeeId, String title, String message, String... roles) {
        int eventId = recordEvent(eventType, category, severity, priority, referenceTable, referenceId,
                actorEmployeeId, title, message);
        OperationalInboxService.invalidateCache();
        if (eventId <= 0 || roles == null) {
            return;
        }

        for (String role : roles) {
            if (!isBlank(role)) {
                addNotification(eventId, null, roleAccessService.normalizeRole(role), title, message,
                        category, severity, priority);
            }
        }
    }

    public void recordForEmployee(String eventType, String category, OperationalNotification.Severity severity,
            OperationalNotification.Priority priority, String referenceTable, String referenceId,
            Integer actorEmployeeId, Integer targetEmployeeId, String title, String message) {
        int eventId = recordEvent(eventType, category, severity, priority, referenceTable, referenceId,
                actorEmployeeId, title, message);
        OperationalInboxService.invalidateCache();
        if (eventId > 0 && targetEmployeeId != null && targetEmployeeId > 0) {
            addNotification(eventId, targetEmployeeId, null, title, message, category, severity, priority);
        }
    }

    public void recordBroadcast(String eventType, String category, OperationalNotification.Severity severity,
            OperationalNotification.Priority priority, String referenceTable, String referenceId,
            Integer actorEmployeeId, String title, String message) {
        int eventId = recordEvent(eventType, category, severity, priority, referenceTable, referenceId,
                actorEmployeeId, title, message);
        OperationalInboxService.invalidateCache();
        if (eventId > 0) {
            addNotification(eventId, null, null, title, message, category, severity, priority);
        }
    }

    public List<OperationalNotification> getPersistentNotifications(int employeeId, String role, int limit) {
        List<OperationalNotification> notifications = new ArrayList<>();
        for (PersistentNotification notification : notificationRepository.findActiveForEmployeeOrRole(
                employeeId, roleAccessService.normalizeRole(role), limit)) {
            notifications.add(toOperationalNotification(notification));
        }
        return notifications;
    }

    public void markRead(int notificationId) {
        notificationRepository.markRead(notificationId);
    }

    public int countUnreadNotifications(int employeeId, String role) {
        return notificationRepository.countUnreadActiveForEmployeeOrRole(
                employeeId, roleAccessService.normalizeRole(role));
    }

    public int markActiveNotificationsRead(int employeeId, String role) {
        int updated = notificationRepository.markActiveReadForEmployeeOrRole(
                employeeId, roleAccessService.normalizeRole(role));
        if (updated > 0) {
            OperationalInboxService.invalidateCache();
        }
        return updated;
    }

    public void acknowledge(int notificationId) {
        notificationRepository.acknowledge(notificationId);
    }

    private int recordEvent(String eventType, String category, OperationalNotification.Severity severity,
            OperationalNotification.Priority priority, String referenceTable, String referenceId,
            Integer actorEmployeeId, String title, String message) {
        Integer actorUserId = resolveUserId(actorEmployeeId);
        OperationalEvent event = new OperationalEvent(
                0,
                truncate(eventType, 100),
                truncate(category, 100),
                enumName(severity, "INFO"),
                enumName(priority, "INFORMATIONAL"),
                truncate(referenceTable, 100),
                truncate(referenceId, 100),
                actorUserId,
                truncate(title, 150),
                truncate(message, 500),
                "ACTIVE",
                LocalDateTime.now()
        );
        return eventRepository.addAndReturnId(event);
    }

    private Integer resolveUserId(Integer employeeId) {
        if (employeeId == null || employeeId <= 0 || userRepository == null) {
            return null;
        }
        User user = userRepository.findByEmployeeId(employeeId);
        return user == null || user.getUserId() <= 0 ? null : user.getUserId();
    }

    private void addNotification(int eventId, Integer targetEmployeeId, String targetRole,
            String title, String message, String category, OperationalNotification.Severity severity,
            OperationalNotification.Priority priority) {
        notificationRepository.add(new PersistentNotification(
                0,
                eventId,
                targetEmployeeId,
                truncate(targetRole, 50),
                truncate(title, 150),
                truncate(message, 500),
                truncate(category, 100),
                enumName(severity, "INFO"),
                enumName(priority, "INFORMATIONAL"),
                "ACTIVE",
                null,
                null,
                LocalDateTime.now()
        ));
    }

    private OperationalNotification toOperationalNotification(PersistentNotification notification) {
        return new OperationalNotification(
                notification.getTitle(),
                notification.getMessage(),
                notification.getCategory(),
                parseSeverity(notification.getSeverity()),
                parsePriority(notification.getPriority()),
                notification.getCreatedAt(),
                notification.getReadAt() == null ? "Unread" : "Read"
        );
    }

    private OperationalNotification.Severity parseSeverity(String value) {
        try {
            return OperationalNotification.Severity.valueOf(safe(value).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return OperationalNotification.Severity.INFO;
        }
    }

    private OperationalNotification.Priority parsePriority(String value) {
        try {
            return OperationalNotification.Priority.valueOf(safe(value).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return OperationalNotification.Priority.INFORMATIONAL;
        }
    }

    private String enumName(Enum<?> value, String fallback) {
        return value == null ? fallback : value.name();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
