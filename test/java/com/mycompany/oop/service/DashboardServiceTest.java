package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.TodayAttendanceSummary;

import java.time.LocalDate;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private static final Logger LOGGER = Logger.getLogger(DashboardServiceTest.class.getName());

    @Test
    void todayAttendanceSummaryUsesReadOnlyAttendanceServices() {
        LOGGER.info("Checking dashboard today attendance summary flow.");

        DashboardService service = new DashboardService();
        LocalDate today = LocalDate.now();
        Employee employee = ServiceTestSupport.employee(10008, "Alice", "Reyes", "Employee");

        ServiceTestSupport.setField(service, "attendanceService", new StubAttendanceService(today));
        ServiceTestSupport.setField(service, "attendanceAwarenessService", new StubAttendanceAwarenessService(today));

        TodayAttendanceSummary summary = service.getTodayAttendanceSummary(employee);

        assertEquals(today, summary.getDate());
        assertEquals("Complete", summary.getStatus());
        assertEquals("Attendance aligns with the assigned schedule.", summary.getMessage());
        assertEquals("08:00", summary.getTimeIn());
        assertEquals("17:00", summary.getTimeOut());
        assertEquals(9.0, summary.getHoursWorked(), 0.001);
    }

    @Test
    void todayAttendanceSummaryHandlesMissingEmployee() {
        LOGGER.info("Checking dashboard today attendance fallback.");

        DashboardService service = new DashboardService();

        TodayAttendanceSummary summary = service.getTodayAttendanceSummary(null);

        assertEquals(LocalDate.now(), summary.getDate());
        assertEquals("Unavailable", summary.getStatus());
        assertEquals(0.0, summary.getHoursWorked(), 0.001);
    }

    private static class StubAttendanceService extends AttendanceService {
        private final LocalDate date;

        private StubAttendanceService(LocalDate date) {
            this.date = date;
        }

        @Override
        public AttendanceRecord getAttendanceForDate(int employeeId, LocalDate date) {
            if (employeeId == 10008 && this.date.equals(date)) {
                return new AttendanceRecord(employeeId, date.toString(), "08:00", "17:00");
            }
            return null;
        }

        @Override
        public double getHoursWorked(AttendanceRecord record) {
            return 9.0;
        }
    }

    private static class StubAttendanceAwarenessService extends AttendanceAwarenessService {
        private final LocalDate date;

        private StubAttendanceAwarenessService(LocalDate date) {
            this.date = date;
        }

        @Override
        public AttendanceAwareness getDailyAwareness(int employeeId, LocalDate date) {
            return new AttendanceAwareness(
                    employeeId,
                    this.date,
                    "Complete",
                    AttendanceAwareness.Severity.NORMAL,
                    "Attendance aligns with the assigned schedule.",
                    "08:00",
                    "17:00",
                    "Primary Shift");
        }
    }
}
