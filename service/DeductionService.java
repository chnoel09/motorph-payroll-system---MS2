package com.mycompany.oop.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;

import com.mycompany.oop.model.Deduction;
import com.mycompany.oop.model.DeductionBracket;
import com.mycompany.oop.model.DeductionType;
import com.mycompany.oop.model.PayrollDeduction;
import com.mycompany.oop.model.PayrollRecord;
import com.mycompany.oop.repository.DeductionBracketDatabaseRepository;
import com.mycompany.oop.repository.DeductionBracketRepository;
import com.mycompany.oop.repository.DeductionDatabaseRepository;
import com.mycompany.oop.repository.DeductionRepository;
import com.mycompany.oop.repository.DeductionTypeDatabaseRepository;
import com.mycompany.oop.repository.DeductionTypeRepository;
import com.mycompany.oop.repository.PayrollDeductionDatabaseRepository;
import com.mycompany.oop.repository.PayrollDeductionRepository;

// Prepared for the future normalized deduction migration.
// Current statutory payroll calculations remain in the existing payroll services.
public class DeductionService {

    private DeductionTypeRepository deductionTypeRepository;
    private DeductionRepository deductionRepository;
    private DeductionBracketRepository deductionBracketRepository;
    private PayrollDeductionRepository payrollDeductionRepository;

    public DeductionService() {
        this.deductionTypeRepository = new DeductionTypeDatabaseRepository();
        this.deductionRepository = new DeductionDatabaseRepository();
        this.deductionBracketRepository = new DeductionBracketDatabaseRepository();
        this.payrollDeductionRepository = new PayrollDeductionDatabaseRepository();
    }

    public DeductionService(PayrollDeductionRepository payrollDeductionRepository) {
        this.deductionTypeRepository = new DeductionTypeDatabaseRepository();
        this.deductionRepository = new DeductionDatabaseRepository();
        this.deductionBracketRepository = new DeductionBracketDatabaseRepository();
        this.payrollDeductionRepository = payrollDeductionRepository;
    }

    public DeductionService(DeductionRepository deductionRepository,
            PayrollDeductionRepository payrollDeductionRepository) {
        this.deductionTypeRepository = new DeductionTypeDatabaseRepository();
        this.deductionRepository = deductionRepository;
        this.deductionBracketRepository = new DeductionBracketDatabaseRepository();
        this.payrollDeductionRepository = payrollDeductionRepository;
    }

    public double computeDeduction(Deduction deduction, double salary) {
        if (deduction == null || salary <= 0) {
            return 0;
        }

        // Future phase: resolve computationMethod with deduction_brackets or fixed rules.
        return 0;
    }

    public List<PayrollDeduction> getDeductionsForPayrollDetail(int runDetailId) {
        if (payrollDeductionRepository == null) {
            return new ArrayList<>();
        }

        return payrollDeductionRepository.findByRunDetailId(runDetailId);
    }

    public void persistLegacyPayrollBreakdown(int runDetailId, PayrollRecord record) {
        if (runDetailId <= 0 || record == null || payrollDeductionRepository == null) {
            return;
        }

        Map<String, Double> legacyAmounts = new LinkedHashMap<>();
        legacyAmounts.put("SSS", record.getSss());
        legacyAmounts.put("PhilHealth", record.getPhilhealth());
        legacyAmounts.put("Pag-IBIG", record.getPagibig());
        legacyAmounts.put("Tax", record.getTax());

        Map<String, Deduction> deductionsByName = loadDeductionsByName();
        for (Map.Entry<String, Double> entry : legacyAmounts.entrySet()) {
            Deduction deduction = deductionsByName.get(normalize(entry.getKey()));
            if (deduction == null) {
                continue;
            }

            payrollDeductionRepository.add(new PayrollDeduction(
                    0,
                    runDetailId,
                    deduction.getDeductionId(),
                    entry.getValue() == null ? 0.0 : entry.getValue(),
                    "Persisted from legacy payroll formula output"
            ));
        }
    }

    public List<Deduction> getConfiguredDeductions() {
        if (deductionRepository == null) {
            return new ArrayList<>();
        }
        return deductionRepository.findAll();
    }

    public List<DeductionType> getDeductionTypes() {
        if (deductionTypeRepository == null) {
            return new ArrayList<>();
        }
        return deductionTypeRepository.findAll();
    }

    public List<DeductionBracket> getBracketsForDeduction(int deductionId) {
        if (deductionBracketRepository == null || deductionId <= 0) {
            return new ArrayList<>();
        }
        return deductionBracketRepository.findByDeductionId(deductionId);
    }

    public List<DeductionBracket> getEffectiveBracketsForDeduction(int deductionId, LocalDate effectiveDate) {
        List<DeductionBracket> effectiveBrackets = new ArrayList<>();
        for (DeductionBracket bracket : getBracketsForDeduction(deductionId)) {
            if (effectiveDate == null
                    || bracket.getEffectiveDate() == null
                    || !bracket.getEffectiveDate().isAfter(effectiveDate)) {
                effectiveBrackets.add(bracket);
            }
        }
        return effectiveBrackets;
    }

    private Map<String, Deduction> loadDeductionsByName() {
        Map<String, Deduction> deductionsByName = new LinkedHashMap<>();
        for (Deduction deduction : getConfiguredDeductions()) {
            deductionsByName.put(normalize(deduction.getDeductionName()), deduction);
        }
        return deductionsByName;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
