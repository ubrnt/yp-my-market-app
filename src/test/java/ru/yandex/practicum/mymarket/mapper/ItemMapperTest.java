package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

class ItemMapperTest {

    private final ItemMapper itemMapper = new ItemMapper("images/", 3);

    @Test
    void toDto_mapsAllFieldsAndBuildsImgPath() {
        ItemDetailedRow row = new ItemDetailedRow(5L, "Мяч", "круглый", "ball.png", 990L, 3);

        ItemDto dto = itemMapper.toDto(row);

        assertEquals(5L, dto.id());
        assertEquals("Мяч", dto.title());
        assertEquals("круглый", dto.description());
        assertEquals("images/5", dto.imgPath());
        assertEquals(990L, dto.price());
        assertEquals(3, dto.count());
    }

    @Test
    void toPageDto_groupsByRowSize_andPadsLastRow() {
        List<ItemDto> items = List.of(
                new ItemDto(1L, "A", "", "images/1", 100L, 0),
                new ItemDto(2L, "B", "", "images/2", 200L, 0),
                new ItemDto(3L, "C", "", "images/3", 300L, 0),
                new ItemDto(4L, "D", "", "images/4", 400L, 0));
        PagingDto paging = new PagingDto(5, 1, false, false);

        ItemsPageDto page = itemMapper.toPageDto(items, paging);

        assertEquals(2, page.items().size());
        assertEquals(3, page.items().get(0).size());
        assertFalse(page.items().get(0).get(0).isDummy());

        assertEquals(3, page.items().get(1).size());
        assertEquals(4L, page.items().get(1).get(0).id());
        assertTrue(page.items().get(1).get(1).isDummy());
        assertTrue(page.items().get(1).get(2).isDummy());

        assertEquals(paging, page.paging());
    }
}
