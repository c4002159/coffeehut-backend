// Item.java — JPA entity for the items (menu catalogue) table -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing a menu item available for purchase.
 * <p>
 * Each row corresponds to a single drink or product in the {@code items} table.
 * Availability and stock are managed by staff via the Inventory page.
 * </p>
 */
@Entity
@Table(name = "items")
@Data
public class Item {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name of the item (e.g. {@code "Latte"}). */
    private String name;

    /** Price in GBP for a regular-sized serving. */
    private Double regularPrice;

    /** Price in GBP for a large-sized serving, or {@code null} if only one size exists. */
    private Double largePrice;

    /**
     * Whether this item is available for customer ordering.
     * <p>
     * Set to {@code false} by staff to manually take an item offline,
     * regardless of stock level.
     * </p>
     */
    private Boolean isAvailable = true;

    /**
     * Current stock quantity, managed by staff via the Inventory page.
     * <p>
     * {@code null} means stock tracking has not been set up yet (treated as in-stock
     * for backwards compatibility). {@code 0} means out of stock — the item will
     * still appear in the customer menu but the Add button will be disabled.
     * </p>
     */
    // Stock quantity — managed by staff via Inventory page. -WeiqiWang
    // null means stock tracking is not set up yet (treated as in-stock for backwards compatibility).
    // 0 means out of stock — item will not appear in customer menu.
    private Integer stock;
}