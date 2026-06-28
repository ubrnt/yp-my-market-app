package ru.yandex.practicum.mymarket.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;

@Component
public class ItemMapper {

    public ItemDto toDto(Item item, int count) {
        return new ItemDto(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                "images/" + item.getId(),
                item.getPrice(),
                count
        );
    }
}
