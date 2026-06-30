package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.OrderDto;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CartService cartService;
    @Autowired
    OrderService orderService;

    @Test
    void buy_createsOrderAndClearsCartAgainstRealDb() {
        List<Item> items = itemRepository.findAll().collectList().block();
        Item first = items.get(0);

        cartService.changeCount(first.getId(), Action.PLUS).block();
        cartService.changeCount(first.getId(), Action.PLUS).block();

        Long orderId = orderService.buy().block();
        OrderDto order = orderService.getOrder(orderId).block();

        assertEquals(orderId, order.id());
        assertEquals(first.getPrice() * 2, order.totalSum());
        assertEquals(1, order.items().size());
        assertEquals(2, order.items().get(0).count());

        assertEquals(0, cartService.getCart().block().items().size());
    }
}
