package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.DeductionBracket;

// Contract prepared for future normalized deduction bracket management.
public interface DeductionBracketRepository {

    DeductionBracket findById(int deductionBracketId);

    List<DeductionBracket> findAll();

    List<DeductionBracket> findByDeductionId(int deductionId);

    void add(DeductionBracket bracket);

    void update(DeductionBracket bracket);

    void delete(int deductionBracketId);
}
