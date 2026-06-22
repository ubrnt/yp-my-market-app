package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@SpringBootTest
@Transactional
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void plus_createsCartItemWithCountOne_thenIncrements() {
        Long id = anyItem().getId();

        cartService.changeCount(id, Action.PLUS);
        assertEquals(1, cartService.getCountByItemId().get(id));

        cartService.changeCount(id, Action.PLUS);
        assertEquals(2, cartService.getCountByItemId().get(id));
    }

    @Test
    void minus_decrements_andRemovesAtZero() {
        Long id = anyItem().getId();
        cartService.changeCount(id, Action.PLUS);
        cartService.changeCount(id, Action.PLUS);

        cartService.changeCount(id, Action.MINUS);
        assertEquals(1, cartService.getCountByItemId().get(id));

        cartService.changeCount(id, Action.MINUS);
        assertFalse(cartService.getCountByItemId().containsKey(id));
    }

    @Test
    void delete_removesItemRegardlessOfCount() {
        Long id = anyItem().getId();
        cartService.changeCount(id, Action.PLUS);
        cartService.changeCount(id, Action.PLUS);

        cartService.changeCount(id, Action.DELETE);

        assertTrue(cartService.getCartItems().isEmpty());
    }

    @Test
    void getTotal_sumsPriceTimesCount() {
        List<Item> items = itemRepository.findAll();
        Item a = items.get(0);
        Item b = items.get(1);

        cartService.changeCount(a.getId(), Action.PLUS);
        cartService.changeCount(a.getId(), Action.PLUS);
        cartService.changeCount(b.getId(), Action.PLUS);

        long expected = a.getPrice() * 2 + b.getPrice();
        assertEquals(expected, cartService.getTotal());
    }

    private Item anyItem() {
        return itemRepository.findAll().get(0);
    }
}
