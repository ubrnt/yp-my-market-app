package ru.yandex.practicum.mymarket;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.BalanceResult;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.PaymentResult;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@SpringBootTest
@AutoConfigureWebTestClient
public abstract class AbstractIntegrationTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected CartItemRepository cartItemRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected OrderItemRepository orderItemRepository;
    @Autowired
    protected ReactiveRedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    protected PaymentServiceClient paymentServiceClient;

    @BeforeEach
    void cleanMutableData() {
        StepVerifier.create(
                orderItemRepository.deleteAll()
                        .then(cartItemRepository.deleteAll())
                        .then(orderRepository.deleteAll())
        ).verifyComplete();
    }

    @BeforeEach
    void flushCache() {
        StepVerifier.create(redisConnectionFactory.getReactiveConnection().serverCommands().flushAll())
                .expectNextCount(1)
                .verifyComplete();
    }

    @BeforeEach
    void stubPaymentService() {
        when(paymentServiceClient.getBalance()).thenReturn(Mono.just(BalanceResult.available(1_000_000L)));
        when(paymentServiceClient.pay(anyLong())).thenReturn(Mono.just(PaymentResult.SUCCESS));
    }
}
