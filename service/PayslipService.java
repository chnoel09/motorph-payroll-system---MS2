package com.mycompany.oop.service;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.model.Payslip;
import com.mycompany.oop.repository.PayslipDatabaseRepository;
import com.mycompany.oop.repository.PayslipRepository;

public class PayslipService {

    private PayslipRepository payslipRepository;

    public PayslipService() {
        this.payslipRepository = new PayslipDatabaseRepository();
    }

    public PayslipService(PayslipRepository payslipRepository) {
        this.payslipRepository = payslipRepository;
    }

    public void createPayslip(Payslip payslip) {
        if (payslipRepository != null && payslip != null) {
            payslipRepository.add(payslip);
        }
    }

    public List<Payslip> getPayslipByEmployeeId(int employeeId) {
        if (payslipRepository == null) {
            return new ArrayList<>();
        }

        return payslipRepository.findByEmployeeId(employeeId);
    }
}
