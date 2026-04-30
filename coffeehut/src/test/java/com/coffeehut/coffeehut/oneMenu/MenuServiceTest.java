package com.coffeehut.coffeehut.oneMenu;

import com.coffeehut.coffeehut.model.Item;
import com.coffeehut.coffeehut.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MenuService}.
 * <p>
 * Covers requirements: FR2, FR21.
 * All dependencies are mocked via Mockito — no real database is used.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private MenuService menuService;

    private Item availableItem;
    private Item unavailableItem;
    private Item soldOutItem;

    @BeforeEach
    void setUp() {
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
     * Verifies that {@code getAvailableItems()} returns only items where
     * {@code isAvailable} is {@code true}.
     * <p>
     * Items manually taken offline by staff ({@code isAvailable = false})
     * must be excluded even when they have stock remaining.
     * </p>
     *
     * @throws AssertionError if the returned list contains offline items
     * Covers FR2
     */
    @Test
    void getAvailableItems_withMixedAvailability_returnsOnlyAvailableItems() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(availableItem, unavailableItem));

        List<Item> result = menuService.getAvailableItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Latte");
        assertThat(result).doesNotContain(unavailableItem);
    }

    /**
     * Verifies that {@code getAvailableItems()} returns an empty list when
     * the repository contains no items at all.
     *
     * @throws AssertionError if the result is {@code null} or non-empty
     * Covers FR2
     */
    @Test
    void getAvailableItems_withEmptyRepository_returnsEmptyList() {
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());

        List<Item> result = menuService.getAvailableItems();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    /**
     * Verifies that {@code getAvailableItems()} includes items with
     * {@code stock = 0} as long as {@code isAvailable} is {@code true}.
     * <p>
     * Sold-out items must still be returned so the frontend can render
     * a "Sold Out" button rather than hiding the item entirely.
     * </p>
     *
     * @throws AssertionError if the sold-out item is missing from the result
     * Covers FR21
     */
    @Test
    void getAvailableItems_withZeroStock_stillReturnsItem() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(availableItem, soldOutItem));

        List<Item> result = menuService.getAvailableItems();

        assertThat(result).contains(soldOutItem);
        assertThat(result.stream()
                .filter(i -> i.getName().equals("Americano"))
                .findFirst()
                .map(Item::getStock)
                .orElse(-1)).isZero();
    }

    /**
     * Verifies that an item with {@code stock = 0} AND {@code isAvailable = false}
     * is excluded from the customer menu.
     *
     * @throws AssertionError if the disabled sold-out item appears in the result
     * Covers FR21
     */
    @Test
    void getAvailableItems_withZeroStockAndUnavailable_excludesItem() {
        Item disabledSoldOut = new Item();
        disabledSoldOut.setId(4L);
        disabledSoldOut.setName("Mocha");
        disabledSoldOut.setIsAvailable(false);
        disabledSoldOut.setStock(0);

        when(itemRepository.findAll()).thenReturn(Arrays.asList(availableItem, disabledSoldOut));

        List<Item> result = menuService.getAvailableItems();

        assertThat(result).hasSize(1);
        assertThat(result).doesNotContain(disabledSoldOut);
    }

    /**
     * Verifies that {@code initData()} seeds exactly 7 default menu items
     * when the repository is empty on first startup.
     *
     * @throws AssertionError if save is not called exactly 7 times
     * Covers FR2
     */
    @Test
    void initData_withEmptyRepository_savesSevenDefaultItems() {
        when(itemRepository.count()).thenReturn(0L);

        menuService.initData();

        verify(itemRepository, times(7)).save(any(Item.class));
    }

    /**
     * Verifies that {@code initData()} does not persist any items when the
     * repository already contains data.
     *
     * @throws AssertionError if save is called when the repository is non-empty
     * Covers FR2
     */
    @Test
    void initData_withExistingData_doesNotSeedAgain() {
        when(itemRepository.count()).thenReturn(7L);

        menuService.initData();

        verify(itemRepository, never()).save(any(Item.class));
    }

    /**
     * Verifies that {@code initData()} triggers seeding when count is exactly 0.
     * <p>
     * Boundary case: confirms the {@code count() == 0} condition.
     * </p>
     *
     * @throws AssertionError if save is not invoked when count is exactly 0
     * Covers FR2
     */
    @Test
    void initData_withCountExactlyZero_triggersSeeding() {
        when(itemRepository.count()).thenReturn(0L);

        menuService.initData();

        verify(itemRepository, atLeastOnce()).save(any(Item.class));
    }
}
