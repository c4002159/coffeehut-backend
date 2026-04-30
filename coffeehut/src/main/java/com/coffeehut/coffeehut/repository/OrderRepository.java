// OrderRepository.java — JPA repository for the orders table -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing {@link Order} entities.
 * <p>
 * This interface provides data access operations for the orders table using Spring Data JPA.
 * It extends {@link JpaRepository} to inherit standard CRUD operations, and also defines
 * custom query methods for retrieving and counting orders based on business-specific criteria
 * such as customer name, archival status, and creation time range.
 * </p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Retrieves a list of orders by customer name.
     * <p>
     * This method performs a query based on the {@code customerName} field of the {@link Order} entity.
     * It returns all orders associated with the specified customer.
     * </p>
     *
     * @param customerName the name of the customer used to filter orders
     * @return a list of matching {@link Order} objects, or an empty list if no orders are found
     */
    List<Order> findByCustomerName(String customerName);

    /**
     * Retrieves all non-archived (active) orders.
     * <p>
     * This method filters orders where the {@code isArchived} flag is {@code false}.
     * It is typically used to display current or active orders in the system.
     * </p>
     *
     * @return a list of active {@link Order} objects, or an empty list if none exist
     */
    List<Order> findByIsArchivedFalse(); // active orders -WeiqiWang

    /**
     * Retrieves all archived orders.
     * <p>
     * This method filters orders where the {@code isArchived} flag is {@code true}.
     * Archived orders are typically no longer active and may be used for historical records.
     * </p>
     *
     * @return a list of archived {@link Order} objects, or an empty list if none exist
     */
    List<Order> findByIsArchivedTrue();  // archived orders -WeiqiWang

    /**
     * Retrieves all archived orders sorted by creation time in descending order.
     * <p>
     * This method returns archived orders (where {@code isArchived} is {@code true})
     * sorted by the {@code createdAt} field, with the most recent orders appearing first.
     * This is useful for displaying recent archived records.
     * </p>
     *
     * @return a list of archived {@link Order} objects sorted by creation time descending,
     *         or an empty list if none exist
     */
    List<Order> findByIsArchivedTrueOrderByCreatedAtDesc();

    /**
     * Counts the number of orders created within a specified time range.
     * <p>
     * This method uses a custom JPQL query to count orders where the {@code createdAt}
     * timestamp falls between the given {@code start} (inclusive) and {@code end} (exclusive).
     * It is commonly used for generating daily order numbers (e.g. <code>A-001</code>)
     * or for reporting purposes.
     * </p>
     *
     * @param start the start of the time range (inclusive)
     * @param end the end of the time range (exclusive)
     * @return the number of orders created within the specified time range
     */
    // Used by OrderController to generate daily order numbers (e.g. A-001). -WeiqiWang
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    long countOrdersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
