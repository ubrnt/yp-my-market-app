package ru.yandex.practicum.mymarket.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;

class RedisItemProviderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RedisItemProvider itemProvider;

    @Autowired
    ReactiveRedisTemplate<String, Item> itemRedisTemplate;

    @Value("${spring.application.name}")
    String appName;

    @Test
    void get_onCacheMiss_loadsFromDbAndStoresInRedis() {
        Mono<Item> cachedAfterGet = itemRepository.findAll().next().flatMap(item -> {
            String key = appName + ":item:" + item.getId();
            return itemProvider.get(item.getId())
                    .then(itemRedisTemplate.opsForValue().get(key));
        });

        StepVerifier.create(cachedAfterGet)
                .assertNext(cached -> {
                    assertNotNull(cached);
                    assertNotNull(cached.getTitle());
                })
                .verifyComplete();
    }

    @Test
    void get_onCacheHit_isServedFromRedisNotDb() {
        Mono<Item> fromProvider = itemRepository.findAll().next().flatMap(item -> {
            String key = appName + ":item:" + item.getId();
            return itemRedisTemplate.opsForValue().set(key, cached(item.getId()))
                    .then(itemProvider.get(item.getId()));
        });

        StepVerifier.create(fromProvider)
                .assertNext(item -> assertEquals("CACHED", item.getTitle()))
                .verifyComplete();
    }

    @Test
    void getAll_keepsIdsOrder_servingCachedAndLoadingMissing() {
        Mono<Void> flow = itemRepository.findAll().collectList().flatMap(items -> {
            List<Long> ids = items.subList(0, 3).stream().map(Item::getId).toList();
            String firstKey = appName + ":item:" + ids.getFirst();
            return itemRedisTemplate.opsForValue().set(firstKey, cached(ids.getFirst()))
                    .then(itemProvider.getAll(ids).collectList())
                    .doOnNext(result -> {
                        assertEquals(ids, result.stream().map(Item::getId).toList());
                        assertEquals("CACHED", result.getFirst().getTitle());
                    })
                    .then();
        });

        StepVerifier.create(flow).verifyComplete();
    }

    @Test
    void getAll_whenNothingCached_loadsAllFromDb() {
        Flux<Item> fromProvider = itemRepository.findAll().collectList().flatMapMany(items -> {
            List<Long> ids = items.subList(0, 3).stream().map(Item::getId).toList();
            return itemProvider.getAll(ids);
        });

        StepVerifier.create(fromProvider)
                .expectNextCount(3)
                .verifyComplete();
    }

    private static Item cached(Long id) {
        Item item = new Item();
        item.setId(id);
        item.setTitle("CACHED");
        item.setDescription("from cache");
        item.setImagePath("cached.png");
        item.setPrice(1L);
        return item;
    }
}
