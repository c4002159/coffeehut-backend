// OrderWithItemsDTO.java — DTO for the active order list endpoint -WeiqiWang

package com.coffeehut.coffeehut.dto;

import com.coffeehut.coffeehut.model.Order;
import lombok.Data;
import java.util.List;

@Data
public class OrderWithItemsDTO {

    private Order order;
    private List<OrderItemSummary> items;

    @Data
    public static class OrderItemSummary {
        private String name;           // resolved from the items table -WeiqiWang
        private Integer quantity;
        private String size;           // "Regular" or "Large"
        private String customizations; // comma-separated list
    }
}
