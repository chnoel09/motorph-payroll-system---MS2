package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.PersistentNotification;

public interface NotificationRepository {

    void add(PersistentNotification notification);

    List<PersistentNotification> findActiveForEmployeeOrRole(int employeeId, String role, int limit);

    int countUnreadActiveForEmployeeOrRole(int employeeId, String role);

    int markActiveReadForEmployeeOrRole(int employeeId, String role);

    void markRead(int notificationId);

    void acknowledge(int notificationId);
}
