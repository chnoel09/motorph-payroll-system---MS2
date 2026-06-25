package com.mycompany.oop.repository;

import java.time.LocalDate;
import java.util.List;

import com.mycompany.oop.model.Holiday;

// Contract prepared for future normalized holiday scheduling.
public interface HolidayRepository {

    Holiday findById(int holidayId);

    List<Holiday> findAll();

    List<Holiday> findByDateRange(LocalDate startDate, LocalDate endDate);

    void add(Holiday holiday);

    void update(Holiday holiday);

    void delete(int holidayId);
}
