// OrderItem.java — JPA entity for the order_items table -WeiqiWang

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

    @Column(columnDefinition = "TEXT")
    private String customizations; // comma-separated customisation options -WeiqiWang
}
