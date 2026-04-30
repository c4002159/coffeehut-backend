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

/**
 * REST controller for store settings and live open/closed status.
 * <p>
 * Exposes public and staff-facing endpoints for reading store configuration,
 * managing Order Automation thresholds, and controlling the store's
 * live open/closed state via manual overrides, holiday exceptions, and
 * weekly opening hours. Priority order for status resolution:
 * manual close > manual force-open > holiday exception > weekly schedule.
 * </p>
 */
@RestController
@CrossOrigin(origins = "*")
public class StoreSettingsController {

    @Autowired private StoreSettingsRepository   storeSettingsRepository;
    @Autowired private ScheduleHoursRepository   hoursRepository;
    @Autowired private ScheduleHolidayRepository holidayRepository;

    // ── Order Automation (staff only) ───────────────────────────────────────

    /**
     * Returns the current store settings (Order Automation config).
     *
     * @return {@link StoreSettings} record with id=1, or 404 if not yet initialised
     */
    @GetMapping("/api/staff/settings")
    public ResponseEntity<StoreSettings> getSettings() {
        return storeSettingsRepository.findById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Saves Order Automation config without overwriting the temporary-close flag.
     * <p>
     * The {@code isTemporarilyClosed} field is preserved from the existing record
     * so that saving automation settings does not accidentally reopen the store.
     * </p>
     *
     * @param incoming updated {@link StoreSettings} payload from the staff portal
     * @return the saved {@link StoreSettings} record
     */
    @PostMapping("/api/staff/settings")
    public ResponseEntity<StoreSettings> saveSettings(@RequestBody StoreSettings incoming) {
        storeSettingsRepository.findById(1L).ifPresent(existing ->
                incoming.setIsTemporarilyClosed(existing.getIsTemporarilyClosed()));
        incoming.setId(1L);
        return ResponseEntity.ok(storeSettingsRepository.save(incoming));
    }

    // ── Store open/closed status ────────────────────────────────────────────

    /**
     * Returns the current live open/closed status of the store.
     * <p>
     * Evaluates three factors in priority order:
     * <ol>
     *   <li>Manual temporary close ({@code isTemporarilyClosed=true}) — highest priority</li>
     *   <li>Manual force-open ({@code manualForceOpen=true}) — overrides holiday and weekly schedule</li>
     *   <li>Holiday exceptions — always higher priority than weekly schedule</li>
     *   <li>Weekly schedule — lowest priority, used as the default</li>
     * </ol>
     * </p>
     *
     * @return map containing {@code isOpen}, {@code reason}, {@code todayHoursLabel},
     *         {@code openTime}, {@code closeTime}, and {@code isClosedToday}
     */
    @GetMapping("/api/store/status")
    public ResponseEntity<Map<String, Object>> getStoreStatus() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        String dayName  = toDayName(today.getDayOfWeek());

        StoreSettings settings = storeSettingsRepository.findById(1L).orElse(null);
        boolean tempClosed  = settings != null && Boolean.TRUE.equals(settings.getIsTemporarilyClosed());
        boolean forceOpen   = settings != null && Boolean.TRUE.equals(settings.getManualForceOpen());

        // Factor 1: manual temporarily closed — highest priority -WeiqiWang
        if (tempClosed) {
            return ResponseEntity.ok(buildStatus(false, "temporarily_closed",
                    "Temporarily Closed", null, null, true));
        }

        // Factor 2: holiday exceptions — checked before weekly, always higher priority than weekly -WeiqiWang
        List<ScheduleHoliday> holidays = holidayRepository.findAll();
        for (ScheduleHoliday h : holidays) {
            if (h.getStartDate() == null || h.getEndDate() == null) continue;
            LocalDate start = LocalDate.parse(h.getStartDate());
            LocalDate end   = LocalDate.parse(h.getEndDate());
            if (!today.isBefore(start) && !today.isAfter(end)) {
                // Holiday applies today — forceOpen can override it -WeiqiWang
                if (forceOpen) {
                    return ResponseEntity.ok(buildStatus(true, "manual_force_open", "Open Today", null, null, false));
                }
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

        // Factor 3: weekly schedule — forceOpen can override it -WeiqiWang
        List<ScheduleHours> hoursList = hoursRepository.findAll();
        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        for (ScheduleHours h : hoursList) {
            if (matchesDayLabel(h.getDayLabel(), dow)) {
                if (forceOpen) {
                    return ResponseEntity.ok(buildStatus(true, "manual_force_open", "Open Today", null, null, false));
                }
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

    /**
     * Sets the store's manual open/closed override from the staff portal.
     * <p>
     * When {@code isTemporarilyClosed=true}, the store is immediately closed for customers
     * regardless of holiday or weekly schedule. When {@code false} (Reopen Store),
     * {@code manualForceOpen} is set to {@code true} to override any schedule-based closure.
     * </p>
     *
     * @param body map containing {@code isTemporarilyClosed} boolean
     * @return map containing {@code isOpen} reflecting the new state
     */
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
        // Reopen Store button sets forceOpen=true; Temporarily Mark Closed clears it -WeiqiWang
        settings.setManualForceOpen(!closed);
        storeSettingsRepository.save(settings);
        return ResponseEntity.ok(Map.of("isOpen", !closed));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Checks whether {@code now} falls within the [openStr, closeStr) range.
     * <p>
     * Accepts both {@code "HH:mm"} (24-hr, new format) and legacy {@code "h:mm AM/PM"} (12-hr).
     * Returns {@code false} if either time string is {@code null} or unparseable.
     * </p>
     *
     * @param openStr  opening time string, e.g. {@code "09:00"} or {@code "9:00 AM"}
     * @param closeStr closing time string, e.g. {@code "18:00"} or {@code "6:00 PM"}
     * @param now      current local time to check against the range
     * @return {@code true} if {@code now} is within [open, close), {@code false} otherwise
     */
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

    /**
     * Parses a time string in either 24-hr ({@code "H:mm"}) or legacy 12-hr ({@code "h:mm a"}) format.
     *
     * @param str the time string to parse
     * @return parsed {@link LocalTime}, or {@code null} if unparseable or blank
     */
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

    /**
     * Returns {@code true} if the given {@code label} matches the provided {@link DayOfWeek}.
     * <p>
     * Supports three label patterns: {@code "Monday - Friday"}, {@code "Saturday"}, {@code "Sunday"}.
     * </p>
     *
     * @param label day label stored in {@code schedule_hours}, e.g. {@code "Monday - Friday"}
     * @param dow   current day of week
     * @return {@code true} if the label covers the given day
     */
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

    /**
     * Builds the standard store status response map.
     *
     * @param isOpen          whether the store is currently open
     * @param reason          machine-readable reason code, e.g. {@code "temporarily_closed"}
     * @param todayHoursLabel human-readable label for today's hours
     * @param openTime        opening time string, or {@code null} if not applicable
     * @param closeTime       closing time string, or {@code null} if not applicable
     * @param isClosedToday   whether the store is closed for the entire day
     * @return ordered map suitable for JSON serialisation
     */
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

    /**
     * Formats a human-readable opening hours label.
     *
     * @param openTime  opening time string
     * @param closeTime closing time string
     * @return label such as {@code "Open Today 09:00 - 18:00"}, or {@code "Open Today"} if times are null
     */
    private String formatOpenLabel(String openTime, String closeTime) {
        if (openTime == null || closeTime == null) return "Open Today";
        return "Open Today " + openTime + " - " + closeTime;
    }

    /**
     * Formats the suffix appended to "Closed Today" messages.
     *
     * @param holidayName name of the holiday exception, may be blank
     * @param dayName     current day name as fallback
     * @return suffix such as {@code " (Christmas)"} or {@code " (Wednesday)"}
     */
    private String formatHolidaySuffix(String holidayName, String dayName) {
        if (holidayName == null || holidayName.isBlank()) return " (" + dayName + ")";
        return " (" + holidayName + ")";
    }

    /**
     * Converts a {@link DayOfWeek} enum value to its English name.
     *
     * @param dow day of week enum
     * @return English day name, e.g. {@code "Monday"}
     */
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
