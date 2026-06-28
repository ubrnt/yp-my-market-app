package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;

@DataR2dbcTest
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;

    @BeforeEach
    void clearOrders() {
        orderItemRepository.deleteAll()
                .then(orderRepository.deleteAll())
                .block();
    }

    @Test
    void orderSaveAndFindTest() {
        Order order = new Order();
        order.setTotalSum(500L);

        StepVerifier.create(orderRepository.save(order).flatMap(saved -> orderRepository.findById(saved.getId())))
                .assertNext(found -> {
                    assertNotNull(found.getId());
                    assertEquals(500L, found.getTotalSum());
                })
                .verifyComplete();
    }

    @Test
    void findAllWithItemsTest() {
        Order order = new Order();
        order.setTotalSum(1980L);
        Order savedOrder = orderRepository.save(order).block();

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(savedOrder.getId());
        orderItem.setItemId(1L);
        orderItem.setCount(2);

        StepVerifier.create(orderItemRepository.save(orderItem).thenMany(orderRepository.findAllWithItems()))
                .assertNext(row -> {
                    assertEquals(savedOrder.getId(), row.orderId());
                    assertEquals(1980L, row.totalSum());
                    assertEquals(1L, row.itemId());
                    assertEquals("Бейсболка чёрная", row.title());
                    assertEquals(990L, row.price());
                    assertEquals(2, row.count());
                })
                .verifyComplete();
    }

    @Test
    void findByIdWithItemsTest() {
        Order firstOrder = orderRepository.save(newOrder(990L)).block();
        Order secondOrder = orderRepository.save(newOrder(1980L)).block();

        OrderItem firstItem = newOrderItem(firstOrder.getId(), 1L, 1);
        OrderItem secondItem = newOrderItem(secondOrder.getId(), 2L, 2);

        StepVerifier.create(orderItemRepository.save(firstItem)
                        .then(orderItemRepository.save(secondItem))
                        .thenMany(orderRepository.findByIdWithItems(firstOrder.getId())))
                .assertNext(row -> {
                    assertEquals(firstOrder.getId(), row.orderId());
                    assertEquals(1L, row.itemId());
                    assertEquals(1, row.count());
                })
                .verifyComplete();
    }

    private static Order newOrder(long totalSum) {
        Order order = new Order();
        order.setTotalSum(totalSum);
        return order;
    }

    private static OrderItem newOrderItem(Long orderId, Long itemId, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setItemId(itemId);
        orderItem.setCount(count);
        return orderItem;
    }
}
