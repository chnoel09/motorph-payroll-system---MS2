package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollRun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PayrollPeriodLifecycleServiceTest {

    private final PayrollPeriodLifecycleService service = new PayrollPeriodLifecycleService();

    @Test
    void partialReadyOnlyRunKeepsPeriodOpen() {
        assertFalse(service.areAllAttendanceEmployeesProcessed(
                Set.of(10001, 10002),
                Set.of(10001)));
    }

    @Test
    void completeReadyOnlyRunMarksPeriodProcessed() {
        assertTrue(service.areAllAttendanceEmployeesProcessed(
                Set.of(10001, 10002),
                Set.of(10001, 10002)));
    }

    @Test
    void employeeWithoutAttendanceDoesNotBlockCompletion() {
        assertTrue(service.areAllAttendanceEmployeesProcessed(
                Set.of(10001, 10002),
                Set.of(10001, 10002, 10035)));
    }

    @Test
    void partialReadyOnlyRunUsesCurrentPeriodStatusForDisplay() {
        PayrollPeriod period = new PayrollPeriod(
                2,
                "Jun-2024-2nd",
                LocalDate.of(2024, 6, 16),
                LocalDate.of(2024, 6, 30),
                PayrollPeriodLifecycleService.STATUS_OPEN_WORKFORCE_REVIEW);
        PayrollRun run = new PayrollRun(
                5,
                2,
                10,
                LocalDateTime.now(),
                PayrollRunService.STATUS_READY_ONLY_PROCESSED);

        assertEquals(PayrollPeriodLifecycleService.STATUS_OPEN_WORKFORCE_REVIEW,
                new PayrollLifecycleService().resolveLifecycleSource(period, run));
    }
}
