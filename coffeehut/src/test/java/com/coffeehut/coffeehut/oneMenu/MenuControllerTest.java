package com.coffeehut.coffeehut.oneMenu;

import com.coffeehut.coffeehut.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MenuController}.
 * <p>
 * Covers requirements: FR2, FR21.
 * All dependencies are mocked via Mockito — no real database is used.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock
    private MenuService menuService;

    private MenuController menuController;

    private Item availableItem;
    private Item unavailableItem;
    private Item soldOutItem;

    @BeforeEach
    void setUp() {
        menuController = new MenuController();
        try {
            var field = MenuController.class.getDeclaredField("menuService");
            field.setAccessible(true);
            field.set(menuController, menuService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        availableItem = new Item();
        availableItem.setId(1L);
        availableItem.setName("Latte");
        availableItem.setRegularPrice(2.50);
        availableItem.setLargePrice(3.00);
        availableItem.setIsAvailable(true);
        availableItem.setStock(10);

        unavailableItem = new Item();
        unavailableItem.setId(2L);
        unavailableItem.setName("Cappuccino");
        unavailableItem.setIsAvailable(false);
        unavailableItem.setStock(5);

        soldOutItem = new Item();
        soldOutItem.setId(3L);
        soldOutItem.setName("Americano");
        soldOutItem.setIsAvailable(true);
        soldOutItem.setStock(0);
    }

    /**
     * Verifies that {@code getMenu()} returns the list provided by
     * {@link MenuService#getAvailableItems()} without any additional filtering.
     * <p>
     * The controller must delegate entirely to the service layer.
     * </p>
     *
     * @throws AssertionError if the returned list does not match the service response
     * Covers FR2
     */
    @Test
    void getMenu_withAvailableItems_returnsServiceResult() {
        when(menuService.getAvailableItems()).thenReturn(List.of(availableItem));

        List<Item> result = menuController.getMenu();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Latte");
        verify(menuService).getAvailableItems();
    }

    /**
     * Verifies that {@code getMenu()} returns an empty list when the service
     * returns no items, rather than throwing an exception or returning {@code null}.
     *
     * @throws AssertionError if the result is {@code null} or an exception is thrown
     * Covers FR2
     */
    @Test
    void getMenu_withNoAvailableItems_returnsEmptyList() {
        when(menuService.getAvailableItems()).thenReturn(Collections.emptyList());

        List<Item> result = menuController.getMenu();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    /**
     * Verifies that {@code getMenu()} includes sold-out items ({@code stock = 0})
     * so the frontend can render a "Sold Out" state.
     * <p>
     * Boundary case: sold-out item with {@code isAvailable = true} must appear
     * in the response.
     * </p>
     *
     * @throws AssertionError if the sold-out item is absent from the response
     * Covers FR21
     */
    @Test
    void getMenu_withSoldOutItem_includesSoldOutInResponse() {
        when(menuService.getAvailableItems()).thenReturn(List.of(soldOutItem));

        List<Item> result = menuController.getMenu();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStock()).isZero();
    }
}
