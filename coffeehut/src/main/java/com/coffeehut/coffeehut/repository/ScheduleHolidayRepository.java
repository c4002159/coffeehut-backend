// ScheduleHolidayRepository.java -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.ScheduleHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ScheduleHoliday} entities.
 * <p>
 * This interface provides data access operations for the schedule_holiday table
 * using Spring Data JPA. By extending {@link JpaRepository}, it inherits standard
 * CRUD operations such as create, read, update, and delete without requiring
 * explicit implementation.
 * <p>
 * It is typically used in scheduling or staff management features to persist
 * and retrieve holiday or unavailable date information.
 * </p>
 */
@Repository
public interface ScheduleHolidayRepository extends JpaRepository<ScheduleHoliday, Long> {
}