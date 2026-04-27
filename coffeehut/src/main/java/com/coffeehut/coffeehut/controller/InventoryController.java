// InventoryController.java — Staff inventory management endpoints -WeiqiWang
// GET  /api/staff/inventory        — list all items with stock and availability
// PATCH /api/staff/inventory/{id}  — update stock and/or availability for one item
//
// Completely isolated under /api/staff/. Customer menu uses /api/menu. -WeiqiWang

package com.coffeehut.coffeehut.controller;

import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/staff/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    @Autowired
    private ItemRepository itemRepository;

    // GET /api/staff/inventory — returns all items (including offline ones). -WeiqiWang
    // Staff need to see everything; customer menu only sees available + in-stock items.
    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    // PATCH /api/staff/inventory/{id} — update stock and/or availability. -WeiqiWang
    // Accepts any combination of: { "stock": 10, "isAvailable": true }
    @PatchMapping("/{id}")
    public ResponseEntity<Item> updateItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        Optional<Item> optional = itemRepository.findById(id);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        Item item = optional.get();

        if (updates.containsKey("stock")) {
            Object raw = updates.get("stock");
            if (raw instanceof Number) {
                int val = ((Number) raw).intValue();
                item.setStock(Math.max(0, val)); // clamp to 0 -WeiqiWang
            }
        }

        if (updates.containsKey("isAvailable")) {
            Object raw = updates.get("isAvailable");
            if (raw instanceof Boolean) {
                item.setIsAvailable((Boolean) raw);
            }
        }

        return ResponseEntity.ok(itemRepository.save(item));
    }
}
