// OrderRepository.java — JPA repository for the orders table -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerName(String customerName);

    List<Order> findByIsArchivedFalse(); // active orders -WeiqiWang

    List<Order> findByIsArchivedTrue();  // archived orders -WeiqiWang

    List<Order> findByIsArchivedTrueOrderByCreatedAtDesc();
}
