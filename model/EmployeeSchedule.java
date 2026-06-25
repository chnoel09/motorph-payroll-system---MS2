package com.mycompany.oop.model;

import java.time.LocalDate;

// Prepared for the future normalized employee_schedules scheduling migration.
public class EmployeeSchedule {

    private int scheduleId;
    private int scheduleBatchId;
    private int employeeId;
    private Integer shiftId;
    private LocalDate scheduleDate;
    private boolean restDay;
    private Integer holidayId;
    private String status;

    public EmployeeSchedule() {
    }

    public EmployeeSchedule(int scheduleId, int scheduleBatchId, int employeeId, Integer shiftId,
            LocalDate scheduleDate, boolean restDay, Integer holidayId, String status) {
        this.scheduleId = scheduleId;
        this.scheduleBatchId = scheduleBatchId;
        this.employeeId = employeeId;
        this.shiftId = shiftId;
        this.scheduleDate = scheduleDate;
        this.restDay = restDay;
        this.holidayId = holidayId;
        this.status = status;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getScheduleBatchId() {
        return scheduleBatchId;
    }

    public void setScheduleBatchId(int scheduleBatchId) {
        this.scheduleBatchId = scheduleBatchId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getShiftId() {
        return shiftId;
    }

    public void setShiftId(Integer shiftId) {
        this.shiftId = shiftId;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public boolean isRestDay() {
        return restDay;
    }

    public void setRestDay(boolean restDay) {
        this.restDay = restDay;
    }

    public Integer getHolidayId() {
        return holidayId;
    }

    public void setHolidayId(Integer holidayId) {
        this.holidayId = holidayId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
