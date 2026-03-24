package com.coffeehut.coffeehut.payment.Controller;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import com.coffeehut.coffeehut.repository.OrderItemRepository;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private OrderItemRepository orderItemRepository;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        LocalDateTime pickup = request.getPickupTime() != null && !request.getPickupTime().isEmpty()
                ? LocalDateTime.parse(request.getPickupTime().replace("Z", ""))
                : LocalDateTime.now().plusMinutes(15);
        order.setPickupTime(pickup);
        order.setTotalPrice(request.getTotalPrice());
        order.setStatus("pending");
        order = orderRepository.save(order);

        for (OrderItemDto item : request.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setItemId(item.getItemId());
            orderItem.setSize(item.getSize());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(item.getSubtotal());
            orderItemRepository.save(orderItem);
        }

        return ResponseEntity.ok(Map.of("id", order.getId(), "orderId", order.getId()));
    }

    @lombok.Data
    public static class CreateOrderRequest {
        private String customerName;
        private String customerPhone;
        private String pickupTime;
        private Double totalPrice;
        private List<OrderItemDto> items;
    }

    @lombok.Data
    public static class OrderItemDto {
        private Long itemId;
        private String size;
        private Integer quantity;
        private Double subtotal;
    }
}
