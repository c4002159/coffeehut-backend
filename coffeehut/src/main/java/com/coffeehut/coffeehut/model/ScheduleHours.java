// ScheduleHours.java — JPA entity for weekly opening hours -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing the store's regular weekly opening hours.
 * <p>
 * Contains exactly three fixed rows (Monday-Friday, Saturday, Sunday),
 * identified by {@code id} values 1, 2, and 3 respectively. These rows
 * are never inserted or deleted at runtime — only updated by staff.
 * </p>
 */
@Entity
@Table(name = "schedule_hours")
@Data
public class ScheduleHours {

    /**
     * Fixed primary key identifying the day group.
     * <p>
     * {@code 1} = Monday–Friday, {@code 2} = Saturday, {@code 3} = Sunday.
     * </p>
     */
    // 1 = Mon-Fri, 2 = Saturday, 3 = Sunday — fixed -WeiqiWang
    @Id
    private Long id;

    /** Display label shown in the staff schedule UI (e.g. {@code "Monday - Friday"}). */
    private String dayLabel;

    /** Opening time for this day group (e.g. {@code "09:00"}); {@code null} when {@code isClosed} is {@code true}. */
    private String openTime;

    /** Closing time for this day group (e.g. {@code "18:00"}); {@code null} when {@code isClosed} is {@code true}. */
    private String closeTime;

    /** Whether the store is closed for this entire day group. */
    private Boolean isClosed;
}