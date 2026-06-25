package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.OperationalEvent;

public interface OperationalEventRepository {

    int addAndReturnId(OperationalEvent event);

    List<OperationalEvent> findRecentActive(int limit);
}
