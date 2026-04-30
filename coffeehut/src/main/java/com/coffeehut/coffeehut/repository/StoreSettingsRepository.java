// StoreSettingsRepository.java — JPA repository for store_settings table -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link StoreSettings} entities.
 * <p>
 * This interface provides data access operations for the store_settings table
 * using Spring Data JPA. By extending {@link JpaRepository}, it inherits standard
 * CRUD functionality such as saving, retrieving, updating, and deleting records.
 * </p>
 * <p>
 * In this application, the store settings are treated as a singleton configuration,
 * where a single row (typically with ID {@code 1}) represents the global settings
 * of the store. Therefore, no custom query methods are required.
 * </p>
 */
@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {

    // No custom queries needed — always read/write id = 1. -WeiqiWang

}