package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CartService cartService;
    @Autowired
    OrderService orderService;

    @Test
    void buy_createsOrderAndClearsCartAgainstRealDb() {
        Item first = itemRepository.findAll().blockFirst();

        StepVerifier.create(
                cartService.changeCount(1L, first.getId(), Action.PLUS)
                        .then(cartService.changeCount(1L, first.getId(), Action.PLUS))
                        .then(orderService.buy(1L))
                        .flatMap(orderService::getOrder)
        ).assertNext(order -> {
            assertEquals(first.getPrice() * 2, order.totalSum());
            assertEquals(1, order.items().size());
            assertEquals(2, order.items().getFirst().count());
        }).verifyComplete();

        StepVerifier.create(cartService.getCart(1L))
                .assertNext(cart -> assertTrue(cart.items().isEmpty()))
                .verifyComplete();
    }
}
