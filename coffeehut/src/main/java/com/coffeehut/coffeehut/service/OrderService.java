// OrderService.java — Business logic for staff order management -WeiqiWang

package com.coffeehut.coffeehut.service;

import com.coffeehut.coffeehut.dto.OrderDetailDTO;
import com.coffeehut.coffeehut.dto.OrderWithItemsDTO;
import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import com.coffeehut.coffeehut.repository.ItemRepository;
import com.coffeehut.coffeehut.repository.OrderItemRepository;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    // --- Shared helpers (called by multiple roles) ---

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<Order> getOrdersByCustomer(String name) {
        return orderRepository.findByCustomerName(name);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    // Creates a new order by copying items from a previous order. -WeiqiWang
    public Order createReorder(ReorderRequest request) {
        Order newOrder = new Order();
        newOrder.setCustomerName(request.getCustomerName());
        newOrder.setCustomerPhone(request.getCustomerPhone());
        newOrder.setTotalPrice(request.getTotalPrice());
        newOrder.setStatus("pending");
        newOrder.setIsArchived(false);
        newOrder.setCreatedAt(LocalDateTime.now());

        // Order number: "A-" + last 4 digits of epoch millis + 3-digit random suffix -WeiqiWang
        String orderNumber = "A-"
                + String.valueOf(System.currentTimeMillis()).substring(9)
                + String.format("%03d", (int)(Math.random() * 1000));
        newOrder.setOrderNumber(orderNumber);

        newOrder.setPickupTime(request.getPickupTime() != null
                ? request.getPickupTime()
                : LocalDateTime.now().plusMinutes(10));

        Order saved = orderRepository.save(newOrder);

        if (request.getItems() != null) {
            for (OrderItem item : request.getItems()) {
                OrderItem newItem = new OrderItem();
                newItem.setOrderId(saved.getId());
                newItem.setItemId(item.getItemId());
                newItem.setSize(item.getSize());
                newItem.setQuantity(item.getQuantity());
                newItem.setSubtotal(item.getSubtotal());
                newItem.setCustomizations(item.getCustomizations());
                orderItemRepository.save(newItem);
            }
        }
        return saved;
    }

    // --- Staff-side methods ---

    // Returns all non-archived orders with their item lists. -WeiqiWang
    public List<OrderWithItemsDTO> getActiveOrdersWithItems() {
        List<Order> activeOrders = orderRepository.findByIsArchivedFalse();
        return activeOrders.stream().map(order -> {
            OrderWithItemsDTO dto = new OrderWithItemsDTO();
            dto.setOrder(order);
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<OrderWithItemsDTO.OrderItemSummary> summaries = items.stream().map(item -> {
                OrderWithItemsDTO.OrderItemSummary summary = new OrderWithItemsDTO.OrderItemSummary();
                String itemName = itemRepository.findById(item.getItemId())
                        .map(Item::getName).orElse("Unknown");
                summary.setName(itemName);
                summary.setQuantity(item.getQuantity());
                summary.setSize(item.getSize());
                summary.setCustomizations(item.getCustomizations());
                return summary;
            }).collect(Collectors.toList());
            dto.setItems(summaries);
            return dto;
        }).collect(Collectors.toList());
    }

    // Updates order status and writes stage timestamps. -WeiqiWang
    // Also handles restore: when status is "pending" or "in_progress", clears
    // isArchived and completedAt so the order moves back to the active list.
    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return null;

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(status);

        switch (status) {
            case "in_progress":
                order.setAcceptedAt(now);
                // Restore path: clear archive flags set during cancellation -WeiqiWang
                order.setIsArchived(false);
                order.setCompletedAt(null);
                order.setCancelledFrom(null);
                break;
            case "pending":
                // Restore path: clear archive flags set during cancellation -WeiqiWang
                order.setIsArchived(false);
                order.setCompletedAt(null);
                order.setCancelledFrom(null);
                break;
            case "ready":
                order.setReadyAt(now);
                break;
            case "collected":
                order.setIsArchived(true);
                order.setCompletedAt(now);
                break;
            case "cancelled":
                order.setIsArchived(true);
                order.setCompletedAt(now);
                break;
            default:
                break;
        }

        return orderRepository.save(order);
    }

    // Returns full detail of a single order with resolved item names. -WeiqiWang
    public OrderDetailDTO getOrderDetail(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return null;

        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        List<OrderDetailDTO.OrderItemWithName> itemsWithName = items.stream().map(item -> {
            OrderDetailDTO.OrderItemWithName dtoItem = new OrderDetailDTO.OrderItemWithName();
            dtoItem.setId(item.getId());
            dtoItem.setOrderId(item.getOrderId());
            dtoItem.setItemId(item.getItemId());
            dtoItem.setSize(item.getSize());
            dtoItem.setQuantity(item.getQuantity());
            dtoItem.setSubtotal(item.getSubtotal());
            dtoItem.setCustomizations(item.getCustomizations());
            String itemName = itemRepository.findById(item.getItemId())
                    .map(Item::getName).orElse("Unknown");
            dtoItem.setName(itemName);
            return dtoItem;
        }).collect(Collectors.toList());

        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrder(order);
        dto.setItems(itemsWithName);
        return dto;
    }

    // Returns archived orders grouped by TODAY / YESTERDAY / LAST_7_DAYS, newest first. -WeiqiWang
    public Map<String, List<Order>> getArchivedOrdersGrouped() {
        List<Order> archived = orderRepository.findByIsArchivedTrue();
        LocalDateTime now            = LocalDateTime.now();
        LocalDateTime todayStart     = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime weekAgo        = todayStart.minusDays(7);

        Map<String, List<Order>> result = new LinkedHashMap<>();
        result.put("TODAY", archived.stream()
                .filter(o -> o.getCompletedAt() != null && o.getCompletedAt().isAfter(todayStart))
                .sorted(Comparator.comparing(Order::getCompletedAt).reversed())
                .collect(Collectors.toList()));
        result.put("YESTERDAY", archived.stream()
                .filter(o -> o.getCompletedAt() != null
                        && o.getCompletedAt().isBefore(todayStart)
                        && o.getCompletedAt().isAfter(yesterdayStart))
                .sorted(Comparator.comparing(Order::getCompletedAt).reversed())
                .collect(Collectors.toList()));
        result.put("LAST_7_DAYS", archived.stream()
                .filter(o -> o.getCompletedAt() != null
                        && o.getCompletedAt().isBefore(yesterdayStart)
                        && o.getCompletedAt().isAfter(weekAgo))
                .sorted(Comparator.comparing(Order::getCompletedAt).reversed())
                .collect(Collectors.toList()));
        return result;
    }

    // Searches archived orders by order number or customer name, newest first. -WeiqiWang
    public List<Order> searchArchivedOrders(String keyword) {
        return orderRepository.findByIsArchivedTrue().stream()
                .filter(o -> o.getOrderNumber() != null && o.getCustomerName() != null
                        && (o.getOrderNumber().toLowerCase().contains(keyword.toLowerCase())
                            || o.getCustomerName().toLowerCase().contains(keyword.toLowerCase())))
                .sorted(Comparator.comparing(
                        o -> o.getCompletedAt() != null ? o.getCompletedAt() : LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    // Cancels an order. Only allowed for pending / in_progress. -WeiqiWang
    // Records cancelledFrom so the staff portal can offer a Restore option.
    // Returns null if cancellation is not permitted for the current status.
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null
                && ("pending".equals(order.getStatus())
                    || "in_progress".equals(order.getStatus()))) {
            order.setCancelledFrom(order.getStatus()); // preserve original status for Restore -WeiqiWang
            order.setStatus("cancelled");
            order.setIsArchived(true);
            order.setCompletedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
        return null;
    }

    // Adds or updates a staff note on an order. -WeiqiWang
    public Order addNote(Long id, String note) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setNotes(note);
            return orderRepository.save(order);
        }
        return null;
    }
}
