package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 根据客户姓名查询订单（原有）
    List<Order> findByCustomerName(String customerName);

    // 查询所有未归档的订单（原有）
    List<Order> findByIsArchivedFalse();

    // 查询所有已归档订单，按创建时间倒序（原有）
    List<Order> findByIsArchivedTrueOrderByCreatedAtDesc();

    // 新增：查询所有已归档订单（不分顺序）—— 如果上面的方法仍然报错，可以加这个备用
    List<Order> findByIsArchivedTrue();
}