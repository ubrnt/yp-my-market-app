package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void buy_persistsOrderFromCart_andClearsCart() {
        List<Item> items = itemRepository.findAll();
        Item a = items.get(0);
        Item b = items.get(1);
        cartService.changeCount(a.getId(), Action.PLUS);
        cartService.changeCount(a.getId(), Action.PLUS);
        cartService.changeCount(b.getId(), Action.PLUS);

        Long orderId = orderService.buy();

        assertTrue(cartService.getCartItems().isEmpty());

        OrderDto order = orderService.getOrder(orderId);
        assertEquals(a.getPrice() * 2 + b.getPrice(), order.totalSum());
        assertEquals(2, order.items().size());
    }
}
