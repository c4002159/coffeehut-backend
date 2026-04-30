package com.coffeehut.coffeehut.service;

import com.coffeehut.coffeehut.dto.OrderDetailDTO;
import com.coffeehut.coffeehut.dto.OrderWithItemsDTO;
import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import com.coffeehut.coffeehut.repository.ItemRepository;
import com.coffeehut.coffeehut.repository.OrderItemRepository;
import com.coffeehut.coffeehut.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderService}.
 * <p>
 * Covers requirements: FR7, FR12, FR13, FR15, FR16, FR23.
 * All dependencies are mocked via Mockito — no real database is used.
 * Each method is tested under normal, error, and boundary conditions.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private OrderService orderService;

    private Order pendingOrder;
    private Order inProgressOrder;
    private Order collectedOrder;

    /**
     * Initialises shared test fixtures before each test method.
     * <p>
     * Creates three representative {@link Order} objects covering the most
     * commonly tested statuses: {@code pending}, {@code in_progress},
     * and {@code collected}.
     * </p>
     */
    @BeforeEach
    void setUp() {
        pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setCustomerName("Alice");
        pendingOrder.setStatus("pending");
        pendingOrder.setIsArchived(false);
        pendingOrder.setOrderNumber("CH-20260430-001");

        inProgressOrder = new Order();
        inProgressOrder.setId(2L);
        inProgressOrder.setCustomerName("Bob");
        inProgressOrder.setStatus("in_progress");
        inProgressOrder.setIsArchived(false);
        inProgressOrder.setOrderNumber("CH-20260430-002");

        collectedOrder = new Order();
        collectedOrder.setId(3L);
        collectedOrder.setCustomerName("Charlie");
        collectedOrder.setStatus("collected");
        collectedOrder.setIsArchived(true);
        collectedOrder.setOrderNumber("CH-20260430-003");
        collectedOrder.setCompletedAt(LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════
    // updateOrderStatus — FR7, FR13
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that transitioning a {@code pending} order to
     * {@code in_progress} sets {@code acceptedAt} and clears archive flags.
     * <p>
     * When staff accept an order, {@code acceptedAt} must be recorded,
     * {@code isArchived} must remain {@code false}, and both
     * {@code completedAt} and {@code cancelledFrom} must be {@code null}.
     * </p>
     *
     * @throws AssertionError if any timestamp or archive flag is incorrect
     * Covers FR13
     */
    @Test
    void updateOrderStatus_pendingToInProgress_setsAcceptedAtAndClearsArchive() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, "in_progress");

        assertEquals("in_progress", result.getStatus());
        assertNotNull(result.getAcceptedAt());
        assertFalse(result.getIsArchived());
        assertNull(result.getCompletedAt());
        assertNull(result.getCancelledFrom());
    }

    /**
     * Verifies that transitioning an order to {@code ready} records
     * the {@code readyAt} timestamp.
     * <p>
     * The {@code readyAt} field must be non-{@code null} after the
     * status update so the staff dashboard can display preparation time.
     * </p>
     *
     * @throws AssertionError if {@code readyAt} is {@code null} or the
     *                        status value is incorrect
     * Covers FR13
     */
    @Test
    void updateOrderStatus_toReady_setsReadyAt() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateOrderStatus(2L, "ready");

        assertEquals("ready", result.getStatus());
        assertNotNull(result.getReadyAt());
    }

    /**
     * Verifies that transitioning an order to {@code collected} archives
     * it and records {@code completedAt}.
     * <p>
     * A collected order must be moved to the archive immediately so it
     * no longer appears on the active dashboard.
     * </p>
     *
     * @throws AssertionError if {@code isArchived} is not {@code true} or
     *                        {@code completedAt} is {@code null}
     * Covers FR13, FR16
     */
    @Test
    void updateOrderStatus_toCollected_archivesAndSetsCompletedAt() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateOrderStatus(2L, "collected");

        assertEquals("collected", result.getStatus());
        assertTrue(result.getIsArchived());
        assertNotNull(result.getCompletedAt());
    }

    /**
     * Verifies that transitioning an order to {@code cancelled} archives
     * it and records {@code completedAt}.
     * <p>
     * Cancellation via {@code updateOrderStatus} must behave identically
     * to collection with respect to archiving.
     * </p>
     *
     * @throws AssertionError if {@code isArchived} is not {@code true} or
     *                        {@code completedAt} is {@code null}
     * Covers FR13
     */
    @Test
    void updateOrderStatus_toCancelled_archivesAndSetsCompletedAt() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, "cancelled");

        assertEquals("cancelled", result.getStatus());
        assertTrue(result.getIsArchived());
        assertNotNull(result.getCompletedAt());
    }

    /**
     * Verifies that restoring a cancelled order to {@code pending} clears
     * the archive flags and completion timestamp.
     * <p>
     * The restore path must set {@code isArchived} back to {@code false}
     * and clear both {@code completedAt} and {@code cancelledFrom} so the
     * order reappears on the active dashboard.
     * </p>
     *
     * @throws AssertionError if any archive flag or timestamp is not cleared
     * Covers FR13
     */
    @Test
    void updateOrderStatus_restoreToPending_clearsArchiveAndCompletedAt() {
        Order cancelledOrder = new Order();
        cancelledOrder.setId(5L);
        cancelledOrder.setStatus("cancelled");
        cancelledOrder.setIsArchived(true);
        cancelledOrder.setCompletedAt(LocalDateTime.now());
        cancelledOrder.setCancelledFrom("pending");

        when(orderRepository.findById(5L)).thenReturn(Optional.of(cancelledOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateOrderStatus(5L, "pending");

        assertEquals("pending", result.getStatus());
        assertFalse(result.getIsArchived());
        assertNull(result.getCompletedAt());
        assertNull(result.getCancelledFrom());
    }

    /**
     * Verifies that {@code updateOrderStatus} returns {@code null} and
     * does not attempt to save when the order ID does not exist.
     * <p>
     * No persistence call must be made for a non-existent order to
     * avoid creating phantom records.
     * </p>
     *
     * @throws AssertionError if the return value is not {@code null} or
     *                        {@code save} is called
     * Covers FR13
     */
    @Test
    void updateOrderStatus_orderNotFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Order result = orderService.updateOrderStatus(99L, "in_progress");

        assertNull(result);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Verifies that an unrecognised status value is persisted without
     * writing any stage timestamps.
     * <p>
     * The service must not throw for unknown status strings; it should
     * fall through the {@code switch} and save the order as-is.
     * </p>
     *
     * @throws AssertionError if any timestamp is non-{@code null} or the
     *                        status is not stored verbatim
     * Covers FR13
     */
    @Test
    void updateOrderStatus_unknownStatus_doesNotSetTimestamps() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateOrderStatus(1L, "unknown_status");

        assertEquals("unknown_status", result.getStatus());
        assertNull(result.getAcceptedAt());
        assertNull(result.getReadyAt());
        assertNull(result.getCompletedAt());
    }

    // ══════════════════════════════════════════════════════
    // cancelOrder — FR13
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a {@code pending} order is cancelled successfully.
     * <p>
     * After cancellation the order must have status {@code cancelled},
     * {@code isArchived} set to {@code true}, a non-{@code null}
     * {@code completedAt}, and {@code cancelledFrom} set to
     * {@code "pending"} so the staff portal can offer a Restore option.
     * </p>
     *
     * @throws AssertionError if any field does not match the expected value
     * Covers FR13
     */
    @Test
    void cancelOrder_pendingOrder_cancelledSuccessfully() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.cancelOrder(1L);

        assertNotNull(result);
        assertEquals("cancelled", result.getStatus());
        assertTrue(result.getIsArchived());
        assertNotNull(result.getCompletedAt());
        assertEquals("pending", result.getCancelledFrom());
    }

    /**
     * Verifies that an {@code in_progress} order is cancelled successfully.
     * <p>
     * {@code cancelledFrom} must record {@code "in_progress"} so the
     * restore flow knows which status to revert to.
     * </p>
     *
     * @throws AssertionError if the status or {@code cancelledFrom} is incorrect
     * Covers FR13
     */
    @Test
    void cancelOrder_inProgressOrder_cancelledSuccessfully() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(inProgressOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.cancelOrder(2L);

        assertNotNull(result);
        assertEquals("cancelled", result.getStatus());
        assertEquals("in_progress", result.getCancelledFrom());
    }

    /**
     * Verifies that a {@code collected} order cannot be cancelled.
     * <p>
     * Once an order has been collected it is complete and must not be
     * modifiable via the cancel endpoint.
     * </p>
     *
     * @throws AssertionError if the return value is not {@code null} or
     *                        {@code save} is called
     * Covers FR13
     */
    @Test
    void cancelOrder_alreadyCollected_returnsNull() {
        when(orderRepository.findById(3L)).thenReturn(Optional.of(collectedOrder));

        Order result = orderService.cancelOrder(3L);

        assertNull(result);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Verifies that an already-cancelled order cannot be cancelled again.
     * <p>
     * Attempting to cancel a {@code cancelled} order must be a no-op
     * to prevent double-archiving.
     * </p>
     *
     * @throws AssertionError if the return value is not {@code null} or
     *                        {@code save} is called
     * Covers FR13
     */
    @Test
    void cancelOrder_alreadyCancelled_returnsNull() {
        Order cancelled = new Order();
        cancelled.setId(4L);
        cancelled.setStatus("cancelled");
        when(orderRepository.findById(4L)).thenReturn(Optional.of(cancelled));

        Order result = orderService.cancelOrder(4L);

        assertNull(result);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Verifies that {@code cancelOrder} returns {@code null} safely when
     * the order ID does not exist.
     *
     * @throws AssertionError if the return value is not {@code null}
     * Covers FR13
     */
    @Test
    void cancelOrder_orderNotFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Order result = orderService.cancelOrder(99L);

        assertNull(result);
    }

    // ══════════════════════════════════════════════════════
    // addNote — FR13
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a staff note is saved to the {@code staffNotes} field.
     * <p>
     * The note must be persisted on the order without altering any other
     * field.
     * </p>
     *
     * @throws AssertionError if {@code staffNotes} does not match the
     *                        supplied value
     * Covers FR13
     */
    @Test
    void addNote_existingOrder_savesStaffNote() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.addNote(1L, "Please add extra sugar");

        assertNotNull(result);
        assertEquals("Please add extra sugar", result.getStaffNotes());
    }

    /**
     * Verifies that adding a staff note does not overwrite the customer's
     * original order notes.
     * <p>
     * {@code notes} (customer-submitted) and {@code staffNotes} (added by
     * staff) are separate fields and must never interfere with each other.
     * </p>
     *
     * @throws AssertionError if {@code notes} is modified or {@code staffNotes}
     *                        does not contain the new value
     * Covers FR13
     */
    @Test
    void addNote_doesNotOverwriteCustomerNotes() {
        pendingOrder.setNotes("Customer note");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.addNote(1L, "Staff note");

        assertEquals("Customer note", result.getNotes());
        assertEquals("Staff note", result.getStaffNotes());
    }

    /**
     * Verifies that {@code addNote} returns {@code null} and does not save
     * when the order ID does not exist.
     *
     * @throws AssertionError if the return value is not {@code null} or
     *                        {@code save} is called
     * Covers FR13
     */
    @Test
    void addNote_orderNotFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Order result = orderService.addNote(99L, "Note");

        assertNull(result);
        verify(orderRepository, never()).save(any());
    }

    /**
     * Verifies that an empty string is accepted as a valid staff note value.
     * <p>
     * Clearing a note by submitting an empty string is a legitimate action
     * and must be persisted without error.
     * </p>
     *
     * @throws AssertionError if {@code staffNotes} is not an empty string
     * Covers FR13
     */
    @Test
    void addNote_emptyNote_savesEmptyString() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.addNote(1L, "");

        assertEquals("", result.getStaffNotes());
    }

    // ══════════════════════════════════════════════════════
    // getActiveOrdersWithItems — FR12
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that {@code getActiveOrdersWithItems} returns all
     * non-archived orders together with their item summaries.
     * <p>
     * Each returned DTO must carry the original {@link Order} and a
     * (possibly empty) list of {@link OrderWithItemsDTO.OrderItemSummary}
     * objects.
     * </p>
     *
     * @throws AssertionError if the result size does not match the number
     *                        of active orders returned by the repository
     * Covers FR12
     */
    @Test
    void getActiveOrdersWithItems_returnsNonArchivedOrders() {
        when(orderRepository.findByIsArchivedFalse())
                .thenReturn(List.of(pendingOrder, inProgressOrder));
        when(orderItemRepository.findByOrderId(any())).thenReturn(List.of());

        var result = orderService.getActiveOrdersWithItems();

        assertEquals(2, result.size());
        verify(orderRepository).findByIsArchivedFalse();
    }

    /**
     * Verifies that archived orders are excluded from the active order list.
     * <p>
     * The repository query is scoped to {@code isArchived = false}, so only
     * orders still being processed should appear on the dashboard.
     * </p>
     *
     * @throws AssertionError if an archived order appears in the result
     * Covers FR12
     */
    @Test
    void getActiveOrdersWithItems_excludesArchivedOrders() {
        when(orderRepository.findByIsArchivedFalse()).thenReturn(List.of(pendingOrder));
        when(orderItemRepository.findByOrderId(any())).thenReturn(List.of());

        var result = orderService.getActiveOrdersWithItems();

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getOrder().getCustomerName());
    }

    /**
     * Verifies that an empty list is returned when there are no active orders.
     *
     * @throws AssertionError if the result is not empty
     * Covers FR12
     */
    @Test
    void getActiveOrdersWithItems_noActiveOrders_returnsEmptyList() {
        when(orderRepository.findByIsArchivedFalse()).thenReturn(List.of());

        var result = orderService.getActiveOrdersWithItems();

        assertTrue(result.isEmpty());
    }

    /**
     * Verifies that item names are resolved from the items table via
     * {@link ItemRepository}.
     * <p>
     * Each order item's name must be looked up by {@code itemId} and
     * stored in the summary DTO so the dashboard can display it without
     * a second request.
     * </p>
     *
     * @throws AssertionError if the resolved item name does not match
     *                        the value stored in the items table
     * Covers FR12
     */
    @Test
    void getActiveOrdersWithItems_resolvesItemName() {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(10L);
        orderItem.setSize("Regular");
        orderItem.setQuantity(1);

        Item menuItem = new Item();
        menuItem.setId(10L);
        menuItem.setName("Latte");

        when(orderRepository.findByIsArchivedFalse()).thenReturn(List.of(pendingOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

        var result = orderService.getActiveOrdersWithItems();

        assertEquals("Latte", result.get(0).getItems().get(0).getName());
    }

    /**
     * Verifies that a {@code null} {@code itemId} on an order item is handled
     * gracefully by defaulting the name to {@code "Unknown"}.
     * <p>
     * This guards against {@link NullPointerException} when an order item
     * was created without a valid menu item reference.
     * </p>
     *
     * @throws AssertionError if the item name is not {@code "Unknown"}
     * Covers FR12
     */
    @Test
    void getActiveOrdersWithItems_nullItemId_returnsUnknown() {
        OrderItem orderItem = new OrderItem();
        orderItem.setItemId(null);

        when(orderRepository.findByIsArchivedFalse()).thenReturn(List.of(pendingOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));

        var result = orderService.getActiveOrdersWithItems();

        assertEquals("Unknown", result.get(0).getItems().get(0).getName());
    }

    // ══════════════════════════════════════════════════════
    // getOrderDetail — FR15
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that {@code getOrderDetail} returns a DTO containing the
     * order and its items with resolved names.
     * <p>
     * The returned {@link OrderDetailDTO} must include the {@link Order}
     * object and a list of {@link OrderDetailDTO.OrderItemWithName} entries
     * where each item's name is looked up from the items table.
     * </p>
     *
     * @throws AssertionError if the DTO is {@code null}, the order does not
     *                        match, or any item name is incorrect
     * Covers FR15
     */
    @Test
    void getOrderDetail_existingOrder_returnsDetailWithItems() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrderId(1L);
        orderItem.setItemId(10L);
        orderItem.setSize("Large");
        orderItem.setQuantity(2);
        orderItem.setSubtotal(5.00);

        Item menuItem = new Item();
        menuItem.setId(10L);
        menuItem.setName("Cappuccino");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(menuItem));

        OrderDetailDTO result = orderService.getOrderDetail(1L);

        assertNotNull(result);
        assertEquals(pendingOrder, result.getOrder());
        assertEquals(1, result.getItems().size());
        assertEquals("Cappuccino", result.getItems().get(0).getName());
        assertEquals("Large", result.getItems().get(0).getSize());
    }

    /**
     * Verifies that {@code getOrderDetail} returns {@code null} when the
     * order ID does not exist.
     *
     * @throws AssertionError if the return value is not {@code null}
     * Covers FR15
     */
    @Test
    void getOrderDetail_orderNotFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        OrderDetailDTO result = orderService.getOrderDetail(99L);

        assertNull(result);
    }

    /**
     * Verifies that a {@code null} {@code itemId} in an order item is handled
     * gracefully within {@code getOrderDetail}, defaulting the name to
     * {@code "Unknown"}.
     *
     * @throws AssertionError if the item name is not {@code "Unknown"}
     * Covers FR15
     */
    @Test
    void getOrderDetail_nullItemId_setsNameToUnknown() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrderId(1L);
        orderItem.setItemId(null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));

        OrderDetailDTO result = orderService.getOrderDetail(1L);

        assertNotNull(result);
        assertEquals("Unknown", result.getItems().get(0).getName());
    }

    // ══════════════════════════════════════════════════════
    // getArchivedOrdersGrouped — FR16, FR23
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that an order completed today appears in the {@code TODAY} group.
     *
     * @throws AssertionError if the order is not in {@code TODAY} or appears
     *                        in another group
     * Covers FR16, FR23
     */
    @Test
    void getArchivedOrdersGrouped_orderCompletedToday_appearsInToday() {
        Order todayOrder = new Order();
        todayOrder.setId(10L);
        todayOrder.setStatus("collected");
        todayOrder.setIsArchived(true);
        todayOrder.setCompletedAt(LocalDateTime.now().withHour(10));

        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(todayOrder));

        Map<String, List<Order>> result = orderService.getArchivedOrdersGrouped();

        assertEquals(1, result.get("TODAY").size());
        assertTrue(result.get("YESTERDAY").isEmpty());
        assertTrue(result.get("LAST_7_DAYS").isEmpty());
    }

    /**
     * Verifies that an order completed yesterday appears in the
     * {@code YESTERDAY} group.
     *
     * @throws AssertionError if the order is not in {@code YESTERDAY}
     * Covers FR16, FR23
     */
    @Test
    void getArchivedOrdersGrouped_orderCompletedYesterday_appearsInYesterday() {
        Order yesterdayOrder = new Order();
        yesterdayOrder.setId(11L);
        yesterdayOrder.setStatus("collected");
        yesterdayOrder.setIsArchived(true);
        yesterdayOrder.setCompletedAt(LocalDateTime.now().minusDays(1).withHour(10));

        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(yesterdayOrder));

        Map<String, List<Order>> result = orderService.getArchivedOrdersGrouped();

        assertTrue(result.get("TODAY").isEmpty());
        assertEquals(1, result.get("YESTERDAY").size());
        assertTrue(result.get("LAST_7_DAYS").isEmpty());
    }

    /**
     * Verifies that an order created yesterday but collected today is
     * classified under {@code TODAY}, confirming that grouping uses
     * {@code completedAt} rather than {@code createdAt}.
     * <p>
     * This is the key test for FR23: classification must be based on
     * completion time, not placement time.
     * </p>
     *
     * @throws AssertionError if the order appears in {@code YESTERDAY}
     *                        instead of {@code TODAY}
     * Covers FR23
     */
    @Test
    void getArchivedOrdersGrouped_createdYesterdayCollectedToday_appearsInToday() {
        Order order = new Order();
        order.setId(12L);
        order.setStatus("collected");
        order.setIsArchived(true);
        order.setCreatedAt(LocalDateTime.now().minusDays(1));
        order.setCompletedAt(LocalDateTime.now().withHour(9));

        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(order));

        Map<String, List<Order>> result = orderService.getArchivedOrdersGrouped();

        assertEquals(1, result.get("TODAY").size(),
                "Grouping must use completedAt, not createdAt");
        assertTrue(result.get("YESTERDAY").isEmpty());
    }

    /**
     * Verifies that an archived order with a {@code null} {@code completedAt}
     * does not appear in any group.
     * <p>
     * Without a completion timestamp the order cannot be classified, so it
     * must be silently excluded from all three groups.
     * </p>
     *
     * @throws AssertionError if the order appears in any group
     * Covers FR16, FR23
     */
    @Test
    void getArchivedOrdersGrouped_nullCompletedAt_notIncludedInAnyGroup() {
        Order order = new Order();
        order.setId(13L);
        order.setStatus("cancelled");
        order.setIsArchived(true);
        order.setCompletedAt(null);

        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(order));

        Map<String, List<Order>> result = orderService.getArchivedOrdersGrouped();

        assertTrue(result.get("TODAY").isEmpty());
        assertTrue(result.get("YESTERDAY").isEmpty());
        assertTrue(result.get("LAST_7_DAYS").isEmpty());
    }

    /**
     * Verifies that all three groups are empty when there are no archived orders.
     *
     * @throws AssertionError if any group is non-empty
     * Covers FR16
     */
    @Test
    void getArchivedOrdersGrouped_noArchivedOrders_allGroupsEmpty() {
        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of());

        Map<String, List<Order>> result = orderService.getArchivedOrdersGrouped();

        assertTrue(result.get("TODAY").isEmpty());
        assertTrue(result.get("YESTERDAY").isEmpty());
        assertTrue(result.get("LAST_7_DAYS").isEmpty());
    }

    // ══════════════════════════════════════════════════════
    // searchArchivedOrders — FR16
    // ══════════════════════════════════════════════════════

    /**
     * Verifies that a search by customer name returns the matching order.
     *
     * @throws AssertionError if the result is empty or the customer name
     *                        does not match
     * Covers FR16
     */
    @Test
    void searchArchivedOrders_byCustomerName_returnsMatch() {
        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(collectedOrder));

        List<Order> result = orderService.searchArchivedOrders("Charlie");

        assertEquals(1, result.size());
        assertEquals("Charlie", result.get(0).getCustomerName());
    }

    /**
     * Verifies that a search by order number returns the matching order.
     *
     * @throws AssertionError if the result is empty
     * Covers FR16
     */
    @Test
    void searchArchivedOrders_byOrderNumber_returnsMatch() {
        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(collectedOrder));

        List<Order> result = orderService.searchArchivedOrders("CH-20260430-003");

        assertEquals(1, result.size());
    }

    /**
     * Verifies that the search is case-insensitive.
     * <p>
     * {@code "charlie"} (lower-case) must match the stored value
     * {@code "Charlie"} (mixed case).
     * </p>
     *
     * @throws AssertionError if the result is empty
     * Covers FR16
     */
    @Test
    void searchArchivedOrders_caseInsensitive_returnsMatch() {
        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(collectedOrder));

        List<Order> result = orderService.searchArchivedOrders("charlie");

        assertEquals(1, result.size());
    }

    /**
     * Verifies that a keyword with no matches returns an empty list.
     *
     * @throws AssertionError if the result is not empty
     * Covers FR16
     */
    @Test
    void searchArchivedOrders_noMatch_returnsEmpty() {
        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(collectedOrder));

        List<Order> result = orderService.searchArchivedOrders("XYZ");

        assertTrue(result.isEmpty());
    }

    /**
     * Verifies that an empty keyword matches all archived orders.
     * <p>
     * An empty string is a substring of every string, so all orders
     * must be returned.
     * </p>
     *
     * @throws AssertionError if the result is empty
     * Covers FR16
     */
    @Test
    void searchArchivedOrders_emptyKeyword_returnsAll() {
        when(orderRepository.findByIsArchivedTrue()).thenReturn(List.of(collectedOrder));

        List<Order> result = orderService.searchArchivedOrders("");

        assertEquals(1, result.size());
    }
}