package com.mycompany.oop.service;

import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.EmployeeSchedule;
import com.mycompany.oop.model.EmployeeShift;
import com.mycompany.oop.model.Holiday;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Operational awareness only. This does not alter official attendance or payroll calculations.
public class AttendanceAwarenessService {

    private final AttendanceService attendanceService;
    private final ScheduleService scheduleService;
    private final EmployeeService employeeService;

    public AttendanceAwarenessService() {
        this.attendanceService = new AttendanceService();
        this.scheduleService = new ScheduleService();
        this.employeeService = new EmployeeService();
    }

    public AttendanceAwareness getDailyAwareness(int employeeId, LocalDate date) {
        if (employeeId <= 0 || date == null) {
            return normal(employeeId, date, "Unavailable", "No attendance awareness available.", "", "", "");
        }

        List<AttendanceAwareness> items = getEmployeeAwareness(employeeId, date, date);
        return items.isEmpty()
                ? normal(employeeId, date, "Unavailable", "No attendance awareness available.", "", "", "")
                : items.get(0);
    }

    public List<AttendanceAwareness> getEmployeeAwareness(int employeeId, LocalDate start, LocalDate end) {
        List<AttendanceAwareness> items = new ArrayList<>();
        if (employeeId <= 0 || start == null || end == null || end.isBefore(start)) {
            return items;
        }

        Map<LocalDate, AttendanceRecord> recordsByDate = getAttendanceByDate(employeeId);
        Map<LocalDate, EmployeeSchedule> schedulesByDate = getSchedulesByDate(employeeId, start, end);
        Map<Integer, EmployeeShift> shiftsById = getShiftsById();
        Map<LocalDate, Holiday> holidaysByDate = getHolidaysByDate(start, end);

        return getEmployeeAwareness(employeeId, start, end, recordsByDate, schedulesByDate, shiftsById, holidaysByDate);
    }

    private List<AttendanceAwareness> getEmployeeAwareness(int employeeId, LocalDate start, LocalDate end,
            Map<LocalDate, AttendanceRecord> recordsByDate,
            Map<LocalDate, EmployeeSchedule> schedulesByDate,
            Map<Integer, EmployeeShift> shiftsById,
            Map<LocalDate, Holiday> holidaysByDate) {

        List<AttendanceAwareness> items = new ArrayList<>();
        if (employeeId <= 0 || start == null || end == null || end.isBefore(start)) {
            return items;
        }

        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            EmployeeSchedule schedule = schedulesByDate.get(cursor);
            EmployeeShift shift = schedule == null || schedule.getShiftId() == null ? null : shiftsById.get(schedule.getShiftId());
            items.add(classify(employeeId, cursor, recordsByDate.get(cursor), schedule, shift, holidaysByDate.get(cursor)));
            cursor = cursor.plusDays(1);
        }

        return items;
    }

    public List<AttendanceAwareness> getTeamAwareness(List<Employee> employees, LocalDate start, LocalDate end) {
        List<AttendanceAwareness> items = new ArrayList<>();
        if (employees == null || employees.isEmpty()) {
            return items;
        }

        Map<Integer, EmployeeShift> shiftsById = getShiftsById();
        Map<LocalDate, Holiday> holidaysByDate = getHolidaysByDate(start, end);

        for (Employee employee : employees) {
            if (employee != null) {
                int employeeId = employee.getEmployeeId();
                items.addAll(getEmployeeAwareness(
                        employeeId,
                        start,
                        end,
                        getAttendanceByDate(employeeId),
                        getSchedulesByDate(employeeId, start, end),
                        shiftsById,
                        holidaysByDate));
            }
        }

        return items;
    }

    public List<AttendanceAwareness> getTeamOperationsAwareness(Employee viewer, LocalDate start, LocalDate end) {
        return getTeamAwareness(employeeService.getTeamOperationsEmployees(viewer), start, end);
    }

    public String getOperationalAlertSummary(Employee viewer) {
        if (viewer == null) {
            return "Attendance awareness is available.";
        }

        RoleAccessService roleAccessService = new RoleAccessService();
        LocalDate today = LocalDate.now();
        List<AttendanceAwareness> awareness;

        if (roleAccessService.isHR(viewer.getRole())
                || employeeService.hasAssignedTeam(viewer.getEmployeeId())) {
            awareness = getTeamOperationsAwareness(viewer, today, today);
        } else {
            awareness = List.of(getDailyAwareness(viewer.getEmployeeId(), today));
        }

        long issues = awareness.stream().filter(AttendanceAwareness::requiresReview).count();
        if (issues == 0) {
            return "No attendance issues require review today.";
        }

        long incomplete = awareness.stream()
                .filter(item -> "Missing Time Out".equals(item.getStatus())
                        || "Missing Time In".equals(item.getStatus())
                        || "Incomplete Attendance".equals(item.getStatus()))
                .count();
        long noSchedule = awareness.stream()
                .filter(item -> "No Assigned Schedule".equals(item.getStatus()))
                .count();

        if (incomplete > 0) {
            return incomplete + " incomplete attendance log" + plural(incomplete) + " require review.";
        }
        if (noSchedule > 0) {
            return noSchedule + " attendance record" + plural(noSchedule) + " without assigned schedule.";
        }

        return issues + " attendance issue" + plural(issues) + " require review.";
    }

    private AttendanceAwareness classify(int employeeId, LocalDate date, AttendanceRecord record,
            EmployeeSchedule schedule, EmployeeShift shift, Holiday holiday) {

        String timeIn = record == null ? "" : safe(record.getTimeIn());
        String timeOut = record == null ? "" : safe(record.getTimeOut());
        String shiftLabel = getShiftLabel(schedule, shift);

        if (holiday != null && record == null) {
            return info(employeeId, date, "Holiday", "Holiday: " + holiday.getHolidayName(), timeIn, timeOut, shiftLabel);
        }

        if (schedule != null && schedule.isRestDay() && record == null) {
            return info(employeeId, date, "Rest Day", "Rest day scheduled.", timeIn, timeOut, shiftLabel);
        }

        if (schedule == null && record == null) {
            return warning(employeeId, date, "No Assigned Schedule", "No schedule assigned.", timeIn, timeOut, shiftLabel);
        }

        if (record == null) {
            return critical(employeeId, date, "Missing Time In", "No time-in recorded for assigned work day.", timeIn, timeOut, shiftLabel);
        }

        if (timeIn.isBlank()) {
            return critical(employeeId, date, "Missing Time In", "Attendance record has no time-in.", timeIn, timeOut, shiftLabel);
        }

        if (timeOut.isBlank()) {
            return warning(employeeId, date, "Missing Time Out", "Attendance record has no time-out yet.", timeIn, timeOut, shiftLabel);
        }

        if (schedule == null) {
            return warning(employeeId, date, "No Assigned Schedule", "Attendance exists without an assigned schedule.", timeIn, timeOut, shiftLabel);
        }

        if (schedule.isRestDay()) {
            return info(employeeId, date, "Rest Day Attendance", "Attendance recorded on a rest day.", timeIn, timeOut, shiftLabel);
        }

        if (shift != null && isLate(timeIn, shift)) {
            return warning(employeeId, date, "Late", "Possible late attendance against assigned shift.", timeIn, timeOut, shiftLabel);
        }

        if (shift != null && isUndertime(timeOut, shift)) {
            return warning(employeeId, date, "Undertime", "Possible undertime against assigned shift.", timeIn, timeOut, shiftLabel);
        }

        return normal(employeeId, date, "Complete", "Attendance aligns with the assigned schedule.", timeIn, timeOut, shiftLabel);
    }

    private Map<LocalDate, AttendanceRecord> getAttendanceByDate(int employeeId) {
        Map<LocalDate, AttendanceRecord> records = new HashMap<>();
        for (AttendanceRecord record : attendanceService.getAttendanceHistory(employeeId)) {
            try {
                records.put(LocalDate.parse(record.getDate()), record);
            } catch (Exception ignored) {
            }
        }
        return records;
    }

    private Map<LocalDate, EmployeeSchedule> getSchedulesByDate(int employeeId, LocalDate start, LocalDate end) {
        Map<LocalDate, EmployeeSchedule> schedules = new HashMap<>();
        for (EmployeeSchedule schedule : scheduleService.getEmployeeSchedulesByDateRange(employeeId, start, end)) {
            if (schedule.getScheduleDate() != null) {
                schedules.put(schedule.getScheduleDate(), schedule);
            }
        }
        return schedules;
    }

    private Map<Integer, EmployeeShift> getShiftsById() {
        Map<Integer, EmployeeShift> shifts = new HashMap<>();
        for (EmployeeShift shift : scheduleService.getEmployeeShifts()) {
            shifts.put(shift.getShiftId(), shift);
        }
        return shifts;
    }

    private Map<LocalDate, Holiday> getHolidaysByDate(LocalDate start, LocalDate end) {
        Map<LocalDate, Holiday> holidays = new HashMap<>();
        for (Holiday holiday : scheduleService.getHolidays()) {
            if (holiday.getHolidayDate() != null
                    && !holiday.getHolidayDate().isBefore(start)
                    && !holiday.getHolidayDate().isAfter(end)) {
                holidays.put(holiday.getHolidayDate(), holiday);
            }
        }
        return holidays;
    }

    private boolean isLate(String timeIn, EmployeeShift shift) {
        try {
            LocalTime actual = LocalTime.parse(timeIn);
            LocalTime threshold = shift.getStartTime().plusMinutes(Math.max(0, shift.getGraceMinutes()));
            return actual.isAfter(threshold);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isUndertime(String timeOut, EmployeeShift shift) {
        try {
            LocalTime actual = LocalTime.parse(timeOut);
            return actual.isBefore(shift.getEndTime());
        } catch (Exception e) {
            return false;
        }
    }

    private String getShiftLabel(EmployeeSchedule schedule, EmployeeShift shift) {
        if (schedule == null) {
            return "No assigned schedule";
        }
        if (schedule.isRestDay()) {
            return "Rest Day";
        }
        if (shift == null) {
            return "Assigned Shift";
        }
        return shift.getShiftName() + " (" + shift.getStartTime() + " - " + shift.getEndTime() + ")";
    }

    private AttendanceAwareness normal(int employeeId, LocalDate date, String status, String message,
            String timeIn, String timeOut, String shiftLabel) {
        return new AttendanceAwareness(employeeId, date, status, AttendanceAwareness.Severity.NORMAL,
                message, timeIn, timeOut, shiftLabel);
    }

    private AttendanceAwareness info(int employeeId, LocalDate date, String status, String message,
            String timeIn, String timeOut, String shiftLabel) {
        return new AttendanceAwareness(employeeId, date, status, AttendanceAwareness.Severity.INFO,
                message, timeIn, timeOut, shiftLabel);
    }

    private AttendanceAwareness warning(int employeeId, LocalDate date, String status, String message,
            String timeIn, String timeOut, String shiftLabel) {
        return new AttendanceAwareness(employeeId, date, status, AttendanceAwareness.Severity.WARNING,
                message, timeIn, timeOut, shiftLabel);
    }

    private AttendanceAwareness critical(int employeeId, LocalDate date, String status, String message,
            String timeIn, String timeOut, String shiftLabel) {
        return new AttendanceAwareness(employeeId, date, status, AttendanceAwareness.Severity.CRITICAL,
                message, timeIn, timeOut, shiftLabel);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String plural(long count) {
        return count == 1 ? "" : "s";
    }
}
