package com.coffeehut.coffeehut.sixZhangkaichen;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link MemberOrderLink} entities.
 * <p>
 * Provides standard CRUD operations inherited from {@link JpaRepository},
 * plus a custom query to look up a link by its associated order ID.
 * Used exclusively by {@link LoyaltyService} to create and update
 * order-to-member stamp links.
 * </p>
 */
public interface MemberOrderLinkRepository extends JpaRepository<MemberOrderLink, Long> {

    /**
     * Finds the loyalty link associated with a given order.
     * <p>
     * Returns an empty {@link Optional} if no link exists for the order,
     * which indicates the order was placed by a guest who is not enrolled
     * in the loyalty scheme. Used by
     * {@link LoyaltyService#handleCollectedOrder(Long)} to determine whether
     * stamp credit should be awarded when an order reaches
     * {@code collected} status.
     * </p>
     *
     * @param orderId the primary key of the order to look up
     * @return an {@link Optional} containing the matching
     *         {@link MemberOrderLink}, or an empty {@link Optional} if
     *         no link exists for the given order
     */
    Optional<MemberOrderLink> findByOrderId(Long orderId);
}