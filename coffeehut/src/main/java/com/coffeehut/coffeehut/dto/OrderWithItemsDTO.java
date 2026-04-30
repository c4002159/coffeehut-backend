// OrderWithItemsDTO.java — DTO for the active order list endpoint -WeiqiWang

package com.coffeehut.coffeehut.dto;

import com.coffeehut.coffeehut.model.Order;
import lombok.Data;
import java.util.List;

/**
 * DTO returned by {@code GET /api/staff/orders/active}.
 * <p>
 * Pairs an {@link Order} entity with a lightweight summary of its items,
 * including resolved menu item names, to populate staff dashboard order cards.
 * </p>
 */
@Data
public class OrderWithItemsDTO {

    /** The full order entity including status, timestamps, and customer info. */
    private Order order;

    /** Lightweight summaries of each item in the order. */
    private List<OrderItemSummary> items;

    /**
     * Lightweight summary of a single order item for dashboard card display.
     * <p>
     * Contains only the fields needed to render the item row on the staff dashboard.
     * Subtotal and foreign keys are omitted to keep the payload compact.
     * </p>
     */
    @Data
    public static class OrderItemSummary {
        /** Human-readable menu item name resolved from the {@code items} table. */
        private String name;           // resolved from the items table -WeiqiWang
        /** Number of units ordered. */
        private Integer quantity;
        /** Selected size, either {@code "Regular"} or {@code "Large"}. */
        private String size;           // "Regular" or "Large"
        /** Customer customisation selections as a comma-separated string. */
        private String customizations; // comma-separated list
    }
}
