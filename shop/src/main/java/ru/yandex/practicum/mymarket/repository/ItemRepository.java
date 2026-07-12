package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.Item;

public interface ItemRepository extends R2dbcRepository<Item, Long>, ItemRepositoryCustom {

    @Query("SELECT count FROM cart_items WHERE item_id = :id")
    Mono<Integer> countInCart(Long id);
}
