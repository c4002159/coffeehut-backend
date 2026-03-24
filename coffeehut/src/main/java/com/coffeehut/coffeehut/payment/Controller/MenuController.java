package com.coffeehut.coffeehut.payment.Controller;

import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    @Resource
    private ItemRepository itemRepository;

    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @PostMapping
    public Item createItem(@RequestBody MenuItemRequest request) {
        Item item = new Item();
        item.setName(request.getName());
        item.setRegularPrice(request.getRegularPrice());
        item.setLargePrice(request.getLargePrice());
        return itemRepository.save(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @RequestBody MenuItemRequest request) {
        return itemRepository.findById(id)
                .map(item -> {
                    if (request.getName() != null) item.setName(request.getName());
                    if (request.getRegularPrice() != null) item.setRegularPrice(request.getRegularPrice());
                    if (request.getLargePrice() != null) item.setLargePrice(request.getLargePrice());
                    return ResponseEntity.ok(itemRepository.save(item));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (itemRepository.existsById(id)) {
            itemRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @lombok.Data
    public static class MenuItemRequest {
        private String name;
        private Double regularPrice;
        private Double largePrice;
    }
}
