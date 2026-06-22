package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@SpringBootTest
@Transactional
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void changeCount_thenCartReflectsItAgainstRealDb() {
        List<Item> items = itemRepository.findAll();
        Item a = items.get(0);
        Item b = items.get(1);

        cartService.changeCount(a.getId(), Action.PLUS);
        cartService.changeCount(a.getId(), Action.PLUS);
        cartService.changeCount(b.getId(), Action.PLUS);

        List<ItemDto> cart = cartService.getCartItems();
        assertEquals(2, cart.size());
        assertEquals(a.getPrice() * 2 + b.getPrice(), cartService.getTotal());
    }
}
