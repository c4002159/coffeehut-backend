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
//
// Time values are stored in "HH:mm" 24-hour format.
// Legacy "h:mm AM/PM" values already in DB are also handled gracefully. -WeiqiWang

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
import java.util.LinkedHashMap;
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

    @GetMapping("/api/store/status")
    public ResponseEntity<Map<String, Object>> getStoreStatus() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        String dayName  = toDayName(today.getDayOfWeek());

        // Factor 1: manual override -WeiqiWang
        boolean tempClosed = storeSettingsRepository.findById(1L)
                .map(s -> Boolean.TRUE.equals(s.getIsTemporarilyClosed()))
                .orElse(false);
        if (tempClosed) {
            return ResponseEntity.ok(buildStatus(false, "temporarily_closed",
                    "Temporarily Closed", null, null, true));
        }

        // Factor 2 & 3: holiday exceptions -WeiqiWang
        List<ScheduleHoliday> holidays = holidayRepository.findAll();
        for (ScheduleHoliday h : holidays) {
            if (h.getStartDate() == null || h.getEndDate() == null) continue;
            LocalDate start = LocalDate.parse(h.getStartDate());
            LocalDate end   = LocalDate.parse(h.getEndDate());
            if (!today.isBefore(start) && !today.isAfter(end)) {
                if (Boolean.TRUE.equals(h.getIsClosed())) {
                    return ResponseEntity.ok(buildStatus(false, "holiday",
                            "Closed Today" + formatHolidaySuffix(h.getName(), dayName),
                            null, null, true));
                }
                boolean open = isWithinTimeRange(h.getOpenTime(), h.getCloseTime(), now);
                return ResponseEntity.ok(buildStatus(open, "holiday_custom_hours",
                        formatOpenLabel(h.getOpenTime(), h.getCloseTime()),
                        h.getOpenTime(), h.getCloseTime(), false));
            }
        }

        // Factor 4: weekly schedule -WeiqiWang
        List<ScheduleHours> hoursList = hoursRepository.findAll();
        DayOfWeek dow = LocalDate.now().getDayOfWeek();

        for (ScheduleHours h : hoursList) {
            if (matchesDayLabel(h.getDayLabel(), dow)) {
                if (Boolean.TRUE.equals(h.getIsClosed())) {
                    return ResponseEntity.ok(buildStatus(false, "weekly_closed",
                            "Closed Today (" + dayName + ")", null, null, true));
                }
                boolean open = isWithinTimeRange(h.getOpenTime(), h.getCloseTime(), now);
                return ResponseEntity.ok(buildStatus(open, "weekly_hours",
                        formatOpenLabel(h.getOpenTime(), h.getCloseTime()),
                        h.getOpenTime(), h.getCloseTime(), false));
            }
        }

        return ResponseEntity.ok(buildStatus(true, "no_schedule", "Open Today", null, null, false));
    }

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

    // Parses "HH:mm" (24-hr, new format) or legacy "h:mm AM/PM" (12-hr).
    // Checks if `now` falls within [open, close). -WeiqiWang
    private boolean isWithinTimeRange(String openStr, String closeStr, LocalTime now) {
        if (openStr == null || closeStr == null) return false;
        try {
            LocalTime open  = parseTimeFlexible(openStr);
            LocalTime close = parseTimeFlexible(closeStr);
            if (open == null || close == null) return false;
            return !now.isBefore(open) && now.isBefore(close);
        } catch (Exception e) {
            return false;
        }
    }

    // Parses both "HH:mm" (24-hr) and legacy "h:mm AM/PM" (12-hr). -WeiqiWang
    private LocalTime parseTimeFlexible(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            // Try 24-hr first ("HH:mm" or "H:mm")
            return LocalTime.parse(str, DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ignored) { /* fall through */ }
        try {
            // Legacy 12-hr fallback ("h:mm AM/PM") -WeiqiWang
            return LocalTime.parse(str.toUpperCase(), DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            return null;
        }
    }

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

    private Map<String, Object> buildStatus(boolean isOpen, String reason, String todayHoursLabel,
                                            String openTime, String closeTime, boolean isClosedToday) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isOpen", isOpen);
        body.put("reason", reason);
        body.put("todayHoursLabel", todayHoursLabel);
        body.put("openTime", openTime);
        body.put("closeTime", closeTime);
        body.put("isClosedToday", isClosedToday);
        return body;
    }

    private String formatOpenLabel(String openTime, String closeTime) {
        if (openTime == null || closeTime == null) return "Open Today";
        return "Open Today " + openTime + " - " + closeTime;
    }

    private String formatHolidaySuffix(String holidayName, String dayName) {
        if (holidayName == null || holidayName.isBlank()) return " (" + dayName + ")";
        return " (" + holidayName + ")";
    }

    private String toDayName(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "Monday";
            case TUESDAY -> "Tuesday";
            case WEDNESDAY -> "Wednesday";
            case THURSDAY -> "Thursday";
            case FRIDAY -> "Friday";
            case SATURDAY -> "Saturday";
            case SUNDAY -> "Sunday";
        };
    }
}
