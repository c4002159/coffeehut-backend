// ScheduleHoursRepository.java -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.ScheduleHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleHoursRepository extends JpaRepository<ScheduleHours, Long> {
}
