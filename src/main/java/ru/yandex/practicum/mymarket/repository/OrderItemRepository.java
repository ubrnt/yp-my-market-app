package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.yandex.practicum.mymarket.domain.OrderItem;

public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {
}
