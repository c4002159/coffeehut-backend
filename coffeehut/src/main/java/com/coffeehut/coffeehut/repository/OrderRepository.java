package com.coffeehut.coffeehut.repository;
import com.coffeehut.coffeehut.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    List<Order> findByCustomerName(String customerName);


    List<Order> findByIsArchivedFalse();
    List<Order> findByIsArchivedTrue();
}
