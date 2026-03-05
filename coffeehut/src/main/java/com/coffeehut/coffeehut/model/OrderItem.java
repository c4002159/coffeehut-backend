package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long itemId;

    private String size;

    private Integer quantity;

    private Double subtotal;
}
