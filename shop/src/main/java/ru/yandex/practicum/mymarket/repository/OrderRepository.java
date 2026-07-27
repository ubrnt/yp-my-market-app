package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.repository.projection.OrderItemDetailedRow;

public interface OrderRepository extends R2dbcRepository<Order, Long> {

    @Query("""
            SELECT o.id AS order_id, o.total_sum, i.id AS item_id, i.title, i.price, oi.count
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN items i ON i.id = oi.item_id
            WHERE o.user_id = :userId
            ORDER BY o.id
            """)
    Flux<OrderItemDetailedRow> findAllWithItems(Long userId);

    @Query("""
            SELECT o.id AS order_id, o.total_sum, i.id AS item_id, i.title, i.price, oi.count
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN items i ON i.id = oi.item_id
            WHERE o.id = :id AND o.user_id = :userId
            ORDER BY o.id
            """)
    Flux<OrderItemDetailedRow> findByIdWithItems(Long id, Long userId);
}
