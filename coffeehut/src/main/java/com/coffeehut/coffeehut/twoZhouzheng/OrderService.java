package com.coffeehut.coffeehut.twoZhouzheng;

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

    // 获取单个订单
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // 按客户名查订单列表
    public List<Order> getOrdersByCustomer(String name) {
        return orderRepository.findByCustomerName(name);
    }

    // 获取订单商品
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    // Reorder — 复制一个历史订单重新下单
    public Order createReorder(ReorderRequest request) {
        // 1. 创建新订单，复制客户信息
        Order newOrder = new Order();
        newOrder.setCustomerName(request.getCustomerName());
        newOrder.setCustomerPhone(request.getCustomerPhone());
        newOrder.setTotalPrice(request.getTotalPrice());
        newOrder.setStatus("pending");
        newOrder.setIsArchived(false);
        newOrder.setCreatedAt(LocalDateTime.now());

        // 2. 取餐时间：用请求里传来的，没有就默认 +10分钟
        if (request.getPickupTime() != null) {
            newOrder.setPickupTime(request.getPickupTime());
        } else {
            newOrder.setPickupTime(LocalDateTime.now().plusMinutes(10));
        }

        // 3. 保存订单
        Order saved = orderRepository.save(newOrder);

        // 4. 保存订单商品
        if (request.getItems() != null) {
            for (OrderItem item : request.getItems()) {
                OrderItem newItem = new OrderItem();
                newItem.setOrderId(saved.getId());
                newItem.setItemId(item.getItemId());
                newItem.setSize(item.getSize());
                newItem.setQuantity(item.getQuantity());
                newItem.setSubtotal(item.getSubtotal());
                orderItemRepository.save(newItem);
            }
        }

        return saved;
    }
}
