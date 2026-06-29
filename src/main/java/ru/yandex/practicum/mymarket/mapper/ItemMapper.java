package ru.yandex.practicum.mymarket.mapper;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

@Component
public class ItemMapper {

    private final String imagesBasePath;
    private final int rowSize;

    public ItemMapper(@Value("${app.images-base-path}") String imagesBasePath,
                      @Value("${app.items-page.row-size}") int rowSize) {
        this.imagesBasePath = imagesBasePath;
        this.rowSize = rowSize;
    }

    public ItemDto toDto(ItemDetailedRow row) {
        return new ItemDto(
                row.id(),
                row.title(),
                row.description(),
                imagesBasePath + row.id(),
                row.price(),
                row.count());
    }

    public ItemsPageDto toPageDto(List<ItemDto> items, PagingDto paging) {
        return new ItemsPageDto(toRows(items), paging);
    }

    private List<List<ItemDto>> toRows(List<ItemDto> items) {
        List<List<ItemDto>> rows = new ArrayList<>();

        for (int from = 0; from < items.size(); from += rowSize) {
            int to = Math.min(from + rowSize, items.size());
            List<ItemDto> row = new ArrayList<>(items.subList(from, to));
            while (row.size() < rowSize) {
                row.add(ItemDto.dummy());
            }
            rows.add(row);
        }

        return rows;
    }
}
