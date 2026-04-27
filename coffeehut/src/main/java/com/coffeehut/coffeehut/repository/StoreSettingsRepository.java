// StoreSettingsRepository.java — JPA repository for store_settings table -WeiqiWang

package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
    // No custom queries needed — always read/write id = 1. -WeiqiWang
}
