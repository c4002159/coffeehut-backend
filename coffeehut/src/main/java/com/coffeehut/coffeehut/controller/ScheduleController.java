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
import com.coffeehut.coffeehut.model.StoreSettings;
import com.coffeehut.coffeehut.repository.ScheduleHolidayRepository;
import com.coffeehut.coffeehut.repository.ScheduleHoursRepository;
import com.coffeehut.coffeehut.repository.StoreSettingsRepository;
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

    @Autowired
    private StoreSettingsRepository storeSettingsRepository;

    // ── Weekly Hours ────────────────────────────────────────────────────────

    @GetMapping("/hours")
    public List<ScheduleHours> getHours() {
        return hoursRepository.findAll();
    }

    // Accepts a list of 3 rows; saves each by id (upsert). -WeiqiWang
    // Clears manualForceOpen so the updated schedule takes effect immediately. -WeiqiWang
    @PostMapping("/hours")
    public List<ScheduleHours> saveHours(@RequestBody List<ScheduleHours> rows) {
        List<ScheduleHours> saved = hoursRepository.saveAll(rows);
        clearManualForceOpen();
        return saved;
    }

    // ── Holiday Exceptions ──────────────────────────────────────────────────

    @GetMapping("/holidays")
    public List<ScheduleHoliday> getHolidays() {
        return holidayRepository.findAll();
    }

    @PostMapping("/holidays")
    public ScheduleHoliday addHoliday(@RequestBody ScheduleHoliday holiday) {
        holiday.setId(null); // force insert, never overwrite -WeiqiWang
        ScheduleHoliday saved = holidayRepository.save(holiday);
        clearManualForceOpen();
        return saved;
    }

    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        if (!holidayRepository.existsById(id)) return ResponseEntity.notFound().build();
        holidayRepository.deleteById(id);
        clearManualForceOpen();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/holidays")
    public ResponseEntity<Void> clearAllHolidays() {
        holidayRepository.deleteAll();
        clearManualForceOpen();
        return ResponseEntity.ok().build();
    }

    // Clears manual overrides so Holiday/Weekly schedule takes effect again. -WeiqiWang
    private void clearManualForceOpen() {
        storeSettingsRepository.findById(1L).ifPresent(s -> {
            s.setManualForceOpen(false);
            s.setIsTemporarilyClosed(false); // also clear manual close so schedule can take effect -WeiqiWang
            storeSettingsRepository.save(s);
        });
    }
}
