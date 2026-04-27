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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Resource private OrderRepository orderRepository;
    @Resource private OrderItemRepository orderItemRepository;
    @Resource private PaymentService paymentService;

    // Parse pickupTime from frontend — handles both "HH:mm" and "HH:mm:ss" variants. -WeiqiWang
    // Frontend sends "2026-04-27T15:30" (no seconds), so we must not rely on
    // LocalDateTime.parse() alone which requires seconds by default.
    private LocalDateTime parsePickupTime(String raw) {
        if (raw == null || raw.isBlank()) return LocalDateTime.now().plusMinutes(15);
        String s = raw.replace("Z", "").trim();
        // Try full ISO with seconds first, then short form without seconds -WeiqiWang
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        }) {
            try { return LocalDateTime.parse(s, fmt); } catch (DateTimeParseException ignored) {}
        }
        // Final fallback -WeiqiWang
        return LocalDateTime.now().plusMinutes(15);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setPickupTime(parsePickupTime(request.getPickupTime())); // safe parse -WeiqiWang
        order.setTotalPrice(request.getTotalPrice());
        order.setStatus("pending");

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            order.setNotes(request.getNotes());
        }

        // Generate order number: CH-YYYYMMDD-NNN (increments per day) -WeiqiWang
        String dateStr    = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd   = dayStart.plusDays(1);
        long todayCount = orderRepository.countOrdersCreatedBetween(dayStart, dayEnd);
        order.setOrderNumber(String.format("CH-%s-%03d", dateStr, todayCount + 1));

        order = orderRepository.save(order);

        // Save order items with itemId and customizations -WeiqiWang
        if (request.getItems() != null) {
            for (OrderItemDto item : request.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setItemId(item.getItemId());
                orderItem.setSize(item.getSize());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setSubtotal(item.getSubtotal());
                if (item.getCustomizations() != null && !item.getCustomizations().isEmpty()) {
                    orderItem.setCustomizations(String.join(",", item.getCustomizations()));
                }
                orderItemRepository.save(orderItem);
            }
        }

        return ResponseEntity.ok(Map.of("id", order.getId(), "orderId", order.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable Long id) {
        return ResponseEntity.ok(orderItemRepository.findByOrderId(id));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@RequestParam String name) {
        return ResponseEntity.ok(orderRepository.findByCustomerName(name));
    }

    @PostMapping("/staff/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id) {
        Optional<Order> optional = orderRepository.findById(id);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();
        Order order = optional.get();
        if ("collected".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order cannot be cancelled"));
        }
        order.setStatus("cancelled");
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("id", order.getId(), "status", "cancelled"));
    }

    @PostMapping("/staff/{id}/refund")
    public ResponseEntity<Map<String, Object>> refundOrder(@PathVariable Long id) {
        Optional<Order> optional = orderRepository.findById(id);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();
        Order order = optional.get();
        PaymentRefundRequest refundReq = new PaymentRefundRequest();
        refundReq.setOrderId(order.getId());
        refundReq.setCustomerID(order.getCustomerPhone());
        refundReq.setTransactionAmount(order.getTotalPrice());
        HorsePayResponse response = paymentService.processRefund(refundReq);
        return ResponseEntity.ok(Map.of(
                "id",            order.getId(),
                "status",        orderRepository.findById(id).map(Order::getStatus).orElse("unknown"),
                "paymentSuccess", response != null && response.getPaymentSuccess() != null ? response.getPaymentSuccess().getStatus() : null,
                "reason",         response != null && response.getPaymentSuccess() != null ? response.getPaymentSuccess().getReason()  : null
        ));
    }

    @lombok.Data
    public static class CreateOrderRequest {
        private String customerName;
        private String customerPhone;
        private String pickupTime;
        private Double totalPrice;
        private String notes;
        private List<OrderItemDto> items;
    }

    @lombok.Data
    public static class OrderItemDto {
        private Long itemId;
        private String size;
        private Integer quantity;
        private Double subtotal;
        private List<String> customizations;
    }
}
