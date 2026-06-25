package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.Deduction;

// Contract prepared for future normalized deduction management.
public interface DeductionRepository {

    Deduction findById(int deductionId);

    List<Deduction> findAll();

    void add(Deduction deduction);

    void update(Deduction deduction);

    void delete(int deductionId);
}
