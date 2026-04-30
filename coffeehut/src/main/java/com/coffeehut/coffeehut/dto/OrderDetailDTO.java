// OrderDetailDTO.java — DTO for the single order detail endpoint -WeiqiWang

package com.coffeehut.coffeehut.dto;

import com.coffeehut.coffeehut.model.Order;
import lombok.Data;
import java.util.List;

/**
 * DTO returned by {@code GET /api/staff/orders/{id}}.
 * <p>
 * Combines the full {@link Order} entity with a list of order items
 * whose menu item names have been resolved from the {@code items} table,
 * so the staff portal does not need to make a separate name lookup.
 * </p>
 */
@Data
public class OrderDetailDTO {

    /** The full order entity including status, timestamps, and customer info. */
    private Order order;

    /** Order items with resolved menu item names. */
    private List<OrderItemWithName> items;

    /**
     * Single order item enriched with the human-readable menu item name.
     * <p>
     * Mirrors the {@code order_items} table columns plus a resolved
     * {@code name} field looked up from the {@code items} table at query time.
     * </p>
     */
    @Data
    public static class OrderItemWithName {

        /** Primary key of the {@code order_items} row. */
        private Long id;

        /** Foreign key linking this item to its parent order. */
        private Long orderId;

        /** Foreign key referencing the {@code items} table; may be {@code null} for legacy rows. */
        private Long itemId;

        /** Human-readable menu item name resolved from the {@code items} table. */
        private String name;           // resolved from the items table -WeiqiWang

        /** Selected size, e.g. {@code "Regular"} or {@code "Large"}. */
        private String size;

        /** Number of units ordered. */
        private Integer quantity;

        /** Line subtotal (unit price × quantity) in GBP. */
        private Double subtotal;

        /**
         * Customer customisation selections as a comma-separated string,
         * e.g. {@code "Hot,Whole Milk,No Sugar"}.
         */
        private String customizations; // comma-separated list
    }
}
