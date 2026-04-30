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

/**
 * Service layer containing business logic for order management.
 * <p>
 * Serves both the customer-facing reorder flow and the staff-side order
 * dashboard. Provides methods for creating reorders, querying active and
 * archived orders, updating order status with stage timestamps, cancelling
 * orders, and adding staff notes. All persistence is delegated to
 * {@link OrderRepository}, {@link OrderItemRepository}, and
 * {@link ItemRepository}.
 * </p>
 */
@Service
public class OrderService {

    /** Repository for {@link Order} CRUD and custom queries. */
    @Autowired
    private OrderRepository orderRepository;

    /** Repository for {@link OrderItem} lookups by order ID. */
    @Autowired
    private OrderItemRepository orderItemRepository;

    /** Repository used to resolve item names from item IDs. */
    @Autowired
    private ItemRepository itemRepository;

    // ── Shared helpers (called by multiple roles) ────────────────────────────

    /**
     * Retrieves a single order by its primary key.
     *
     * @param id the primary key of the {@link Order} to retrieve
     * @return the matching {@link Order}, or {@code null} if no order
     *         exists for the given {@code id}
     */
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    /**
     * Retrieves all orders placed by a given customer name.
     *
     * @param name the customer name to search for
     * @return a list of {@link Order} objects matching the customer name;
     *         never {@code null} but may be empty
     */
    public List<Order> getOrdersByCustomer(String name) {
        return orderRepository.findByCustomerName(name);
    }

    /**
     * Retrieves all order items belonging to a given order.
     *
     * @param orderId the primary key of the parent order
     * @return a list of {@link OrderItem} objects for the given order;
     *         never {@code null} but may be empty
     */
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * Creates a new order by copying items from a previous order.
     * <p>
     * Sets the new order status to {@code "pending"} and generates a
     * unique order number in the format {@code A-XXXXYYY} where
     * {@code XXXX} is the last four digits of the current epoch millisecond
     * and {@code YYY} is a three-digit random suffix. If the request does
     * not specify a pickup time, the service defaults to 10 minutes from
     * the current time.
     * </p>
     *
     * @param request the reorder payload containing customer details,
     *                optional pickup time, total price, and item list
     * @return the persisted {@link Order} representing the new reorder
     */
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

    // ── Staff-side methods ───────────────────────────────────────────────────

    /**
     * Returns all non-archived orders together with their item summaries.
     * <p>
     * For each active order, the item list is fetched from
     * {@link OrderItemRepository} and each item's name is resolved from
     * {@link ItemRepository}. A {@code null} {@code itemId} is handled
     * gracefully by defaulting the name to {@code "Unknown"}.
     * </p>
     *
     * @return a list of {@link OrderWithItemsDTO} objects representing all
     *         active (non-archived) orders; never {@code null} but may be empty
     */
    public List<OrderWithItemsDTO> getActiveOrdersWithItems() {
        List<Order> activeOrders = orderRepository.findByIsArchivedFalse();
        return activeOrders.stream().map(order -> {
            OrderWithItemsDTO dto = new OrderWithItemsDTO();
            dto.setOrder(order);
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<OrderWithItemsDTO.OrderItemSummary> summaries = items.stream().map(item -> {
                OrderWithItemsDTO.OrderItemSummary summary = new OrderWithItemsDTO.OrderItemSummary();
                String itemName = item.getItemId() != null
                        ? itemRepository.findById(item.getItemId()).map(Item::getName).orElse("Unknown")
                        : "Unknown"; // guard against null itemId -WeiqiWang
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

    /**
     * Updates an order's status and writes the appropriate stage timestamp.
     * <p>
     * Timestamp behaviour per status transition:
     * <ul>
     *   <li>{@code in_progress} — sets {@code acceptedAt}; also clears
     *       {@code isArchived}, {@code completedAt}, and {@code cancelledFrom}
     *       for the restore path.</li>
     *   <li>{@code pending} — clears {@code isArchived}, {@code completedAt},
     *       and {@code cancelledFrom} for the restore path.</li>
     *   <li>{@code ready} — sets {@code readyAt}.</li>
     *   <li>{@code collected} — sets {@code isArchived = true} and
     *       {@code completedAt}.</li>
     *   <li>{@code cancelled} — sets {@code isArchived = true} and
     *       {@code completedAt}.</li>
     * </ul>
     * </p>
     *
     * @param id     the primary key of the order to update
     * @param status the new status value to apply
     * @return the updated and persisted {@link Order}, or {@code null} if
     *         no order exists for the given {@code id}
     */
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

    /**
     * Returns the full detail of a single order with resolved item names.
     * <p>
     * Fetches the order and its associated {@link OrderItem} list, then
     * resolves each item's display name from {@link ItemRepository}.
     * A {@code null} {@code itemId} is handled gracefully by defaulting
     * the name to {@code "Unknown"}.
     * </p>
     *
     * @param id the primary key of the order to retrieve
     * @return an {@link OrderDetailDTO} containing the order and its items,
     *         or {@code null} if no order exists for the given {@code id}
     */
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
            String itemName = item.getItemId() != null
                    ? itemRepository.findById(item.getItemId()).map(Item::getName).orElse("Unknown")
                    : "Unknown"; // guard against null itemId -WeiqiWang
            dtoItem.setName(itemName);
            return dtoItem;
        }).collect(Collectors.toList());

        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrder(order);
        dto.setItems(itemsWithName);
        return dto;
    }

    /**
     * Returns all archived orders grouped by completion time into three
     * buckets: {@code TODAY}, {@code YESTERDAY}, and {@code LAST_7_DAYS}.
     * <p>
     * Grouping is based on {@code completedAt}, not {@code createdAt},
     * so an order placed yesterday but collected today appears in
     * {@code TODAY}. Orders with a {@code null} {@code completedAt} are
     * excluded from all groups. Within each group, orders are sorted
     * newest-first by {@code completedAt}.
     * </p>
     *
     * @return a {@link LinkedHashMap} with keys {@code "TODAY"},
     *         {@code "YESTERDAY"}, and {@code "LAST_7_DAYS"}, each mapping
     *         to a (possibly empty) list of archived {@link Order} objects
     */
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

    /**
     * Searches archived orders by order number or customer name.
     * <p>
     * The search is case-insensitive and uses substring matching against
     * both {@code orderNumber} and {@code customerName}. Results are
     * sorted newest-first by {@code completedAt}; orders with a
     * {@code null} {@code completedAt} are placed at the end.
     * </p>
     *
     * @param keyword the search term to match against order number or
     *                customer name; an empty string matches all orders
     * @return a list of matching archived {@link Order} objects sorted
     *         by {@code completedAt} descending; never {@code null} but
     *         may be empty
     */
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

    /**
     * Cancels an order. Only allowed for {@code pending} and
     * {@code in_progress} statuses.
     * <p>
     * Records the original status in {@code cancelledFrom} so the staff
     * portal can offer a Restore option. Sets {@code isArchived = true}
     * and writes {@code completedAt} to the current time. Returns
     * {@code null} without saving if the order does not exist or its
     * current status is not cancellable.
     * </p>
     *
     * @param id the primary key of the order to cancel
     * @return the cancelled and persisted {@link Order}, or {@code null}
     *         if the order does not exist or cannot be cancelled in its
     *         current status
     */
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

    /**
     * Adds or replaces the staff note on an order.
     * <p>
     * Writes to the {@code staffNotes} field, which is separate from
     * the {@code notes} field submitted by the customer at order time.
     * The two fields must never overwrite each other.
     * </p>
     *
     * @param id   the primary key of the order to annotate
     * @param note the staff note text to store; may be an empty string
     *             to clear the existing note
     * @return the updated and persisted {@link Order}, or {@code null}
     *         if no order exists for the given {@code id}
     */
    public Order addNote(Long id, String note) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setStaffNotes(note);
            return orderRepository.save(order);
        }
        return null;
    }
}