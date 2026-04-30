// Order.java — JPA entity for the orders table -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * JPA entity representing a customer order.
 * <p>
 * Tracks the full lifecycle of an order from placement through to collection
 * or cancellation. Status transitions are managed by staff via the dashboard.
 * </p>
 */
@Entity
@Table(name = "orders")
@Data
public class Order {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name provided by the customer at time of ordering. */
    private String customerName;

    /** Optional phone number provided by the customer. */
    private String customerPhone;

    /** Human-readable order reference (e.g. {@code "CH-20260430-001"}). */
    @Column(unique = true)
    private String orderNumber;

    /** Requested pickup time selected by the customer. */
    private LocalDateTime pickupTime;

    /** Total order value in GBP including service fee and tax. */
    private Double totalPrice;

    /**
     * Current order status.
     * <p>
     * Valid values (agreed across all team members):
     * <ul>
     *   <li>{@code "pending"}     — new order, awaiting acceptance</li>
     *   <li>{@code "in_progress"} — accepted, being prepared</li>
     *   <li>{@code "ready"}       — ready for pickup</li>
     *   <li>{@code "collected"}   — picked up, complete</li>
     *   <li>{@code "cancelled"}   — cancelled</li>
     * </ul>
     * </p>
     */
    // Status values agreed across all team members: -WeiqiWang
    private String status = "pending";

    /** Timestamp when the order was created. */
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Timestamp when the order was accepted (transition: {@code pending} to {@code in_progress}). */
    private LocalDateTime acceptedAt;

    /** Timestamp when the order was marked ready (transition: {@code in_progress} to {@code ready}). */
    private LocalDateTime readyAt;

    /** Timestamp when the order was collected or cancelled. */
    private LocalDateTime completedAt;

    /** Whether this order has been moved to the archive view. */
    private Boolean isArchived = false;

    /**
     * Status the order held at the time of cancellation.
     * <p>
     * Used by the staff portal to decide whether to show a Restore button.
     * Values: {@code "pending"} | {@code "in_progress"} | {@code null} (not cancelled).
     * </p>
     */
    // Records the status the order held at time of cancellation. -WeiqiWang
    private String cancelledFrom;

    /** Optional priority flag set by staff (e.g. {@code "low"}, {@code "medium"}, {@code "high"}). */
    private String priority;

    /** Order type (e.g. {@code "Pickup"}, {@code "Delivery"}, {@code "Express"}). */
    private String type;

    /** Allergy information provided by the customer. */
    private String allergies;

    /** Special instructions submitted by the customer at time of ordering. */
    // Notes submitted by customer at time of ordering -WeiqiWang
    private String notes;

    /** Additional notes added by staff after the order is placed. */
    // Notes added by staff after order is placed (via Add Note in OrderDetail) -WeiqiWang
    private String staffNotes;
}