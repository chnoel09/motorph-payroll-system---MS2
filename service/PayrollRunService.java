package com.mycompany.oop.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollRun;
import com.mycompany.oop.model.PayrollRunDetail;
import com.mycompany.oop.repository.PayrollPeriodDatabaseRepository;
import com.mycompany.oop.repository.PayrollPeriodRepository;
import com.mycompany.oop.repository.PayrollRunDatabaseRepository;
import com.mycompany.oop.repository.PayrollRunDetailDatabaseRepository;
import com.mycompany.oop.repository.PayrollRunDetailRepository;
import com.mycompany.oop.repository.PayrollRunRepository;

// Bridge for normalized payroll periods/runs. It does not calculate payroll formulas.
public class PayrollRunService {

    public static final String STATUS_READY_ONLY_PROCESSED = "READY_ONLY_PROCESSED";

    private PayrollPeriodRepository payrollPeriodRepository;
    private PayrollRunRepository payrollRunRepository;
    private PayrollRunDetailRepository payrollRunDetailRepository;

    public PayrollRunService() {
        this.payrollPeriodRepository = new PayrollPeriodDatabaseRepository();
        this.payrollRunRepository = new PayrollRunDatabaseRepository();
        this.payrollRunDetailRepository = new PayrollRunDetailDatabaseRepository();
    }

    public PayrollRunService(PayrollRunRepository payrollRunRepository,
            PayrollRunDetailRepository payrollRunDetailRepository) {
        this.payrollPeriodRepository = new PayrollPeriodDatabaseRepository();
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunDetailRepository = payrollRunDetailRepository;
    }

    public PayrollPeriod getOrCreatePeriod(String cutoffPeriod, LocalDate periodStart, LocalDate periodEnd) {
        PayrollPeriod existing = payrollPeriodRepository.findByCutoffAndRange(
                cutoffPeriod,
                periodStart.toString(),
                periodEnd.toString()
        );

        if (existing != null) {
            return existing;
        }

        PayrollPeriod period = new PayrollPeriod(0, cutoffPeriod, periodStart, periodEnd,
                PayrollPeriodLifecycleService.STATUS_DRAFT);
        int periodId = payrollPeriodRepository.addAndReturnId(period);
        period.setPeriodId(periodId);
        return period;
    }

    public PayrollRun createProcessedRun(PayrollPeriod period, Integer processedBy) {
        if (period == null || period.getPeriodId() <= 0) {
            return null;
        }

        PayrollRun run = new PayrollRun(
                0,
                period.getPeriodId(),
                processedBy,
                LocalDateTime.now(),
                PayrollPeriodLifecycleService.STATUS_PROCESSED
        );
        int runId = payrollRunRepository.addAndReturnId(run);
        run.setRunId(runId);

        period.setStatus(PayrollPeriodLifecycleService.STATUS_PROCESSED);
        payrollPeriodRepository.update(period);
        return run;
    }

    public PayrollRun createReadyOnlyProcessedRun(PayrollPeriod period, Integer processedBy) {
        if (period == null || period.getPeriodId() <= 0) {
            return null;
        }

        PayrollRun run = new PayrollRun(
                0,
                period.getPeriodId(),
                processedBy,
                LocalDateTime.now(),
                STATUS_READY_ONLY_PROCESSED
        );
        int runId = payrollRunRepository.addAndReturnId(run);
        run.setRunId(runId);
        return run;
    }

    public PayrollRun getLatestRunForPeriod(PayrollPeriod period) {
        if (period == null || period.getPeriodId() <= 0) {
            return null;
        }
        return payrollRunRepository.findLatestByPeriodId(period.getPeriodId());
    }

    public int getRunDetailCount(int runId) {
        return getPayrollRunDetails(runId).size();
    }

    public void createPayrollRun(PayrollRun run) {
        if (payrollRunRepository != null && run != null) {
            payrollRunRepository.add(run);
        }
    }

    public void addPayrollRunDetail(PayrollRunDetail detail) {
        addPayrollRunDetailAndReturnId(detail);
    }

    public int addPayrollRunDetailAndReturnId(PayrollRunDetail detail) {
        if (payrollRunDetailRepository != null && detail != null) {
            return payrollRunDetailRepository.addAndReturnId(detail);
        }
        return 0;
    }

    public List<PayrollRunDetail> getPayrollRunDetails(int runId) {
        if (payrollRunDetailRepository == null) {
            return new ArrayList<>();
        }

        return payrollRunDetailRepository.findByRunId(runId);
    }
}
