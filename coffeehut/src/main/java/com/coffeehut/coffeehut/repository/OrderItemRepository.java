package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA repository for {@link OrderItem} entities.
 * <p>
 * Provides standard CRUD operations inherited from {@link JpaRepository},
 * plus a custom query to retrieve all items belonging to a specific order.
 * Used by both the staff-side order detail view and the customer-facing
 * order status page to fetch the line items associated with an order.
 * </p>
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Retrieves all order items belonging to a given order.
     * <p>
     * Used by the service layer to assemble order detail DTOs and active
     * order summaries. Returns an empty list if the order exists but has
     * no associated items, which can occur for reorders created before
     * items were saved.
     * </p>
     *
     * @param orderId the primary key of the parent {@link com.coffeehut.coffeehut.model.Order}
     * @return a list of {@link OrderItem} objects belonging to the given
     *         order; never {@code null} but may be empty
     */
    List<OrderItem> findByOrderId(Long orderId);
}