package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;

@DataR2dbcTest
class OrderItemRepositoryTest {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;

    @Test
    void orderItemSaveAndFindTest() {
        Order order = new Order();
        order.setTotalSum(500L);
        Order savedOrder = orderRepository.save(order).block();
        assertNotNull(savedOrder);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(savedOrder.getId());
        orderItem.setItemId(1L);
        orderItem.setCount(2);

        StepVerifier.create(orderItemRepository.save(orderItem).flatMap(saved -> orderItemRepository.findById(saved.getId())))
                .assertNext(found -> {
                    assertNotNull(found.getId());
                    assertEquals(savedOrder.getId(), found.getOrderId());
                    assertEquals(1L, found.getItemId());
                    assertEquals(2, found.getCount());
                })
                .verifyComplete();
    }
}
