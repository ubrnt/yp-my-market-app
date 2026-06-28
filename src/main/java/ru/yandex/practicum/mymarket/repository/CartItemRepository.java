package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.domain.CartItem;

public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {

    @Query("""
            SELECT i.id, i.title, i.description, i.image_path, i.price, ci.count
            FROM cart_items ci
            JOIN items i ON i.id = ci.item_id
            ORDER BY i.id
            """)
    Flux<ItemRow> findAllWithItems();

    record ItemRow(
            Long id,
            String title,
            String description,
            @Column("image_path") String imagePath,
            long price,
            int count
    ) {
    }
}
