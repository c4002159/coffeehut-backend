// StoreSettings.java — JPA entity for staff-side store configuration -WeiqiWang
package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing global store configuration managed by staff.
 * <p>
 * This is a singleton entity — only one row with {@code id = 1} ever exists.
 * It stores order automation settings and the store's open/closed override flags.
 * </p>
 */
@Entity
@Table(name = "store_settings")
@Data
public class StoreSettings {

    /**
     * Fixed primary key — always {@code 1}.
     * <p>
     * This is a singleton row; no additional rows should ever be inserted.
     * </p>
     */
    // always 1 — singleton row -WeiqiWang
    @Id
    private Long id;

    /** Whether the auto-cancel automation is enabled. */
    private Boolean autoCancelEnabled;

    /** Number of minutes after which a pending order is automatically cancelled. */
    private Integer autoCancelMins;

    /** Whether the auto-collect automation is enabled. */
    private Boolean autoCollectEnabled;

    /** Number of minutes after which a ready order is automatically marked as collected. */
    private Integer autoCollectMins;

    /**
     * Whether the store has been temporarily closed by staff.
     * <p>
     * When {@code true}, customers cannot place new orders regardless of the
     * weekly schedule or holiday settings.
     * </p>
     */
    // Store open/closed status — set by staff via Schedule page -WeiqiWang
    private Boolean isTemporarilyClosed = false;

    /**
     * Manual force-open flag set when staff clicks the Reopen Store button.
     * <p>
     * When {@code true}, overrides Holiday and Weekly Schedule closed states.
     * Cleared automatically when a Holiday or Weekly Schedule entry is saved,
     * so that schedule changes take effect again.
     * </p>
     */
    // Manual force-open flag — set when staff clicks Reopen Store button. -WeiqiWang
    private Boolean manualForceOpen = false;
}