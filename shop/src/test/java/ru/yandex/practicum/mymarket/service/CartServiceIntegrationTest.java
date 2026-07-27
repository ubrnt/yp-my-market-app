package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;

class CartServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CartService cartService;

    @Test
    void changeCount_reflectsInCartAgainstRealDb() {
        List<Item> items = itemRepository.findAll().collectList().block();
        Item first = items.get(0);
        Item second = items.get(1);

        StepVerifier.create(
                cartService.changeCount(1L, first.getId(), Action.PLUS)
                        .then(cartService.changeCount(1L, first.getId(), Action.PLUS))
                        .then(cartService.changeCount(1L, second.getId(), Action.PLUS))
                        .then(cartService.getCart(1L))
        ).assertNext(cart -> {
            assertEquals(2, cart.items().size());
            assertEquals(first.getPrice() * 2 + second.getPrice(), cart.total());
        }).verifyComplete();
    }
}
