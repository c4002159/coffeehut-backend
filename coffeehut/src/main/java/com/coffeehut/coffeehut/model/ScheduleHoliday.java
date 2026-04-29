// ScheduleHoliday.java — JPA entity for holiday exceptions -WeiqiWang
// Each row is one custom holiday exception added by staff.
// Completely separate from items / members / orders tables.

package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "schedule_holidays")
@Data
public class ScheduleHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;       // e.g. "Christmas"
    private String startDate;  // ISO date string e.g. "2026-12-25"
    private String endDate;    // ISO date string e.g. "2026-12-26" (may equal startDate)
    private Boolean isClosed;  // true = Closed All Day
    private String openTime;   // e.g. "09:00"  (null when isClosed = true)
    private String closeTime;  // e.g. "17:00"  (null when isClosed = true)
}
