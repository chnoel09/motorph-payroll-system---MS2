package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.ScheduleBatch;

// Contract prepared for future normalized schedule batch management.
public interface ScheduleBatchRepository {

    ScheduleBatch findById(int scheduleBatchId);

    List<ScheduleBatch> findAll();

    void add(ScheduleBatch batch);

    void update(ScheduleBatch batch);

    void delete(int scheduleBatchId);
}
