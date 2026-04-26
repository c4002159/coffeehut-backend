package com.coffeehut.coffeehut.payment.Controller;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import com.coffeehut.coffeehut.payment.Service.PaymentService;
import com.coffeehut.coffeehut.payment.dto.HorsePayResponse;
import com.coffeehut.coffeehut.payment.dto.PaymentRefundRequest;
import com.coffeehut.coffeehut.repository.OrderItemRepository;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private OrderItemRepository orderItemRepository;

    @Resource
    private PaymentService paymentService;

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

        // Generate order number: CH-YYYYMMDD-001 (increments per day)
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        long todayCount = orderRepository.countOrdersCreatedBetween(dayStart, dayEnd);
        String orderNumber = String.format("CH-%s-%03d", dateStr, todayCount + 1);
        order.setOrderNumber(orderNumber);

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

    // GET /api/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/orders/{id}/items
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable Long id) {
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return ResponseEntity.ok(items);
    }

    // GET /api/orders/customer?name=xxx
    @GetMapping("/customer")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@RequestParam String name) {
        List<Order> orders = orderRepository.findByCustomerName(name);
        return ResponseEntity.ok(orders);
    }

    // POST /api/staff/orders/{id}/cancel
    @PostMapping("/staff/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id) {
        Optional<Order> optional = orderRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Order order = optional.get();
        if (order.getStatus().equals("collected") || order.getStatus().equals("cancelled")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order cannot be cancelled"));
        }
        order.setStatus("cancelled");
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("id", order.getId(), "status", "cancelled"));
    }

    // POST /api/orders/staff/{id}/refund
    @PostMapping("/staff/{id}/refund")
    public ResponseEntity<Map<String, Object>> refundOrder(@PathVariable Long id) {
        Optional<Order> optional = orderRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = optional.get();
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(order.getId());
        request.setCustomerID(order.getCustomerPhone());
        request.setTransactionAmount(order.getTotalPrice());

        HorsePayResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(Map.of(
                "id", order.getId(),
                "status", Optional.ofNullable(orderRepository.findById(id).orElse(order).getStatus()).orElse("unknown"),
                "paymentSuccess", response != null && response.getPaymentSuccess() != null
                        ? response.getPaymentSuccess().getStatus()
                        : null,
                "reason", response != null && response.getPaymentSuccess() != null
                        ? response.getPaymentSuccess().getReason()
                        : null
        ));
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
