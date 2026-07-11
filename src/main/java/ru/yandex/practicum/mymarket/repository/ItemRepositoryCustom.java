package ru.yandex.practicum.mymarket.repository;

import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

public interface ItemRepositoryCustom {

    Flux<ItemDetailedRow> findForPage(String search, SortType sort, int limit, long offset);
}
