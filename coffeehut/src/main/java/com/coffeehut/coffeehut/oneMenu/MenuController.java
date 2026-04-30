package com.coffeehut.coffeehut.oneMenu;
import com.coffeehut.coffeehut.model.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller exposing the customer-facing menu API.
 * <p>
 * Handles GET requests to {@code /api/menu} and returns only items that are
 * currently available for ordering. Delegates all filtering logic to
 * {@link MenuService}.
 * </p>
 */
@RestController
@RequestMapping("/api/menu")
@CrossOrigin(origins = "*")
public class MenuController {

    /** Service responsible for menu data retrieval and seed initialisation. */
    @Autowired
    private MenuService menuService;

    /**
     * Returns all menu items currently visible to customers.
     * <p>
     * Items that have been manually taken offline ({@code isAvailable = false})
     * are excluded. Items with {@code stock = 0} are still included so the
     * frontend can render a "Sold Out" state rather than hiding the item entirely.
     * </p>
     *
     * @return a list of available {@link Item} entities; never {@code null}
     */
    @GetMapping
    public List<Item> getMenu() {
        return menuService.getAvailableItems();
    }
}