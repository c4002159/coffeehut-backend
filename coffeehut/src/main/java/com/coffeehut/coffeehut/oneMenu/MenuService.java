package com.coffeehut.coffeehut.oneMenu;

import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuService {

    @Autowired
    private ItemRepository itemRepository;

    @PostConstruct
    public void initData() {
        if (itemRepository.count() == 0) {
            // Default stock = 99 so items appear in menu from day one. -WeiqiWang
            itemRepository.save(createItem("Americano",          1.50, 2.00,  99));
            itemRepository.save(createItem("Americano with Milk",2.00, 2.50,  99));
            itemRepository.save(createItem("Latte",              2.50, 3.00,  99));
            itemRepository.save(createItem("Cappuccino",         2.50, 3.00,  99));
            itemRepository.save(createItem("Hot Chocolate",      2.00, 2.50,  99));
            itemRepository.save(createItem("Mocha",              2.50, 3.00,  99));
            itemRepository.save(createItem("Mineral Water",      1.00, null,  99));
        }
    }

    private Item createItem(String name, Double regular, Double large, Integer stock) {
        Item item = new Item();
        item.setName(name);
        item.setRegularPrice(regular);
        item.setLargePrice(large);
        item.setIsAvailable(true);
        item.setStock(stock);
        return item;
    }

    // Returns items visible to customers: must be available AND have stock > 0.
    // If stock is null (legacy rows before stock tracking), treat as in-stock. -WeiqiWang
    public List<Item> getAvailableItems() {
        return itemRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()))
                .filter(item -> item.getStock() == null || item.getStock() > 0)
                .collect(Collectors.toList());
    }
}
