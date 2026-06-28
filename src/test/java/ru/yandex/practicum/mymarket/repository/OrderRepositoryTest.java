package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Order;

@DataR2dbcTest
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

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
}
