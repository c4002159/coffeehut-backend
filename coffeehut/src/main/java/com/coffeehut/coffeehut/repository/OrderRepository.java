package com.coffeehut.coffeehut.repository;
import com.coffeehut.coffeehut.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerName(String customerName);

    List<Order> findByIsArchivedFalse();
    List<Order> findByIsArchivedTrue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    long countOrdersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
