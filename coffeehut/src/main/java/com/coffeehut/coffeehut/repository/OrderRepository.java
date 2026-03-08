package com.coffeehut.coffeehut.repository;
import com.coffeehut.coffeehut.model.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 根据姓名查询订单

    List<Order> findByCustomerName(String customerName);

    // 查询未归档订单
    List<Order> findByIsArchivedFalse();
    List<Order> findByIsArchivedTrue();
}
