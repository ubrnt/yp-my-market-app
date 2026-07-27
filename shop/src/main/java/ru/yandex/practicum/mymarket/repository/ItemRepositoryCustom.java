package ru.yandex.practicum.mymarket.repository;

import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;

public interface ItemRepositoryCustom {

    Flux<ItemCountRow> findPageIdsWithCount(Long userId, String search, SortType sort, int limit, long offset);
}
