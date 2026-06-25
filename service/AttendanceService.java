/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.repository.AttendanceDatabaseRepository;
import com.mycompany.oop.repository.AttendanceRepository;

public class AttendanceService {

    private AttendanceRepository repository;

    private static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    public AttendanceService() {
        this.repository = new AttendanceDatabaseRepository();
    }

    public void timeIn(int employeeId) {
        String today = LocalDate.now().format(ISO_DATE_FMT);
        String now = LocalTime.now().format(TIME_FMT);

        AttendanceRecord existing = repository.findByEmployeeAndDate(employeeId, today);

        if (existing == null) {
            repository.saveAttendance(new AttendanceRecord(employeeId, today, now, ""));
        }
    }

    public void timeOut(int employeeId) {
        String today = LocalDate.now().format(ISO_DATE_FMT);
        String now = LocalTime.now().format(TIME_FMT);

        AttendanceRecord existing = repository.findByEmployeeAndDate(employeeId, today);

        if (existing != null && (existing.getTimeOut() == null || existing.getTimeOut().isEmpty())) {
            existing.setTimeOut(now);
            repository.updateAttendance(existing);
        }
    }

    public List<AttendanceRecord> getAttendanceHistory(int employeeId) {
        return repository.findByEmployeeId(employeeId);
    }

    public AttendanceRecord getAttendanceForDate(int employeeId, LocalDate date) {
        if (employeeId <= 0 || date == null) {
            return null;
        }

        return repository.findByEmployeeAndDate(employeeId, date.format(ISO_DATE_FMT));
    }

    public List<AttendanceRecord> getAttendanceHistoryByCutoff(int employeeId, String cutoffPeriod) {
        List<AttendanceRecord> filtered = new ArrayList<>();

        if (cutoffPeriod == null || cutoffPeriod.trim().isEmpty()) {
            return getAttendanceHistory(employeeId);
        }

        LocalDate[] range = parseCutoffRange(cutoffPeriod);
        LocalDate start = range[0];
        LocalDate end = range[1];

        for (AttendanceRecord record : repository.findByEmployeeId(employeeId)) {
            try {
                LocalDate recordDate = LocalDate.parse(record.getDate(), ISO_DATE_FMT);

                if (!recordDate.isBefore(start) && !recordDate.isAfter(end)) {
                    filtered.add(record);
                }
            } catch (Exception e) {
                // skip invalid rows
            }
        }

        return filtered;
    }

    public List<String> getAvailableCutoffs() {
        Set<YearMonth> months = new TreeSet<>();

        for (AttendanceRecord record : repository.findAll()) {
            try {
                LocalDate date = LocalDate.parse(record.getDate(), ISO_DATE_FMT);
                months.add(YearMonth.from(date));
            } catch (Exception e) {
                // skip invalid rows
            }
        }

        List<String> cutoffs = new ArrayList<>();

        for (YearMonth ym : months) {
            String prefix = ym.format(MONTH_FMT) + "-" + ym.getYear();
            cutoffs.add(prefix + "-1st");
            cutoffs.add(prefix + "-2nd");
        }

        return cutoffs;
    }

    public List<String> getAvailableCutoffsForEmployee(int employeeId) {
        Set<YearMonth> months = new TreeSet<>();

        for (AttendanceRecord record : repository.findByEmployeeId(employeeId)) {
            try {
                LocalDate date = LocalDate.parse(record.getDate(), ISO_DATE_FMT);
                months.add(YearMonth.from(date));
            } catch (Exception e) {
                // skip invalid rows
            }
        }

        List<String> cutoffs = new ArrayList<>();

        for (YearMonth ym : months) {
            String prefix = ym.format(MONTH_FMT) + "-" + ym.getYear();
            cutoffs.add(prefix + "-1st");
            cutoffs.add(prefix + "-2nd");
        }

        return cutoffs;
    }

    public double getHoursForCutoff(int employeeId, String cutoffPeriod) {
        double totalHours = 0.0;

        for (AttendanceRecord record : getAttendanceHistoryByCutoff(employeeId, cutoffPeriod)) {
            totalHours += getHoursWorked(record);
        }

        return totalHours;
    }

    public double getHoursForDateRange(int employeeId, LocalDate start, LocalDate end) {
        double totalHours = 0.0;

        if (start == null || end == null || end.isBefore(start)) {
            return totalHours;
        }

        for (AttendanceRecord record : repository.findByEmployeeId(employeeId)) {
            try {
                LocalDate recordDate = LocalDate.parse(record.getDate(), ISO_DATE_FMT);

                if (!recordDate.isBefore(start) && !recordDate.isAfter(end)) {
                    totalHours += getHoursWorked(record);
                }
            } catch (Exception e) {
                // skip invalid rows
            }
        }

        return totalHours;
    }

    public LocalDate getCutoffStartDate(String cutoffPeriod) {
        return parseCutoffRange(cutoffPeriod)[0];
    }

    public LocalDate getCutoffEndDate(String cutoffPeriod) {
        return parseCutoffRange(cutoffPeriod)[1];
    }

    public double getHoursWorked(AttendanceRecord record) {
        try {
            if (record.getTimeIn() == null || record.getTimeIn().isEmpty()
                    || record.getTimeOut() == null || record.getTimeOut().isEmpty()) {
                return 0.0;
            }

            LocalTime timeIn = LocalTime.parse(normalizeTime(record.getTimeIn()));
            LocalTime timeOut = LocalTime.parse(normalizeTime(record.getTimeOut()));

            long minutes = Duration.between(timeIn, timeOut).toMinutes();

            if (minutes < 0) {
                return 0.0;
            }

            return minutes / 60.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private LocalDate[] parseCutoffRange(String cutoffPeriod) {
        String[] parts = cutoffPeriod.split("-");
        Month month = Month.from(MONTH_FMT.parse(parts[0]));
        int year = Integer.parseInt(parts[1]);
        boolean firstHalf = parts[2].equals("1st");

        LocalDate start;
        LocalDate end;

        if (firstHalf) {
            start = LocalDate.of(year, month, 1);
            end = LocalDate.of(year, month, 15);
        } else {
            start = LocalDate.of(year, month, 16);
            end = YearMonth.of(year, month).atEndOfMonth();
        }

        return new LocalDate[]{start, end};
    }

    private String normalizeTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return "";
        }

        try {
            return LocalTime.parse(time.trim()).format(TIME_FMT);
        } catch (Exception e) {
            return time.trim();
        }
    }
}
