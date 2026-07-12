package ru.yandex.practicum.mymarket.mapper;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

@Component
public class ItemMapper {

    private final String imagesUrlPrefix;
    private final int rowSize;

    public ItemMapper(@Value("${app.images.url-prefix}") String imagesUrlPrefix,
                      @Value("${app.items-page.row-size}") int rowSize) {
        this.imagesUrlPrefix = imagesUrlPrefix;
        this.rowSize = rowSize;
    }

    public ItemDto toDto(ItemDetailedRow row) {
        return new ItemDto(
                row.id(),
                row.title(),
                row.description(),
                imagesUrlPrefix + row.id(),
                row.price(),
                row.count());
    }

    public ItemDto toDto(Item item, int count) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                imagesUrlPrefix + item.getId(),
                item.getPrice(),
                count);
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
