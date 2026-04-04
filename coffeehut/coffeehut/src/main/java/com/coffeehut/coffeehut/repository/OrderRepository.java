package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByIsArchivedFalse();
    List<Order> findByIsArchivedTrue();
    List<Order> findByIsArchivedTrueOrderByCreatedAtDesc();
}