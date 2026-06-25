package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.mycompany.oop.model.PayrollRecord;
import com.mycompany.oop.model.RegularEmployee;

class PayrollProcessorTest {

    private final PayrollProcessor processor = new PayrollProcessor();

    @Test
    void processPayrollComputesGrossDeductionsAndNetPay() {
        RegularEmployee employee = employeeWithPay(100, 1500);

        PayrollRecord record = processor.processPayroll(employee, 80);

        assertEquals(8000.00, record.getBasicComponent(), 0.001);
        assertEquals(750.00, record.getAllowanceComponent(), 0.001);
        assertEquals(8750.00, record.getGross(), 0.001);
        assertEquals(393.75, record.getSss(), 0.001);
        assertEquals(131.25, record.getPhilhealth(), 0.001);
        assertEquals(50.00, record.getPagibig(), 0.001);
        assertEquals(0.00, record.getTax(), 0.001);
        assertEquals(575.00, record.getTotalDeductions(), 0.001);
        assertEquals(8175.00, record.getNet(), 0.001);
    }

    @Test
    void processPayrollMatchesMotorphTwentyFiveThousandMonthlySample() {
        RegularEmployee employee = employeeWithPay(125, 0);

        PayrollRecord record = processor.processPayroll(employee, 100);

        assertEquals(12500.00, record.getGross(), 0.001);
        assertEquals(562.50, record.getSss(), 0.001);
        assertEquals(187.50, record.getPhilhealth(), 0.001);
        assertEquals(50.00, record.getPagibig(), 0.001);
        assertEquals(192.525, record.getTax(), 0.001);
        assertEquals(992.525, record.getTotalDeductions(), 0.001);
        assertEquals(11507.475, record.getNet(), 0.001);
    }

    @Test
    void processPayrollComputesTaxAfterStatutoryDeductions() {
        RegularEmployee employee = employeeWithPay(500, 0);

        PayrollRecord record = processor.processPayroll(employee, 100);

        assertEquals(50000.00, record.getGross(), 0.001);
        assertEquals(562.50, record.getSss(), 0.001);
        assertEquals(450.00, record.getPhilhealth(), 0.001);
        assertEquals(50.00, record.getPagibig(), 0.001);
        assertEquals(8171.955, record.getTax(), 0.001);
        assertEquals(9234.455, record.getTotalDeductions(), 0.001);
        assertEquals(40765.545, record.getNet(), 0.001);
    }

    @Test
    void processPayrollUsesSemiMonthlyBaseTaxWithUnchangedPercentageRate() {
        RegularEmployee employee = employeeWithPay(1000, 0);

        PayrollRecord record = processor.processPayroll(employee, 100);

        assertEquals(100000.00, record.getGross(), 0.001);
        assertEquals(562.50, record.getSss(), 0.001);
        assertEquals(450.00, record.getPhilhealth(), 0.001);
        assertEquals(50.00, record.getPagibig(), 0.001);
        assertEquals(21452.18, record.getTax(), 0.001);
        assertEquals(22514.68, record.getTotalDeductions(), 0.001);
        assertEquals(77485.32, record.getNet(), 0.001);
    }

    @Test
    void processPayrollReturnsZeroValuesWhenHoursAreNotPositive() {
        RegularEmployee employee = employeeWithPay(90, 1000);

        PayrollRecord record = processor.processPayroll(employee, 0);

        assertEquals(0.00, record.getBasicComponent(), 0.001);
        assertEquals(0.00, record.getAllowanceComponent(), 0.001);
        assertEquals(0.00, record.getGross(), 0.001);
        assertEquals(0.00, record.getTotalDeductions(), 0.001);
        assertEquals(0.00, record.getNet(), 0.001);
    }

    @ParameterizedTest
    @CsvSource({
        "10416.50, 0.0",
        "10416.51, 0.0015",
        "16666.50, 937.50",
        "16666.51, 937.502",
        "33333.00, 4270.80",
        "33333.01, 4270.8325"
    })
    void withholdingTaxUsesSemiMonthlyBracketBoundaries(double taxableIncome, double expectedTax) {
        assertEquals(expectedTax, processor.computeSemiMonthlyWithholdingTax(taxableIncome), 0.00001);
    }

    private RegularEmployee employeeWithPay(double hourlyRate, double allowance) {
        return new RegularEmployee(
                10001,
                "Juan",
                "Dela Cruz",
                "Developer",
                "Active",
                30000,
                allowance,
                hourlyRate,
                "juan",
                "password",
                "Employee");
    }
}
