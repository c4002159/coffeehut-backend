package com.coffeehut.coffeehut.model;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*") // 必须允许跨域，否则前端无法获取数据

public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    // 根据订单 ID 获取详情 (前端轮询用)
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // 根据客户姓名获取所有订单
    @GetMapping("/customer")
    public List<Order> getOrdersByCustomer(@RequestParam String name) {
        return orderRepository.findByCustomerName(name);
    }
}
