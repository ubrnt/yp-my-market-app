package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

class ItemMapperTest {

    private final ItemMapper itemMapper = new ItemMapper("images/");

    @Test
    void toDtoTest() {
        ItemDetailedRow row = new ItemDetailedRow(5L, "Мяч", "круглый", "ball.png", 990L, 3);

        ItemDto dto = itemMapper.toDto(row);

        assertEquals(5L, dto.id());
        assertEquals("Мяч", dto.title());
        assertEquals("круглый", dto.description());
        assertEquals("images/5", dto.imgPath());
        assertEquals(990L, dto.price());
        assertEquals(3, dto.count());
    }
}
