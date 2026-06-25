/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.model;

public class AttendanceRecord {

    private int employeeId;
    private String date;
    private String timeIn;
    private String timeOut;

    public AttendanceRecord(int 
            employeeId, 
            String date, 
            String timeIn, 
            String timeOut) 
    {
        this.employeeId = employeeId;
        this.date = safeTrim(date);
        this.timeIn = safeTrim(timeIn);
        this.timeOut = safeTrim(timeOut);
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getDate() {
        return date;
    }

    public String getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(String timeIn) {
        this.timeIn = safeTrim(timeIn);
    }

    public String getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(String timeOut) {
        this.timeOut = safeTrim(timeOut);
    }

    // ================= HELPER =================
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}