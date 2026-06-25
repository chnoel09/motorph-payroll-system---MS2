package com.mycompany.oop.model;

// Prepared for the future normalized deduction_types migration.
public class DeductionType {

    private int deductionTypeId;
    private String deductionTypeName;

    public DeductionType() {
    }

    public DeductionType(int deductionTypeId, String deductionTypeName) {
        this.deductionTypeId = deductionTypeId;
        this.deductionTypeName = deductionTypeName;
    }

    public int getDeductionTypeId() {
        return deductionTypeId;
    }

    public void setDeductionTypeId(int deductionTypeId) {
        this.deductionTypeId = deductionTypeId;
    }

    public String getDeductionTypeName() {
        return deductionTypeName;
    }

    public void setDeductionTypeName(String deductionTypeName) {
        this.deductionTypeName = deductionTypeName;
    }
}
