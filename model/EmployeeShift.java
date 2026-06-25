package com.mycompany.oop.model;

import java.time.LocalDate;
import java.time.LocalTime;

// Prepared for the future normalized employee_shifts scheduling migration.
public class EmployeeShift {

    private int shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private int graceMinutes;
    private LocalDate effectiveDate;
    private Integer assignedBy;

    public EmployeeShift() {
    }

    public EmployeeShift(int shiftId, String shiftName, LocalTime startTime, LocalTime endTime,
            int graceMinutes, LocalDate effectiveDate, Integer assignedBy) {
        this.shiftId = shiftId;
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.graceMinutes = graceMinutes;
        this.effectiveDate = effectiveDate;
        this.assignedBy = assignedBy;
    }

    public int getShiftId() {
        return shiftId;
    }

    public void setShiftId(int shiftId) {
        this.shiftId = shiftId;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getGraceMinutes() {
        return graceMinutes;
    }

    public void setGraceMinutes(int graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Integer getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Integer assignedBy) {
        this.assignedBy = assignedBy;
    }
}
