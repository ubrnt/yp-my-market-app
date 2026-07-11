package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@SpringBootTest
@AutoConfigureWebTestClient
public abstract class AbstractIntegrationTest {

    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected CartItemRepository cartItemRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected OrderItemRepository orderItemRepository;

    @BeforeEach
    void cleanMutableData() {
        StepVerifier.create(
                orderItemRepository.deleteAll()
                        .then(cartItemRepository.deleteAll())
                        .then(orderRepository.deleteAll())
        ).verifyComplete();
    }
}
