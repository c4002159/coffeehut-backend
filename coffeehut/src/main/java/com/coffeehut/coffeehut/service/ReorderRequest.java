package com.coffeehut.coffeehut.service;

import com.coffeehut.coffeehut.model.OrderItem;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request payload for creating a reorder based on a previous order.
 * <p>
 * Carries the customer details, desired pickup time, total price, and
 * the list of items to be reordered. Populated by the service layer from
 * a previously completed order and passed to
 * {@link OrderService#createReorder(ReorderRequest)} to persist the new order.
 * </p>
 * <p>
 * This class is a plain data-transfer object with no validation logic;
 * all validation and business rules are enforced by the service layer
 * that consumes it.
 * </p>
 */
public class ReorderRequest {

    /** The name of the customer placing the reorder. */
    private String customerName;

    /** The customer's phone number; may be {@code null} if not provided. */
    private String customerPhone;

    /**
     * The requested pickup time for the reorder.
     * <p>
     * If {@code null}, {@link OrderService#createReorder(ReorderRequest)}
     * defaults the pickup time to 10 minutes from the moment the reorder
     * is created.
     * </p>
     */
    private LocalDateTime pickupTime;

    /**
     * The total price of the reorder in GBP.
     * <p>
     * Should reflect the sum of all item subtotals. The service layer
     * stores this value directly without recalculating from individual
     * item prices.
     * </p>
     */
    private Double totalPrice;

    /**
     * The list of {@link OrderItem} entries to be included in the reorder.
     * <p>
     * Each entry carries the item ID, size, quantity, subtotal, and any
     * customisations copied from the original order. May be {@code null}
     * if the caller omits the field, in which case no order items are saved.
     * </p>
     */
    private List<OrderItem> items;

    /**
     * Returns the name of the customer placing the reorder.
     *
     * @return the customer's name
     */
    public String getCustomerName() { return customerName; }

    /**
     * Sets the name of the customer placing the reorder.
     *
     * @param customerName the customer's name; must not be blank
     */
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    /**
     * Returns the customer's phone number.
     *
     * @return the customer's phone number, or {@code null} if not provided
     */
    public String getCustomerPhone() { return customerPhone; }

    /**
     * Sets the customer's phone number.
     *
     * @param customerPhone the customer's phone number; may be {@code null}
     */
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    /**
     * Returns the requested pickup time for the reorder.
     *
     * @return the pickup time, or {@code null} if not specified by the caller
     */
    public LocalDateTime getPickupTime() { return pickupTime; }

    /**
     * Sets the requested pickup time for the reorder.
     * <p>
     * When {@code null}, {@link OrderService#createReorder(ReorderRequest)}
     * will default the pickup time to 10 minutes after the reorder is created.
     * </p>
     *
     * @param pickupTime the desired pickup time, or {@code null} to use
     *                   the service-layer default
     */
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }

    /**
     * Returns the total price of the reorder in GBP.
     *
     * @return the total price
     */
    public Double getTotalPrice() { return totalPrice; }

    /**
     * Sets the total price of the reorder in GBP.
     *
     * @param totalPrice the total price; should be a positive value
     */
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    /**
     * Returns the list of items to be included in the reorder.
     *
     * @return the list of {@link OrderItem} entries copied from the original
     *         order, or {@code null} if no items were provided
     */
    public List<OrderItem> getItems() { return items; }

    /**
     * Sets the list of items to be included in the reorder.
     *
     * @param items the list of {@link OrderItem} entries to reorder;
     *              may be {@code null}, in which case no items are persisted
     */
    public void setItems(List<OrderItem> items) { this.items = items; }
}
