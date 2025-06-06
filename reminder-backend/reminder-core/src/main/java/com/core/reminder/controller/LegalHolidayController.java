package com.core.reminder.controller;

import com.core.reminder.model.LegalHoliday;
import com.core.reminder.service.LegalHolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class LegalHolidayController {

    @Autowired
    private LegalHolidayService legalHolidayService;

    @GetMapping
    public ResponseEntity<List<LegalHoliday>> getHolidaysByYearRange(
            @RequestParam Integer startYear,
            @RequestParam Integer endYear) {
        List<LegalHoliday> holidays = legalHolidayService.getHolidaysByYearRange(startYear, endYear);
        return ResponseEntity.ok(holidays);
    }
} 