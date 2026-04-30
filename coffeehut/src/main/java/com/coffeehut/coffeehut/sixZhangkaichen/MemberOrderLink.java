package com.coffeehut.coffeehut.sixZhangkaichen;

import jakarta.persistence.*;

/**
 * JPA entity representing the link between a loyalty member and an order.
 * <p>
 * Maps to the {@code member_order_links} table. Each row associates one
 * {@link Member} with one order placed by that member, and tracks whether
 * the order has already been counted toward the member's stamp total via
 * the {@code counted} flag.
 * </p>
 * <p>
 * The two-step design — create the link when the order is placed, mark it
 * {@code counted = true} only when the order is collected — prevents stamps
 * from being awarded before the customer receives their order and ensures
 * the increment is idempotent even if the collected-order hook is triggered
 * more than once.
 * </p>
 */
@Entity
@Table(name = "member_order_links")
public class MemberOrderLink {

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The primary key of the {@link Member} who placed the order. */
    private Long memberId;

    /** The primary key of the order associated with this link. */
    private Long orderId;

    /**
     * Whether this order has already been counted toward the member's stamp total.
     * <p>
     * Set to {@code false} when the link is first created (order placed) and
     * updated to {@code true} by {@link LoyaltyService#handleCollectedOrder(Long)}
     * once the order reaches {@code collected} status. Guards against
     * double-counting if the handler is invoked more than once for the same order.
     * </p>
     */
    private Boolean counted;

    /**
     * Default no-argument constructor required by JPA.
     */
    public MemberOrderLink() {
    }

    /**
     * Convenience constructor for creating a fully initialised link record.
     *
     * @param memberId the primary key of the loyalty member who placed the order
     * @param orderId  the primary key of the order being linked
     * @param counted  {@code false} when the link is first created;
     *                 {@code true} once the order has been collected and
     *                 the stamp has been awarded
     */
    public MemberOrderLink(Long memberId, Long orderId, Boolean counted) {
        this.memberId = memberId;
        this.orderId  = orderId;
        this.counted  = counted;
    }

    /**
     * Returns the auto-generated primary key of this link record.
     *
     * @return the unique identifier of this {@link MemberOrderLink}
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the primary key of the loyalty member associated with this link.
     *
     * @return the member's unique identifier
     */
    public Long getMemberId() {
        return memberId;
    }

    /**
     * Returns the primary key of the order associated with this link.
     *
     * @return the order's unique identifier
     */
    public Long getOrderId() {
        return orderId;
    }

    /**
     * Returns whether this order has already been counted toward the member's
     * stamp total.
     * <p>
     * Callers should use {@link Boolean#TRUE#equals(Object)} rather than
     * a direct equality check to guard against a {@code null} value that
     * could exist in legacy records.
     * </p>
     *
     * @return {@code true} if the stamp has been awarded, {@code false} if
     *         the order has been placed but not yet collected, or {@code null}
     *         for legacy records created before this field was introduced
     */
    public Boolean getCounted() {
        return counted;
    }

    /**
     * Sets the primary key of the loyalty member associated with this link.
     *
     * @param memberId the primary key of the loyalty member
     */
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    /**
     * Sets the primary key of the order associated with this link.
     *
     * @param orderId the primary key of the order
     */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /**
     * Sets whether this order has been counted toward the member's stamp total.
     * <p>
     * Should only be set to {@code true} by
     * {@link LoyaltyService#handleCollectedOrder(Long)} after confirming the
     * order has reached {@code collected} status and the stamp has not already
     * been awarded.
     * </p>
     *
     * @param counted {@code true} to mark the stamp as awarded,
     *                {@code false} to indicate it is still pending
     */
    public void setCounted(Boolean counted) {
        this.counted = counted;
    }
}