package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;

class ItemServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ItemService itemService;

    @Test
    void getItem_returnsMappedItemAgainstRealDb() {
        Item first = itemRepository.findAll().blockFirst();

        ItemDto dto = itemService.getItem(first.getId()).block();

        assertEquals(first.getId(), dto.id());
        assertEquals(first.getTitle(), dto.title());
        assertEquals(first.getPrice(), dto.price());
        assertEquals("images/" + first.getId(), dto.imgPath());
    }

    @Test
    void getItems_buildsPagedRowsAgainstRealDb() {
        StepVerifier.create(itemService.getItems(null, SortType.NO, 1, 5))
                .assertNext(page -> {
                    assertFalse(page.items().isEmpty());
                    page.items().forEach(row -> assertEquals(3, row.size()));
                })
                .verifyComplete();
    }
}
