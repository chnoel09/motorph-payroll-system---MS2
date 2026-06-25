package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.DeductionType;

// Contract prepared for future normalized deduction type management.
public interface DeductionTypeRepository {

    DeductionType findById(int deductionTypeId);

    List<DeductionType> findAll();

    void add(DeductionType deductionType);

    void update(DeductionType deductionType);

    void delete(int deductionTypeId);
}
