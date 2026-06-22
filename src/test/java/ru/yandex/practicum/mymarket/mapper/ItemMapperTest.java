package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;

class ItemMapperTest {

    private final ItemMapper mapper = new ItemMapper();

    @Test
    void toDto_mapsAllFieldsAndBuildsImgPath() {
        Item item = new Item();
        item.setId(7L);
        item.setTitle("Бейсболка чёрная");
        item.setDescription("Хлопковая кепка");
        item.setPrice(990L);

        ItemDto dto = mapper.toDto(item, 3);

        assertEquals(7L, dto.id());
        assertEquals("Бейсболка чёрная", dto.title());
        assertEquals("Хлопковая кепка", dto.description());
        assertEquals("images/7", dto.imgPath());
        assertEquals(990L, dto.price());
        assertEquals(3, dto.count());
    }

    @Test
    void toDto_zeroCount_whenNotInCart() {
        Item item = new Item();
        item.setId(1L);
        item.setTitle("Мяч");
        item.setPrice(1990L);

        ItemDto dto = mapper.toDto(item, 0);

        assertEquals(0, dto.count());
        assertEquals("images/1", dto.imgPath());
    }
}
