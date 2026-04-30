package com.coffeehut.coffeehut.model;
import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity representing a loyalty scheme member.
 * <p>
 * Stores customer account details and loyalty progress. This entity maps to
 * the {@code members} table and is separate from {@code StaffAccount}.
 * </p>
 */
@Entity
@Table(name = "members")
@Data
public class Member {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name of the member. */
    private String name;

    /** Unique email address used for login. */
    @Column(unique = true)
    private String email;

    /** Plain-text password (to be hashed in future iterations). */
    private String password;

    /**
     * Cumulative cup count used to track loyalty stamp progress.
     * <p>
     * Increments by the number of cups purchased per order.
     * Resets to the remainder after every 9 cups earned.
     * </p>
     */
    private Integer totalOrders = 0;

    /**
     * Number of unused free cups available to the member.
     * <p>
     * Incremented when {@code totalOrders} reaches a multiple of 9.
     * Decremented when a free cup is redeemed at checkout.
     * </p>
     */
    private Integer freeCups = 0;
}