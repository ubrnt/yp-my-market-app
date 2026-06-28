package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;

class OrderItemRepositoryTest extends AbstractRepositoryTest {

    @Test
    void orderItemSaveAndFindTest() {
        Item item = saveItem("Товар", "Описание", 100L, "t.png");
        Order order = saveOrder(200L);
        OrderItem saved = saveOrderItem(order.getId(), item.getId(), 2);

        StepVerifier.create(orderItemRepository.findById(saved.getId()))
                .assertNext(found -> {
                    assertEquals(order.getId(), found.getOrderId());
                    assertEquals(item.getId(), found.getItemId());
                    assertEquals(2, found.getCount());
                })
                .verifyComplete();
    }
}
