package ru.yandex.practicum.mymarket.cache;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Component
public class RedisItemProvider {

    private static final Logger log = LoggerFactory.getLogger(RedisItemProvider.class);

    private final ReactiveRedisTemplate<String, Item> redis;
    private final ItemRepository itemRepository;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisItemProvider(ReactiveRedisTemplate<String, Item> itemRedisTemplate,
                             ItemRepository itemRepository,
                             @Value("${spring.application.name}") String keyPrefix,
                             @Value("${app.cache.items.ttl}") Duration ttl) {
        this.redis = itemRedisTemplate;
        this.itemRepository = itemRepository;
        this.keyPrefix = keyPrefix;
        this.ttl = ttl;
    }

    public Mono<Item> get(long id) {
        return redis.opsForValue().get(key(id))
                .switchIfEmpty(Mono.defer(() -> itemRepository.findById(id).flatMap(this::put)))
                .onErrorResume(e -> {
                    log.warn("Redis unavailable, serving item {} from DB", id, e);
                    return itemRepository.findById(id);
                });
    }

    public Flux<Item> getAll(List<Long> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }

        List<String> keys = ids.stream().map(this::key).toList();

        return redis.opsForValue().multiGet(keys)
                .flatMapMany(cached -> assemble(ids, cached))
                .onErrorResume(e -> {
                    log.warn("Redis unavailable, serving {} items from DB", ids.size(), e);
                    return fromDb(ids);
                });
    }

    private Flux<Item> assemble(List<Long> ids, List<Item> cached) {
        Map<Long, Item> byId = new ConcurrentHashMap<>();
        List<Long> missed = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            Item item = cached.get(i);
            if (item != null) {
                byId.put(ids.get(i), item);
            } else {
                missed.add(ids.get(i));
            }
        }

        Mono<Void> loadMissing = missed.isEmpty()
                ? Mono.empty()
                : itemRepository.findAllById(missed)
                        .flatMap(this::put)
                        .doOnNext(item -> byId.put(item.getId(), item))
                        .then();

        return loadMissing.thenMany(Flux.fromIterable(ids).mapNotNull(byId::get));
    }

    private Flux<Item> fromDb(List<Long> ids) {
        return itemRepository.findAllById(ids)
                .collectMap(Item::getId)
                .flatMapMany(byId -> Flux.fromIterable(ids).mapNotNull(byId::get));
    }

    private Mono<Item> put(Item item) {
        return redis.opsForValue().set(key(item.getId()), item, ttl).thenReturn(item);
    }

    private String key(long id) {
        return keyPrefix + ":item:" + id;
    }
}
