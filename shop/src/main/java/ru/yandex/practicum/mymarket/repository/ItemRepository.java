package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

public interface ItemRepository extends R2dbcRepository<Item, Long>, ItemRepositoryCustom {

    @Query("""
            SELECT i.id, i.title, i.description, i.image_path, i.price, COALESCE(ci.count, 0) AS count
            FROM items i
            LEFT JOIN cart_items ci ON ci.item_id = i.id
            WHERE i.id = :id
            """)
    Mono<ItemDetailedRow> findByIdWithCountInCart(Long id);
}
