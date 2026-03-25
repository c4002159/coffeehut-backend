package com.coffeehut.coffeehut.two;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import com.coffeehut.coffeehut.repository.OrderItemRepository;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // 🔥 生成5位订单号（00001开始）
    public String generateOrderNumber() {
        long count = orderRepository.count() + 1;
        return String.format("%05d", count);
    }

    // 🔥 创建订单（用于reorder）
    public Order createOrder(Order order, List<OrderItem> items) {

        // 1. 设置订单号
        order.setOrderNumber(generateOrderNumber());

        // 2. 设置时间
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);

        // 取餐时间 = 当前 +10分钟
        if (order.getPickupTime() == null) {
            order.setPickupTime(now.plusMinutes(10));
        }

        // 默认状态
        order.setStatus("pending");

        // 3. 保存订单
        Order savedOrder = orderRepository.save(order);

        // 4. 保存订单项
        for (OrderItem item : items) {
            item.setOrderId(savedOrder.getId());
            orderItemRepository.save(item);
        }

        return savedOrder;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<Order> getOrdersByCustomer(String name) {
        return orderRepository.findByCustomerName(name);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
}