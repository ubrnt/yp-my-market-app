package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.yandex.practicum.mymarket.domain.Order;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
}
