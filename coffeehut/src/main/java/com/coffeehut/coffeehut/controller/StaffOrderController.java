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

/**
 * REST controller for staff-facing order management.
 * <p>
 * All endpoints are under {@code /api/staff/orders/} and are not accessible
 * from the customer-facing interface. Covers the full order lifecycle:
 * viewing active orders, progressing status, cancelling, restoring,
 * archiving, searching, and adding staff notes.
 * </p>
 */
@RestController
@RequestMapping("/api/staff/orders")
@CrossOrigin(origins = "*") // allow all origins during local development -WeiqiWang
public class StaffOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Returns all non-archived orders with their item summaries.
     * <p>
     * Used to populate the staff dashboard. Each item's name is resolved
     * from the {@code items} table so the client does not need extra lookups.
     * </p>
     *
     * @return list of {@link OrderWithItemsDTO} for all active orders
     */
    // GET /api/staff/orders/active -WeiqiWang
    @GetMapping("/active")
    public List<OrderWithItemsDTO> getActiveOrders() {
        return orderService.getActiveOrdersWithItems();
    }

    /**
     * Returns full detail of a single order including resolved item names.
     *
     * @param id the order id
     * @return {@link OrderDetailDTO} if found, or 404 if not
     */
    // GET /api/staff/orders/{id} -WeiqiWang
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> getOrderDetail(@PathVariable Long id) {
        OrderDetailDTO detail = orderService.getOrderDetail(id);
        return detail != null ? ResponseEntity.ok(detail) : ResponseEntity.notFound().build();
    }

    /**
     * Updates the status of an order and writes the corresponding timestamp.
     * <p>
     * Also handles order restore: passing {@code "pending"} or {@code "in_progress"}
     * clears {@code isArchived} and {@code completedAt}, moving the order back
     * to the active dashboard.
     * </p>
     *
     * @param id   the order id
     * @param body map containing {@code "status"} key with the new status value
     * @return the updated {@link Order}, or 404 if not found
     */
    // PATCH /api/staff/orders/{id}/status -WeiqiWang
    // Also handles restore: passing "pending" or "in_progress" clears isArchived.
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Order updated = orderService.updateOrderStatus(id, body.get("status"));
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    /**
     * Returns archived orders grouped into TODAY, YESTERDAY, and LAST_7_DAYS.
     * <p>
     * Grouping is based on {@code completedAt} timestamp, sorted newest first
     * within each group.
     * </p>
     *
     * @return map with keys {@code "TODAY"}, {@code "YESTERDAY"}, {@code "LAST_7_DAYS"}
     */
    // GET /api/staff/orders/archived -WeiqiWang
    @GetMapping("/archived")
    public Map<String, List<Order>> getArchivedOrders() {
        return orderService.getArchivedOrdersGrouped();
    }

    /**
     * Searches archived orders by order number or customer name.
     * <p>
     * Case-insensitive substring match. Results are sorted by {@code completedAt}
     * descending (newest first).
     * </p>
     *
     * @param keyword search term to match against order number or customer name
     * @return matching archived orders, sorted newest first
     */
    // GET /api/staff/orders/archived/search?keyword=xxx -WeiqiWang
    @GetMapping("/archived/search")
    public List<Order> searchArchivedOrders(@RequestParam String keyword) {
        return orderService.searchArchivedOrders(keyword);
    }

    /**
     * Cancels an order, recording the status it held at cancellation time.
     * <p>
     * Only {@code pending} and {@code in_progress} orders can be cancelled.
     * The original status is preserved in {@code cancelledFrom} so the staff
     * portal can offer a Restore option.
     * </p>
     *
     * @param id the order id
     * @return the cancelled {@link Order}, or 400 if the order cannot be cancelled
     */
    // POST /api/staff/orders/{id}/cancel -WeiqiWang
    // Returns 400 if the order is not in a cancellable status.
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        Order cancelled = orderService.cancelOrder(id);
        return cancelled != null
                ? ResponseEntity.ok(cancelled)
                : ResponseEntity.badRequest().body(Map.of("error", "Order cannot be cancelled"));
    }

    /**
     * Adds or updates a staff note on the order.
     * <p>
     * Writes to {@code staffNotes}, which is separate from {@code notes}
     * (customer-submitted at order time). Displayed in the Staff Notes
     * section on the Order Detail page.
     * </p>
     *
     * @param id          the order id
     * @param noteRequest request body containing the note text
     * @return the updated {@link Order}, or 404 if not found
     */
    // PATCH /api/staff/orders/{id}/note -WeiqiWang
    @PatchMapping("/{id}/note")
    public ResponseEntity<Order> addNote(@PathVariable Long id, @RequestBody NoteRequest noteRequest) {
        Order updated = orderService.addNote(id, noteRequest.getNote());
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }
}
