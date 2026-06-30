package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartDto;

class CartServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CartService cartService;

    @Test
    void changeCount_reflectsInCartAgainstRealDb() {
        List<Item> items = itemRepository.findAll().collectList().block();
        Item first = items.get(0);
        Item second = items.get(1);

        cartService.changeCount(first.getId(), Action.PLUS).block();
        cartService.changeCount(first.getId(), Action.PLUS).block();
        cartService.changeCount(second.getId(), Action.PLUS).block();

        CartDto cart = cartService.getCart().block();

        assertEquals(2, cart.items().size());
        assertEquals(first.getPrice() * 2 + second.getPrice(), cart.total());
    }
}
