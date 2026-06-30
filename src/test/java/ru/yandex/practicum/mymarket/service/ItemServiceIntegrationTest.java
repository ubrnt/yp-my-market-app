package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.SortType;

class ItemServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ItemService itemService;

    @Test
    void getItem_returnsMappedItemAgainstRealDb() {
        Item firstItem = itemRepository.findAll().blockFirst();

        ItemDto dto = itemService.getItem(firstItem.getId()).block();

        assertEquals(firstItem.getId(), dto.id());
        assertEquals(firstItem.getTitle(), dto.title());
        assertEquals(firstItem.getPrice(), dto.price());
        assertEquals("images/" + firstItem.getId(), dto.imgPath());
    }

    @Test
    void getItems_buildsPagedRowsAgainstRealDb() {
        ItemsPageDto page = itemService.getItems(null, SortType.NO, 1, 5).block();

        assertFalse(page.items().isEmpty());
        page.items().forEach(row -> assertEquals(3, row.size()));
    }
}
