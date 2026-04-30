package com.coffeehut.coffeehut.oneMenu;
import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for customer-facing menu operations.
 * <p>
 * Responsible for seeding the initial menu catalogue on first startup and
 * for querying items that should be visible to customers. Stock-out items
 * are intentionally retained in query results so the frontend can display
 * a "Sold Out" indicator rather than silently hiding them.
 * </p>
 */
@Service
public class MenuService {

    /** Repository used to read and persist {@link Item} records. */
    @Autowired
    private ItemRepository itemRepository;

    /**
     * Seeds the database with the default menu catalogue on first application startup.
     * <p>
     * Only runs when the {@code items} table is completely empty, ensuring that
     * existing data is never overwritten after the first deployment.
     * Each item is created with {@code stock = 99} so it appears immediately
     * in the customer menu without requiring manual stock entry.
     * </p>
     */
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

    /**
     * Constructs a new {@link Item} entity with the supplied attributes.
     * <p>
     * All items created via this helper are marked as available by default.
     * </p>
     *
     * @param name    display name of the item
     * @param regular price in GBP for a regular-sized serving
     * @param large   price in GBP for a large-sized serving, or {@code null} if no large size exists
     * @param stock   initial stock quantity
     * @return a new, unsaved {@link Item} instance
     */
    private Item createItem(String name, Double regular, Double large, Integer stock) {
        Item item = new Item();
        item.setName(name);
        item.setRegularPrice(regular);
        item.setLargePrice(large);
        item.setIsAvailable(true);
        item.setStock(stock);
        return item;
    }

    /**
     * Returns all menu items visible to customers.
     * <p>
     * Only items where {@code isAvailable = true} are included; items manually
     * taken offline by staff are excluded. Items with {@code stock = 0} are
     * still returned so the frontend can show a "Sold Out" button rather than
     * hiding the item entirely.
     * </p>
     *
     * @return a filtered list of {@link Item} entities; never {@code null}
     */
    // Returns items visible to customers: must be available (not manually taken offline).
    // Stock=0 items are still returned so the frontend can show Sold Out. -WeiqiWang
    public List<Item> getAvailableItems() {
        return itemRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsAvailable()))
                .collect(Collectors.toList());
    }
}