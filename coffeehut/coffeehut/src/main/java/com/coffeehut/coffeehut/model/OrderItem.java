package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private Long itemId;
    private String size;
    private Integer quantity;
    private Double subtotal;

    // 新增定制项字段（存储为逗号分隔字符串）
    @Column(columnDefinition = "TEXT")
    private String customizations;
}