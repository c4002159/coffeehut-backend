// Order.java — JPA entity for the orders table -WeiqiWang

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

    // Status values agreed across all team members: -WeiqiWang
    //   pending     → new order, awaiting acceptance
    //   in_progress → accepted, being prepared
    //   ready       → ready for pickup
    //   collected   → picked up, complete
    //   cancelled   → cancelled
    private String status = "pending";

    // Timestamps written at each status transition -WeiqiWang
    private LocalDateTime createdAt   = LocalDateTime.now();
    private LocalDateTime acceptedAt;   // pending → in_progress
    private LocalDateTime readyAt;      // in_progress → ready
    private LocalDateTime completedAt;  // collected or cancelled

    private Boolean isArchived = false;

    // Records the status the order held at time of cancellation. -WeiqiWang
    // Used by the staff portal to decide whether to offer a Restore button.
    // Values: "pending" | "in_progress" | null (not yet cancelled)
    private String cancelledFrom;

    private String priority;    // low / medium / high
    private String type;        // Pickup / Delivery / Express
    private String allergies;

    // Notes submitted by customer at time of ordering -WeiqiWang
    private String notes;

    // Notes added by staff after order is placed (via Add Note in OrderDetail) -WeiqiWang
    private String staffNotes;
}
