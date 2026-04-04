package com.coffeehut.coffeehut.dto;

import com.coffeehut.coffeehut.model.Order;
import com.coffeehut.coffeehut.model.OrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderDetailDTO {
    private Order order;
    private List<OrderItemWithName> items;

    @Data
    public static class OrderItemWithName {
        private Long id;
        private Long orderId;
        private Long itemId;
        private String name;   // 商品名称
        private String size;
        private Integer quantity;
        private Double subtotal;
        // 可根据需要添加 customizations 等
    }
}