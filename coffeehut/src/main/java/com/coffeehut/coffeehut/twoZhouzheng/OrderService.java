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

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<Order> getOrdersByCustomer(String name) {
        return orderRepository.findByCustomerName(name);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Order createReorder(ReorderRequest request) {
        Order newOrder = new Order();
        newOrder.setCustomerName(request.getCustomerName());
        newOrder.setCustomerPhone(request.getCustomerPhone());
        newOrder.setTotalPrice(request.getTotalPrice());
        newOrder.setStatus("pending");
        newOrder.setIsArchived(false);
        newOrder.setCreatedAt(LocalDateTime.now());

        if (request.getPickupTime() != null) {
            newOrder.setPickupTime(request.getPickupTime());
        } else {
            newOrder.setPickupTime(LocalDateTime.now().plusMinutes(10));
        }

        Order saved = orderRepository.save(newOrder);

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
