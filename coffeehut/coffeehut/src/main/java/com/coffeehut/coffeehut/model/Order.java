package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;
    private String customerPhone;
    @Column(unique = true)
    private String orderNumber;
    private LocalDateTime pickupTime;
    private Double totalPrice;
    private String status = "new"; // new, preparing, ready, collected
    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean isArchived = false;

    // 新增字段
    private String priority;   // low, medium, high
    private String type;       // Pickup, Delivery, Express
    private String allergies;
    private String notes;
}