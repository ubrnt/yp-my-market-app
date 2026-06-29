package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;

class OrderRepositoryTest extends AbstractRepositoryTest {

    @Test
    void save_thenFindById_returnsOrder() {
        Order saved = saveOrder(500L);

        StepVerifier.create(orderRepository.findById(saved.getId()))
                .assertNext(found -> assertEquals(500L, found.getTotalSum()))
                .verifyComplete();
    }

    @Test
    void findAllWithItems_joinsItemsAcrossOrders() {
        Item item = saveItem("Мяч", "круглый", 990L, "ball.png");
        Order order = saveOrder(1980L);
        saveOrderItem(order.getId(), item.getId(), 2);

        StepVerifier.create(orderRepository.findAllWithItems())
                .assertNext(row -> {
                    assertEquals(order.getId(), row.orderId());
                    assertEquals(1980L, row.totalSum());
                    assertEquals(item.getId(), row.itemId());
                    assertEquals("Мяч", row.title());
                    assertEquals(990L, row.price());
                    assertEquals(2, row.count());
                })
                .verifyComplete();
    }

    @Test
    void findByIdWithItems_returnsOnlyThatOrder() {
        Item item = saveItem("Мяч", "круглый", 990L, "ball.png");
        Order firstOrder = saveOrder(990L);
        Order secondOrder = saveOrder(1980L);
        saveOrderItem(firstOrder.getId(), item.getId(), 1);
        saveOrderItem(secondOrder.getId(), item.getId(), 2);

        StepVerifier.create(orderRepository.findByIdWithItems(firstOrder.getId()))
                .assertNext(row -> {
                    assertEquals(firstOrder.getId(), row.orderId());
                    assertEquals(item.getId(), row.itemId());
                    assertEquals(1, row.count());
                })
                .verifyComplete();
    }
}
