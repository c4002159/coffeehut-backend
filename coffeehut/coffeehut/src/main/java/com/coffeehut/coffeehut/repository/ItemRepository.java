package com.coffeehut.coffeehut.repository;

import com.coffeehut.coffeehut.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
