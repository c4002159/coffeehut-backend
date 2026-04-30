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

/**
 * REST controller for managing the store's weekly schedule and holiday exceptions.
 * <p>
 * All endpoints are staff-only ({@code /api/staff/schedule/}) and are not accessible
 * from the customer-facing interface. Any modification to hours or holidays automatically
 * clears manual open/close overrides so that the updated schedule takes effect immediately.
 * </p>
 */
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

    /**
     * Returns all weekly opening hour rows (typically 3: Mon-Fri, Saturday, Sunday).
     *
     * @return list of {@link ScheduleHours} records
     */
    @GetMapping("/hours")
    public List<ScheduleHours> getHours() {
        return hoursRepository.findAll();
    }

    /**
     * Saves (upserts) the full set of weekly opening hour rows.
     * <p>
     * Expects exactly 3 rows covering Monday–Friday, Saturday, and Sunday.
     * After saving, clears {@code manualForceOpen} and {@code isTemporarilyClosed}
     * so that the new schedule takes effect immediately without requiring
     * a separate staff action on the Manage Schedule page.
     * </p>
     *
     * @param rows list of {@link ScheduleHours} rows to upsert
     * @return the saved list of {@link ScheduleHours} records
     */
    @PostMapping("/hours")
    public List<ScheduleHours> saveHours(@RequestBody List<ScheduleHours> rows) {
        List<ScheduleHours> saved = hoursRepository.saveAll(rows);
        clearManualOverrides();
        return saved;
    }

    // ── Holiday Exceptions ──────────────────────────────────────────────────

    /**
     * Returns all holiday exception records.
     *
     * @return list of {@link ScheduleHoliday} records
     */
    @GetMapping("/holidays")
    public List<ScheduleHoliday> getHolidays() {
        return holidayRepository.findAll();
    }

    /**
     * Adds a new holiday exception.
     * <p>
     * Forces an INSERT by clearing the incoming id, so existing records are never
     * accidentally overwritten. Clears manual overrides after saving so the new
     * holiday takes effect immediately.
     * </p>
     *
     * @param holiday the holiday exception to add
     * @return the persisted {@link ScheduleHoliday} record with its generated id
     */
    @PostMapping("/holidays")
    public ScheduleHoliday addHoliday(@RequestBody ScheduleHoliday holiday) {
        holiday.setId(null); // force insert, never overwrite -WeiqiWang
        ScheduleHoliday saved = holidayRepository.save(holiday);
        clearManualOverrides();
        return saved;
    }

    /**
     * Deletes a single holiday exception by id.
     * <p>
     * Clears manual overrides after deletion so the remaining schedule
     * takes effect immediately.
     * </p>
     *
     * @param id the id of the {@link ScheduleHoliday} to delete
     * @return 200 OK on success, 404 if the record does not exist
     */
    @DeleteMapping("/holidays/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        if (!holidayRepository.existsById(id)) return ResponseEntity.notFound().build();
        holidayRepository.deleteById(id);
        clearManualOverrides();
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes all holiday exceptions.
     * <p>
     * Clears manual overrides after deletion so the weekly schedule
     * resumes as the active source of truth.
     * </p>
     *
     * @return 200 OK on success
     */
    @DeleteMapping("/holidays")
    public ResponseEntity<Void> clearAllHolidays() {
        holidayRepository.deleteAll();
        clearManualOverrides();
        return ResponseEntity.ok().build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Clears all manual open/closed overrides on the {@link StoreSettings} record.
     * <p>
     * Sets both {@code manualForceOpen} and {@code isTemporarilyClosed} to {@code false},
     * allowing Holiday exceptions and Weekly Schedule to determine the live store status
     * without interference from previous staff button presses.
     * Called automatically whenever hours or holidays are modified.
     * </p>
     */
    private void clearManualOverrides() {
        storeSettingsRepository.findById(1L).ifPresent(s -> {
            s.setManualForceOpen(false);
            s.setIsTemporarilyClosed(false); // also clear manual close so schedule can take effect -WeiqiWang
            storeSettingsRepository.save(s);
        });
    }
}
