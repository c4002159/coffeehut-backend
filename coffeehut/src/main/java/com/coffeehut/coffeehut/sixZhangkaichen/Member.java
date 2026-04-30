package com.coffeehut.coffeehut.sixZhangkaichen;

import jakarta.persistence.*;

/**
 * JPA entity representing a loyalty scheme member.
 * <p>
 * Maps to the {@code members} table in the database. Each member earns
 * one stamp per completed order ({@code totalOrders}), and one free cup
 * is awarded for every 10 stamps accumulated. The free-cup balance is
 * tracked separately in {@code freeCups} so it can be adjusted
 * independently by staff if needed.
 * </p>
 * <p>
 * Note: this entity is distinct from the staff accounts stored in the
 * {@code staff_accounts} table — loyalty members are customers only.
 * </p>
 */
@Entity(name = "LoyaltyMember")
@Table(name = "members")
public class Member {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The member's display name as provided at registration. */
    private String name;

    /**
     * The member's email address.
     * <p>
     * Must be unique across all loyalty members. Stored in lower-case
     * so that lookups are case-insensitive.
     * </p>
     */
    @Column(unique = true)
    private String email;

    /**
     * The member's plain-text password.
     * <p>
     * Stored without hashing in the current implementation.
     * </p>
     */
    private String password;

    /**
     * The cumulative number of orders completed by this member.
     * <p>
     * Incremented by {@link LoyaltyService#addOneOrder(Long)} each time
     * an order reaches {@code collected} status. May be {@code null} for
     * legacy records created before this field was introduced; callers
     * should treat {@code null} as zero.
     * </p>
     */
    private Integer totalOrders;

    /**
     * The number of free cups the member has earned but not yet redeemed.
     * <p>
     * Initialised to {@code 0} on creation. One free cup is awarded for
     * every 10 completed orders. Negative values are prevented by the
     * service layer.
     * </p>
     */
    private Integer freeCups = 0;

    /**
     * Default no-argument constructor required by JPA.
     */
    public Member() {
    }

    /**
     * Convenience constructor for creating a fully initialised member.
     * <p>
     * {@code freeCups} is always set to {@code 0} on construction
     * regardless of the caller's intent, because a brand-new member
     * starts with no free cups.
     * </p>
     *
     * @param name        the member's display name
     * @param email       the member's email address (should be pre-normalised
     *                    to lower-case by the caller)
     * @param password    the member's plain-text password
     * @param totalOrders the initial order count (typically {@code 0}
     *                    for a new member)
     */
    public Member(String name, String email, String password, Integer totalOrders) {
        this.name        = name;
        this.email       = email;
        this.password    = password;
        this.totalOrders = totalOrders;
        this.freeCups    = 0;
    }

    /**
     * Returns the auto-generated primary key of this member.
     *
     * @return the member's unique identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the member's display name.
     *
     * @return the member's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the member's display name.
     *
     * @param name the new display name; must not be blank
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the member's email address.
     *
     * @return the member's email address in lower-case
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the member's email address.
     * <p>
     * The caller is responsible for normalising the value to lower-case
     * before calling this method so that uniqueness constraints are
     * enforced consistently.
     * </p>
     *
     * @param email the new email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the member's plain-text password.
     *
     * @return the stored password string
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the member's plain-text password.
     *
     * @param password the new password; must not be blank
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the cumulative number of orders completed by this member.
     * <p>
     * May return {@code null} for legacy records. Callers should treat
     * {@code null} as {@code 0}.
     * </p>
     *
     * @return the total order count, or {@code null} for legacy records
     */
    public Integer getTotalOrders() {
        return totalOrders;
    }

    /**
     * Sets the cumulative order count for this member.
     *
     * @param totalOrders the new total order count; must not be negative
     */
    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    /**
     * Returns the number of free cups the member has earned but not redeemed.
     * <p>
     * Returns {@code 0} if the stored value is {@code null}, which can
     * occur for records created before the {@code freeCups} column was
     * added to the schema.
     * </p>
     *
     * @return the free-cup balance; never {@code null}
     */
    public Integer getFreeCups() {
        // Null-safe fallback for legacy records that pre-date this column
        return freeCups == null ? 0 : freeCups;
    }

    /**
     * Sets the number of free cups available to this member.
     *
     * @param freeCups the new free-cup balance; must not be negative
     */
    public void setFreeCups(Integer freeCups) {
        this.freeCups = freeCups;
    }
}