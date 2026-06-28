package ru.yandex.practicum.mymarket.repository;

import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.dto.SortType;

public interface ItemRepositoryCustom {

    Flux<ItemRow> findItemsForPage(String search, SortType sort, int limit, long offset);

    record ItemRow(
            Long id,
            String title,
            String description,
            String imagePath,
            long price,
            int count
    ) {
    }
}
