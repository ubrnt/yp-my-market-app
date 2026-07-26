package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record ItemsPageDto(
        List<List<ItemDto>> items,
        PagingDto paging
) {

    public record PagingDto(
            int pageSize,
            int pageNumber,
            boolean hasPrevious,
            boolean hasNext
    ) {
    }
}
