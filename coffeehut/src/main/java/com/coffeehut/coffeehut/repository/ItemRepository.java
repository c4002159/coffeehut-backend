package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Item} entities.
 * <p>
 * Provides standard CRUD operations and pagination support inherited
 * from {@link JpaRepository}. Used by both the customer-facing menu
 * endpoint and the staff inventory management endpoint to read and
 * update menu item data including stock levels and availability flags.
 * </p>
 * <p>
 * No custom query methods are required at this time — all lookups
 * beyond simple ID-based retrieval are performed in the service layer
 * via in-memory filtering of {@link JpaRepository#findAll()} results.
 * </p>
 */
public interface ItemRepository extends JpaRepository<Item, Long> {
}