package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.yandex.practicum.mymarket.domain.CartItem;

public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {
}
