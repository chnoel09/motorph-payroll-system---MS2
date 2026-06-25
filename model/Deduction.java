package com.mycompany.oop.model;

// Prepared for the future normalized deductions migration.
public class Deduction {

    private int deductionId;
    private int deductionTypeId;
    private String deductionName;
    private boolean governmentMandated;
    private String computationMethod;

    public Deduction() {
    }

    public Deduction(int deductionId, int deductionTypeId, String deductionName,
            boolean governmentMandated, String computationMethod) {
        this.deductionId = deductionId;
        this.deductionTypeId = deductionTypeId;
        this.deductionName = deductionName;
        this.governmentMandated = governmentMandated;
        this.computationMethod = computationMethod;
    }

    public int getDeductionId() {
        return deductionId;
    }

    public void setDeductionId(int deductionId) {
        this.deductionId = deductionId;
    }

    public int getDeductionTypeId() {
        return deductionTypeId;
    }

    public void setDeductionTypeId(int deductionTypeId) {
        this.deductionTypeId = deductionTypeId;
    }

    public String getDeductionName() {
        return deductionName;
    }

    public void setDeductionName(String deductionName) {
        this.deductionName = deductionName;
    }

    public boolean isGovernmentMandated() {
        return governmentMandated;
    }

    public void setGovernmentMandated(boolean governmentMandated) {
        this.governmentMandated = governmentMandated;
    }

    public String getComputationMethod() {
        return computationMethod;
    }

    public void setComputationMethod(String computationMethod) {
        this.computationMethod = computationMethod;
    }
}
