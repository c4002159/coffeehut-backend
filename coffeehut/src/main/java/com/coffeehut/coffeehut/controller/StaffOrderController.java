// StaffOrderController.java — REST endpoints for staff order management -WeiqiWang

package com.coffeehut.coffeehut.controller;

import com.coffeehut.coffeehut.dto.NoteRequest;
import com.coffeehut.coffeehut.dto.OrderDetailDTO;
import com.coffeehut.coffeehut.dto.OrderWithItemsDTO;
import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/orders")
@CrossOrigin(origins = "*") // allow all origins during local development -WeiqiWang
public class StaffOrderController {

    @Autowired
    private OrderService orderService;

    // GET /api/staff/orders/active -WeiqiWang
    @GetMapping("/active")
    public List<OrderWithItemsDTO> getActiveOrders() {
        return orderService.getActiveOrdersWithItems();
    }

    // GET /api/staff/orders/{id} -WeiqiWang
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(@PathVariable Long id) {
        OrderDetailDTO detail = orderService.getOrderDetail(id);
        return detail != null ? ResponseEntity.ok(detail) : ResponseEntity.notFound().build();
    }

    // PATCH /api/staff/orders/{id}/status -WeiqiWang
    // Also handles restore: passing "pending" or "in_progress" clears isArchived.
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Order updated = orderService.updateOrderStatus(id, body.get("status"));
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    // GET /api/staff/orders/archived -WeiqiWang
    @GetMapping("/archived")
    public Map<String, List<Order>> getArchivedOrders() {
        return orderService.getArchivedOrdersGrouped();
    }

    // GET /api/staff/orders/archived/search?keyword=xxx -WeiqiWang
    @GetMapping("/archived/search")
    public List<Order> searchArchivedOrders(@RequestParam String keyword) {
        return orderService.searchArchivedOrders(keyword);
    }

    // POST /api/staff/orders/{id}/cancel -WeiqiWang
    // Returns 400 if the order is not in a cancellable status.
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Order cancelled = orderService.cancelOrder(id);
        return cancelled != null
                ? ResponseEntity.ok(cancelled)
                : ResponseEntity.badRequest().body(Map.of("error", "Order cannot be cancelled"));
    }

    // PATCH /api/staff/orders/{id}/note -WeiqiWang
    @PatchMapping("/{id}/note")
    public ResponseEntity<Order> addNote(@PathVariable Long id, @RequestBody NoteRequest noteRequest) {
        Order updated = orderService.addNote(id, noteRequest.getNote());
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
}
