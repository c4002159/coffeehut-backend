// ScheduleHours.java — JPA entity for weekly opening hours -WeiqiWang
// Stores the 3 fixed rows: Monday-Friday, Saturday, Sunday.
// Completely separate from items / members / orders tables.

package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "schedule_hours")
@Data
public class ScheduleHours {

    @Id
    private Long id; // 1 = Mon-Fri, 2 = Saturday, 3 = Sunday — fixed -WeiqiWang

    private String dayLabel;   // e.g. "Monday - Friday"
    private String openTime;   // e.g. "9:00 AM"  (null when isClosed = true)
    private String closeTime;  // e.g. "6:00 PM"  (null when isClosed = true)
    private Boolean isClosed;  // true = Closed All Day
}
