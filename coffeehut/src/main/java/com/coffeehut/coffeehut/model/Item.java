// Item.java — JPA entity for the items (menu catalogue) table -WeiqiWang

package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "items")
@Data
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double regularPrice;
    private Double largePrice;
    private Boolean isAvailable = true;

    // Stock quantity — managed by staff via Inventory page. -WeiqiWang
    // null means stock tracking is not set up yet (treated as in-stock for backwards compatibility).
    // 0 means out of stock — item will not appear in customer menu.
    private Integer stock;
}
