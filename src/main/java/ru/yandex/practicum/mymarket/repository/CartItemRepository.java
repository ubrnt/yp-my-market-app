package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    @Query("""
            SELECT i.id, i.title, i.description, i.image_path, i.price, ci.count
            FROM cart_items ci
            JOIN items i ON i.id = ci.item_id
            ORDER BY i.id
            """)
    Flux<ItemDetailedRow> findAllWithItems();

    Mono<CartItem> findByItemId(Long itemId);

    Mono<Void> deleteByItemId(Long itemId);
}
