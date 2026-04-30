// ScheduleHoursRepository.java -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.ScheduleHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ScheduleHours} entities.
 * <p>
 * This interface provides data access operations for the schedule_hours table
 * using Spring Data JPA. By extending {@link JpaRepository}, it inherits standard
 * CRUD functionality such as saving, updating, deleting, and retrieving records.
 * </p>
 * <p>
 * It is commonly used in scheduling-related features to manage business opening hours,
 * staff working hours, or system-defined time slots. Additional query methods can be
 * defined here if more complex filtering or lookup logic is required.
 * </p>
 */
@Repository
public interface ScheduleHoursRepository extends JpaRepository<ScheduleHours, Long> {
}