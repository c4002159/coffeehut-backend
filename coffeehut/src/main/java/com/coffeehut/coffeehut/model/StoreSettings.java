// StoreSettings.java — JPA entity for staff-side store configuration -WeiqiWang
// Singleton row (id = 1). Stores Order Automation settings and store open/closed status.

package com.coffeehut.coffeehut.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "store_settings")
@Data
public class StoreSettings {

    @Id
    private Long id; // always 1 — singleton row -WeiqiWang

    // Order Automation -WeiqiWang
    private Boolean autoCancelEnabled;
    private Integer autoCancelMins;
    private Boolean autoCollectEnabled;
    private Integer autoCollectMins;

    // Store open/closed status — set by staff via Schedule page -WeiqiWang
    // false = open (normal), true = temporarily closed by staff
    private Boolean isTemporarilyClosed = false;
}
