package com.mycompany.oop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.repository.AttendanceRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttendanceServiceTest {

    private static final Logger LOGGER = Logger.getLogger(AttendanceServiceTest.class.getName());

    private AttendanceService service;
    private FakeAttendanceRepository repository;

    @BeforeEach
    void setUp() {
        service = new AttendanceService();
        repository = new FakeAttendanceRepository();
        ServiceTestSupport.setField(service, "repository", repository);
    }

    @Test
    void getAttendanceForDateReturnsRecordForEmployeeAndDate() {
        LOGGER.info("Checking read-only attendance lookup by date.");

        LocalDate date = LocalDate.of(2024, 6, 3);
        repository.saveAttendance(new AttendanceRecord(10008, date.toString(), "08:00", "17:00"));

        AttendanceRecord record = service.getAttendanceForDate(10008, date);

        assertEquals("08:00", record.getTimeIn());
        assertEquals("17:00", record.getTimeOut());
        assertNull(service.getAttendanceForDate(0, date));
        assertNull(service.getAttendanceForDate(10008, null));
    }

    @Test
    void getHoursWorkedComputesCompletedAttendanceOnly() {
        LOGGER.info("Checking attendance hours calculation.");

        assertEquals(8.0, service.getHoursWorked(new AttendanceRecord(10008, "2024-06-03", "08:00", "16:00")));
        assertEquals(0.0, service.getHoursWorked(new AttendanceRecord(10008, "2024-06-03", "08:00", "")));
        assertEquals(0.0, service.getHoursWorked(new AttendanceRecord(10008, "2024-06-03", "17:00", "08:00")));
    }

    @Test
    void timeInCreatesRecordOnlyWhenTodayIsMissing() {
        LOGGER.info("Checking isolated time-in behavior.");

        service.timeIn(10008);
        service.timeIn(10008);

        assertEquals(1, repository.findByEmployeeId(10008).size());
        assertEquals(1, repository.saveCount);
    }

    @Test
    void timeOutUpdatesExistingOpenRecord() {
        LOGGER.info("Checking isolated time-out behavior.");

        String today = LocalDate.now().toString();
        repository.saveAttendance(new AttendanceRecord(10008, today, "08:00", ""));

        service.timeOut(10008);

        AttendanceRecord record = repository.findByEmployeeAndDate(10008, today);
        assertEquals(1, repository.updateCount);
        assertEquals("08:00", record.getTimeIn());
    }

    private static class FakeAttendanceRepository implements AttendanceRepository {
        private final Map<String, AttendanceRecord> records = new HashMap<>();
        private int saveCount;
        private int updateCount;

        @Override
        public void saveAttendance(AttendanceRecord record) {
            saveCount++;
            records.put(key(record.getEmployeeId(), record.getDate()), record);
        }

        @Override
        public void updateAttendance(AttendanceRecord record) {
            updateCount++;
            records.put(key(record.getEmployeeId(), record.getDate()), record);
        }

        @Override
        public List<AttendanceRecord> findAll() {
            return new ArrayList<>(records.values());
        }

        @Override
        public List<AttendanceRecord> findByEmployeeId(int employeeId) {
            return records.values().stream()
                    .filter(record -> record.getEmployeeId() == employeeId)
                    .toList();
        }

        @Override
        public AttendanceRecord findByEmployeeAndDate(int employeeId, String date) {
            return records.get(key(employeeId, date));
        }

        private String key(int employeeId, String date) {
            return employeeId + "|" + date;
        }
    }
}
