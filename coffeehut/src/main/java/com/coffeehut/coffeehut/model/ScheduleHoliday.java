// ScheduleHoliday.java — JPA entity for holiday exceptions -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing a holiday or special-hours exception.
 * <p>
 * Each row defines a named date range during which the store either closes
 * entirely or operates on custom hours. Holiday exceptions take precedence
 * over the regular weekly schedule defined in {@link ScheduleHours}.
 * </p>
 */
@Entity
@Table(name = "schedule_holidays")
@Data
public class ScheduleHoliday {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable label for the holiday (e.g. {@code "Christmas"}). */
    private String name;

    /** Start date of the holiday period as an ISO date string (e.g. {@code "2026-12-25"}). */
    private String startDate;

    /** End date of the holiday period as an ISO date string (e.g. {@code "2026-12-26"}); may equal {@code startDate}. */
    private String endDate;

    /** Whether the store is fully closed during this period. When {@code true}, {@code openTime} and {@code closeTime} are ignored. */
    private Boolean isClosed;

    /** Custom opening time during the holiday (e.g. {@code "09:00"}); {@code null} when {@code isClosed} is {@code true}. */
    private String openTime;

    /** Custom closing time during the holiday (e.g. {@code "17:00"}); {@code null} when {@code isClosed} is {@code true}. */
    private String closeTime;
}