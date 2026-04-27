// ScheduleHolidayRepository.java -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.ScheduleHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleHolidayRepository extends JpaRepository<ScheduleHoliday, Long> {
}
