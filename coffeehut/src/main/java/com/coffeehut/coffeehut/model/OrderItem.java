// OrderItem.java — JPA entity for the order_items table -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing a single line item within an order.
 * <p>
 * Each row records one product, its size, quantity, subtotal, and any
 * customisations selected by the customer. Multiple {@code OrderItem} rows
 * may belong to a single {@link Order}.
 * </p>
 */
@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Foreign key referencing the parent {@link Order#getId()}. */
    private Long orderId;

    /** Foreign key referencing the {@link Item#getId()} that was ordered. */
    private Long itemId;

    /** Size selected by the customer (e.g. {@code "Regular"} or {@code "Large"}). */
    private String size;

    /** Number of units of this item ordered. */
    private Integer quantity;

    /** Line subtotal in GBP ({@code unitPrice × quantity}). */
    private Double subtotal;

    /** Comma-separated list of customisation options selected by the customer. */
    @Column(columnDefinition = "TEXT")
    private String customizations; // comma-separated customisation options -WeiqiWang
}