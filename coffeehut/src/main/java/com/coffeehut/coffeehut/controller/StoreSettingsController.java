// StoreSettingsController.java -WeiqiWang
//
// GET  /api/staff/settings          — read Order Automation config (staff)
// POST /api/staff/settings          — save Order Automation config (staff)
// GET  /api/store/status            — PUBLIC: is the store open right now?
// POST /api/staff/store/status      — set temporarily closed flag (staff)
//
// GET /api/store/status combines three factors: -WeiqiWang
//   1. isTemporarilyClosed flag (manual override by staff)
//   2. Today's day of week vs weekly schedule_hours
//   3. Today's date vs schedule_holidays

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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class StoreSettingsController {

    @Autowired private StoreSettingsRepository   storeSettingsRepository;
    @Autowired private ScheduleHoursRepository   hoursRepository;
    @Autowired private ScheduleHolidayRepository holidayRepository;

    // ── Order Automation (staff only) ───────────────────────────────────────

    @GetMapping("/api/staff/settings")
    public ResponseEntity<StoreSettings> getSettings() {
        return storeSettingsRepository.findById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/staff/settings")
    public ResponseEntity<StoreSettings> saveSettings(@RequestBody StoreSettings incoming) {
        storeSettingsRepository.findById(1L).ifPresent(existing ->
                incoming.setIsTemporarilyClosed(existing.getIsTemporarilyClosed()));
        incoming.setId(1L);
        return ResponseEntity.ok(storeSettingsRepository.save(incoming));
    }

    // ── Store open/closed status ────────────────────────────────────────────

    // PUBLIC — customer client calls this to decide whether to allow ordering. -WeiqiWang
    // Returns { "isOpen": true/false }
    // Logic priority:
    //   1. If staff manually marked closed → closed
    //   2. If today matches a holiday exception that is "Closed All Day" → closed
    //   3. If today matches a holiday exception with custom hours → use those hours
    //   4. Otherwise use the weekly schedule for today's day of week
    @GetMapping("/api/store/status")
    public ResponseEntity<Map<String, Object>> getStoreStatus() {
        // Factor 1: manual override -WeiqiWang
        boolean tempClosed = storeSettingsRepository.findById(1L)
                .map(s -> Boolean.TRUE.equals(s.getIsTemporarilyClosed()))
                .orElse(false);
        if (tempClosed) {
            return ResponseEntity.ok(Map.of("isOpen", false, "reason", "temporarily_closed"));
        }

        String todayStr = LocalDate.now().toString(); // "2026-04-27"
        LocalTime now   = LocalTime.now();

        // Factor 2 & 3: holiday exceptions -WeiqiWang
        List<ScheduleHoliday> holidays = holidayRepository.findAll();
        for (ScheduleHoliday h : holidays) {
            if (h.getStartDate() == null || h.getEndDate() == null) continue;
            LocalDate start = LocalDate.parse(h.getStartDate());
            LocalDate end   = LocalDate.parse(h.getEndDate());
            LocalDate today = LocalDate.now();
            if (!today.isBefore(start) && !today.isAfter(end)) {
                // Today is within this holiday range
                if (Boolean.TRUE.equals(h.getIsClosed())) {
                    return ResponseEntity.ok(Map.of("isOpen", false, "reason", "holiday"));
                }
                // Holiday with custom hours
                boolean open = isWithinTimeRange(h.getOpenTime(), h.getCloseTime(), now);
                return ResponseEntity.ok(Map.of("isOpen", open, "reason", "holiday_custom_hours"));
            }
        }

        // Factor 4: weekly schedule -WeiqiWang
        List<ScheduleHours> hoursList = hoursRepository.findAll();
        DayOfWeek dow = LocalDate.now().getDayOfWeek();

        for (ScheduleHours h : hoursList) {
            if (matchesDayLabel(h.getDayLabel(), dow)) {
                if (Boolean.TRUE.equals(h.getIsClosed())) {
                    return ResponseEntity.ok(Map.of("isOpen", false, "reason", "weekly_closed"));
                }
                boolean open = isWithinTimeRange(h.getOpenTime(), h.getCloseTime(), now);
                return ResponseEntity.ok(Map.of("isOpen", open, "reason", "weekly_hours"));
            }
        }

        // No schedule configured → default open -WeiqiWang
        return ResponseEntity.ok(Map.of("isOpen", true, "reason", "no_schedule"));
    }

    // STAFF ONLY — toggle temporary close flag -WeiqiWang
    @PostMapping("/api/staff/store/status")
    public ResponseEntity<Map<String, Boolean>> setStoreStatus(
            @RequestBody Map<String, Boolean> body) {
        boolean closed = Boolean.TRUE.equals(body.get("isTemporarilyClosed"));
        StoreSettings settings = storeSettingsRepository.findById(1L).orElseGet(() -> {
            StoreSettings s = new StoreSettings();
            s.setId(1L);
            s.setAutoCancelEnabled(true);
            s.setAutoCancelMins(15);
            s.setAutoCollectEnabled(true);
            s.setAutoCollectMins(15);
            return s;
        });
        settings.setIsTemporarilyClosed(closed);
        storeSettingsRepository.save(settings);
        return ResponseEntity.ok(Map.of("isOpen", !closed));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    // Parses "9:00 AM" / "6:00 PM" and checks if `now` falls within [open, close). -WeiqiWang
    private boolean isWithinTimeRange(String openStr, String closeStr, LocalTime now) {
        if (openStr == null || closeStr == null) return false;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a");
            LocalTime open  = LocalTime.parse(openStr.toUpperCase(),  fmt);
            LocalTime close = LocalTime.parse(closeStr.toUpperCase(), fmt);
            return !now.isBefore(open) && now.isBefore(close);
        } catch (Exception e) {
            return false;
        }
    }

    // Maps a dayLabel string to a DayOfWeek. -WeiqiWang
    // "Monday - Friday" matches Mon–Fri; "Saturday" matches Sat; "Sunday" matches Sun.
    private boolean matchesDayLabel(String label, DayOfWeek dow) {
        if (label == null) return false;
        String l = label.toLowerCase();
        if (l.contains("monday") && l.contains("friday")) {
            return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        }
        if (l.contains("saturday")) return dow == DayOfWeek.SATURDAY;
        if (l.contains("sunday"))   return dow == DayOfWeek.SUNDAY;
        return false;
    }
}
