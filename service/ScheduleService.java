package com.mycompany.oop.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.DatabaseConnection;
import com.mycompany.oop.model.EmployeeShift;
import com.mycompany.oop.model.EmployeeSchedule;
import com.mycompany.oop.model.Holiday;
import com.mycompany.oop.model.ScheduleBatch;
import com.mycompany.oop.repository.EmployeeScheduleDatabaseRepository;
import com.mycompany.oop.repository.EmployeeScheduleRepository;
import com.mycompany.oop.repository.EmployeeShiftDatabaseRepository;
import com.mycompany.oop.repository.EmployeeShiftRepository;
import com.mycompany.oop.repository.HolidayDatabaseRepository;
import com.mycompany.oop.repository.HolidayRepository;
import com.mycompany.oop.repository.ScheduleBatchDatabaseRepository;
import com.mycompany.oop.repository.ScheduleBatchRepository;

// Scheduling service for reference data and future schedule assignment flows.
// This service is not wired into the current attendance or payroll computation flow.
public class ScheduleService {

    private static final String[] REQUIRED_SCHEDULING_TABLES = {
            "employee_shifts",
            "schedule_windows",
            "employee_schedules",
            "holidays"
    };

    private static final String[] REQUIRED_REFERENCE_TABLES = {
            "employee_shifts",
            "holidays"
    };

    private EmployeeShiftRepository employeeShiftRepository;
    private ScheduleBatchRepository scheduleBatchRepository;
    private EmployeeScheduleRepository employeeScheduleRepository;
    private HolidayRepository holidayRepository;

    public ScheduleService() {
        this.employeeShiftRepository = new EmployeeShiftDatabaseRepository();
        this.scheduleBatchRepository = new ScheduleBatchDatabaseRepository();
        this.employeeScheduleRepository = new EmployeeScheduleDatabaseRepository();
        this.holidayRepository = new HolidayDatabaseRepository();
    }

    public ScheduleService(ScheduleBatchRepository scheduleBatchRepository,
            EmployeeScheduleRepository employeeScheduleRepository) {
        this.scheduleBatchRepository = scheduleBatchRepository;
        this.employeeScheduleRepository = employeeScheduleRepository;
    }

    public ScheduleService(EmployeeShiftRepository employeeShiftRepository,
            ScheduleBatchRepository scheduleBatchRepository,
            EmployeeScheduleRepository employeeScheduleRepository,
            HolidayRepository holidayRepository) {
        this.employeeShiftRepository = employeeShiftRepository;
        this.scheduleBatchRepository = scheduleBatchRepository;
        this.employeeScheduleRepository = employeeScheduleRepository;
        this.holidayRepository = holidayRepository;
    }

    public boolean isSchedulingSchemaAvailable() {
        return hasTables(REQUIRED_SCHEDULING_TABLES);
    }

    public boolean isSchedulingReferenceSchemaAvailable() {
        return hasTables(REQUIRED_REFERENCE_TABLES);
    }

    private boolean hasTables(String[] tableNames) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (conn == null) {
                return false;
            }

            DatabaseMetaData metaData = conn.getMetaData();

            for (String tableName : tableNames) {
                if (!tableExists(metaData, tableName)) {
                    return false;
                }
            }

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public String getSchedulingSchemaStatusMessage() {
        return isSchedulingSchemaAvailable()
                ? "Workforce schedule setup is available."
                : "Workforce schedule setup is not yet available in the operational database.";
    }

    public String getSchedulingReferenceSchemaStatusMessage() {
        return isSchedulingReferenceSchemaAvailable()
                ? "Shift setup and holiday setup are available."
                : "Shift setup and holiday setup are not yet available in the operational database.";
    }

    public List<EmployeeShift> getEmployeeShifts() {
        if (!isSchedulingReferenceSchemaAvailable() || employeeShiftRepository == null) {
            return new ArrayList<>();
        }

        return employeeShiftRepository.findAll();
    }

    public List<ScheduleBatch> getScheduleBatches() {
        if (!isSchedulingSchemaAvailable() || scheduleBatchRepository == null) {
            return new ArrayList<>();
        }

        return scheduleBatchRepository.findAll();
    }

    public List<EmployeeSchedule> getAllEmployeeSchedules() {
        if (!isSchedulingSchemaAvailable() || employeeScheduleRepository == null) {
            return new ArrayList<>();
        }

        return employeeScheduleRepository.findAll();
    }

    public List<Holiday> getHolidays() {
        if (!isSchedulingReferenceSchemaAvailable() || holidayRepository == null) {
            return new ArrayList<>();
        }

        return holidayRepository.findAll();
    }

    public void createEmployeeShift(EmployeeShift shift) {
        if (isSchedulingReferenceSchemaAvailable() && employeeShiftRepository != null && shift != null) {
            employeeShiftRepository.add(shift);
        }
    }

    public void updateEmployeeShift(EmployeeShift shift) {
        if (isSchedulingReferenceSchemaAvailable() && employeeShiftRepository != null && shift != null) {
            employeeShiftRepository.update(shift);
        }
    }

    public void deleteEmployeeShift(int shiftId) {
        if (isSchedulingReferenceSchemaAvailable() && employeeShiftRepository != null && shiftId > 0) {
            employeeShiftRepository.delete(shiftId);
        }
    }

    public void createHoliday(Holiday holiday) {
        if (isSchedulingReferenceSchemaAvailable() && holidayRepository != null && holiday != null) {
            holidayRepository.add(holiday);
        }
    }

    public void updateHoliday(Holiday holiday) {
        if (isSchedulingReferenceSchemaAvailable() && holidayRepository != null && holiday != null) {
            holidayRepository.update(holiday);
        }
    }

    public void deleteHoliday(int holidayId) {
        if (isSchedulingReferenceSchemaAvailable() && holidayRepository != null && holidayId > 0) {
            holidayRepository.delete(holidayId);
        }
    }

    public void createScheduleBatch(ScheduleBatch batch) {
        if (isSchedulingSchemaAvailable() && scheduleBatchRepository != null && batch != null) {
            scheduleBatchRepository.add(batch);
        }
    }

    public void assignEmployeeSchedule(EmployeeSchedule schedule) {
        if (isSchedulingSchemaAvailable() && employeeScheduleRepository != null && schedule != null) {
            employeeScheduleRepository.add(schedule);
        }
    }

    public void updateEmployeeSchedule(EmployeeSchedule schedule) {
        if (isSchedulingSchemaAvailable() && employeeScheduleRepository != null && schedule != null) {
            employeeScheduleRepository.update(schedule);
        }
    }

    public void deleteEmployeeSchedule(int scheduleId) {
        if (isSchedulingSchemaAvailable() && employeeScheduleRepository != null && scheduleId > 0) {
            employeeScheduleRepository.delete(scheduleId);
        }
    }

    public List<EmployeeSchedule> getEmployeeSchedules(int employeeId) {
        if (!isSchedulingSchemaAvailable() || employeeScheduleRepository == null) {
            return new ArrayList<>();
        }

        return employeeScheduleRepository.findByEmployeeId(employeeId);
    }

    public List<EmployeeSchedule> getEmployeeSchedulesByDateRange(
            int employeeId, LocalDate startDate, LocalDate endDate) {

        if (!isSchedulingSchemaAvailable() || employeeScheduleRepository == null || startDate == null || endDate == null) {
            return new ArrayList<>();
        }

        return employeeScheduleRepository.findByDateRange(employeeId, startDate, endDate);
    }

    public List<EmployeeSchedule> getEmployeeSchedulesByDateRange(
            List<Integer> employeeIds, LocalDate startDate, LocalDate endDate) {

        List<EmployeeSchedule> schedules = new ArrayList<>();
        if (employeeIds == null || employeeIds.isEmpty() || startDate == null || endDate == null) {
            return schedules;
        }

        for (Integer employeeId : employeeIds) {
            if (employeeId != null && employeeId > 0) {
                schedules.addAll(getEmployeeSchedulesByDateRange(employeeId, startDate, endDate));
            }
        }

        return schedules;
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }

        try (ResultSet rs = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
