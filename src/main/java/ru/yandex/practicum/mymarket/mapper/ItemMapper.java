package ru.yandex.practicum.mymarket.mapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

@Component
public class ItemMapper {

    private final String imagesBasePath;

    public ItemMapper(@Value("${app.images-base-path}") String imagesBasePath) {
        this.imagesBasePath = imagesBasePath;
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
}
