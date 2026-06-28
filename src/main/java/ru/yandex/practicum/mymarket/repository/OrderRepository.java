package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.Column;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.domain.Order;

public interface OrderRepository extends R2dbcRepository<Order, Long> {

    @Query("""
            SELECT o.id AS order_id, o.total_sum, i.id AS item_id, i.title, i.price, oi.count
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN items i ON i.id = oi.item_id
            ORDER BY o.id
            """)
    Flux<OrderRow> findAllWithItems();

    @Query("""
            SELECT o.id AS order_id, o.total_sum, i.id AS item_id, i.title, i.price, oi.count
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN items i ON i.id = oi.item_id
            WHERE o.id = :id
            ORDER BY o.id
            """)
    Flux<OrderRow> findByIdWithItems(Long id);

    record OrderRow(
            @Column("order_id") Long orderId,
            @Column("total_sum") long totalSum,
            @Column("item_id") Long itemId,
            String title,
            long price,
            int count
    ) {
    }
}
