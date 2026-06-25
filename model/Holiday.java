package com.mycompany.oop.model;

import java.time.LocalDate;

// Prepared for the future normalized holidays scheduling migration.
public class Holiday {

    private int holidayId;
    private String holidayName;
    private LocalDate holidayDate;
    private String holidayType;
    private double multiplier;

    public Holiday() {
    }

    public Holiday(int holidayId, String holidayName, LocalDate holidayDate,
            String holidayType, double multiplier) {
        this.holidayId = holidayId;
        this.holidayName = holidayName;
        this.holidayDate = holidayDate;
        this.holidayType = holidayType;
        this.multiplier = multiplier;
    }

    public int getHolidayId() {
        return holidayId;
    }

    public void setHolidayId(int holidayId) {
        this.holidayId = holidayId;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}
