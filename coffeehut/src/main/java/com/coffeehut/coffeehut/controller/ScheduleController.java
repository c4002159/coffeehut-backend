// ScheduleController.java — Staff schedule management endpoints -WeiqiWang
//
// GET  /api/staff/schedule/hours              — read weekly hours (3 rows)
// POST /api/staff/schedule/hours              — save all 3 weekly hour rows
// GET  /api/staff/schedule/holidays           — list all holiday exceptions
// POST /api/staff/schedule/holidays           — add one holiday exception
// DELETE /api/staff/schedule/holidays/{id}    — delete one holiday exception
// DELETE /api/staff/schedule/holidays         — clear all holiday exceptions
//
// All under /api/staff/ — completely isolated from customer endpoints. -WeiqiWang

package com.coffeehut.coffeehut.controller;

import com.coffeehut.coffeehut.model.ScheduleHoliday;
import com.coffeehut.coffeehut.model.ScheduleHours;
import com.coffeehut.coffeehut.repository.ScheduleHolidayRepository;
import com.coffeehut.coffeehut.repository.ScheduleHoursRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/schedule")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleHoursRepository hoursRepository;

    @Autowired
    private ScheduleHolidayRepository holidayRepository;

    // ── Weekly Hours ────────────────────────────────────────────────────────

    @GetMapping("/hours")
    public List<ScheduleHours> getHours() {
        return hoursRepository.findAll();
    }

    // Accepts a list of 3 rows; saves each by id (upsert). -WeiqiWang
    @PostMapping("/hours")
    public List<ScheduleHours> saveHours(@RequestBody List<ScheduleHours> rows) {
        return hoursRepository.saveAll(rows);
    }

    // ── Holiday Exceptions ──────────────────────────────────────────────────

    @GetMapping("/holidays")
    public List<ScheduleHoliday> getHolidays() {
        return holidayRepository.findAll();
    }

    @PostMapping("/holidays")
    public ScheduleHoliday addHoliday(@RequestBody ScheduleHoliday holiday) {
        holiday.setId(null); // force insert, never overwrite -WeiqiWang
        return holidayRepository.save(holiday);
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        if (!holidayRepository.existsById(id)) return ResponseEntity.notFound().build();
        holidayRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/holidays")
    public ResponseEntity<Void> clearAllHolidays() {
        holidayRepository.deleteAll();
        return ResponseEntity.ok().build();
    }
}
