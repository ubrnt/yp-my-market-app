package ru.yandex.practicum.mymarket.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class RedisItemProviderUnitTest {

    private static final String PREFIX = "my-market-app";
    private static final Duration TTL = Duration.ofSeconds(60);

    @Mock
    ReactiveRedisTemplate<String, Item> redis;

    @Mock
    ReactiveValueOperations<String, Item> valueOps;

    @Mock
    ItemRepository itemRepository;

    RedisItemProvider itemProvider;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        itemProvider = new RedisItemProvider(redis, itemRepository, PREFIX, TTL);
    }

    @Test
    void get_whenCached_returnsFromRedisWithoutTouchingDb() {
        when(valueOps.get(key(1))).thenReturn(Mono.just(item(1)));

        StepVerifier.create(itemProvider.get(1))
                .assertNext(item -> assertEquals(1L, item.getId()))
                .verifyComplete();

        verify(itemRepository, never()).findById(any(Long.class));
    }

    @Test
    void get_whenMiss_loadsFromDbAndStores() {
        when(valueOps.get(key(1))).thenReturn(Mono.empty());
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1)));
        when(valueOps.set(eq(key(1)), any(Item.class), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(itemProvider.get(1))
                .assertNext(item -> assertEquals(1L, item.getId()))
                .verifyComplete();

        verify(valueOps).set(eq(key(1)), any(Item.class), eq(TTL));
    }

    @Test
    void get_whenRedisFails_fallsBackToDb() {
        when(valueOps.get(key(1))).thenReturn(Mono.error(new RuntimeException("redis down")));
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item(1)));

        StepVerifier.create(itemProvider.get(1))
                .assertNext(item -> assertEquals(1L, item.getId()))
                .verifyComplete();
    }

    @Test
    void getAll_mergesCachedAndLoaded_keepsIdsOrder() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(valueOps.multiGet(List.of(key(1), key(2), key(3))))
                .thenReturn(Mono.just(Arrays.asList(item(1), null, item(3))));
        when(itemRepository.findAllById(List.of(2L))).thenReturn(Flux.just(item(2)));
        when(valueOps.set(eq(key(2)), any(Item.class), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(itemProvider.getAll(ids))
                .assertNext(item -> assertEquals(1L, item.getId()))
                .assertNext(item -> assertEquals(2L, item.getId()))
                .assertNext(item -> assertEquals(3L, item.getId()))
                .verifyComplete();
    }

    @Test
    void getAll_whenRedisFails_fallsBackToDb_kepsIdsOrder() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(valueOps.multiGet(List.of(key(1), key(2), key(3))))
                .thenReturn(Mono.error(new RuntimeException("redis down")));
        when(itemRepository.findAllById(ids)).thenReturn(Flux.just(item(3), item(1), item(2)));

        StepVerifier.create(itemProvider.getAll(ids))
                .assertNext(item -> assertEquals(1L, item.getId()))
                .assertNext(item -> assertEquals(2L, item.getId()))
                .assertNext(item -> assertEquals(3L, item.getId()))
                .verifyComplete();
    }

    private static String key(long id) {
        return PREFIX + ":item:" + id;
    }

    private static Item item(long id) {
        Item item = new Item();
        item.setId(id);
        item.setTitle("Item " + id);
        item.setPrice(100L * id);
        return item;
    }
}
