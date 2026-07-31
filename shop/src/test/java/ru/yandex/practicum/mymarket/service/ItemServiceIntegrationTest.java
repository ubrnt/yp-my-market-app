package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;

class ItemServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ItemService itemService;

    @Test
    void getItem_returnsMappedItemAgainstRealDb() {
        Item first = itemRepository.findAll().blockFirst();

        ItemDto dto = itemService.getItem(first.getId(), 1L).block();

        assertEquals(first.getId(), dto.id());
        assertEquals(first.getTitle(), dto.title());
        assertEquals(first.getPrice(), dto.price());
        assertEquals("images/" + first.getId(), dto.imgPath());
    }

    @Test
    void getItems_anonymous_showsItemsWithZeroCountsAgainstRealDb() {
        Item first = itemRepository.findAll().blockFirst();
        CartItem cartItem = new CartItem();
        cartItem.setUserId(1L);
        cartItem.setItemId(first.getId());
        cartItem.setCount(3);
        cartItemRepository.save(cartItem).block();

        StepVerifier.create(itemService.getItemsAnonymous( null, SortType.NO, 1, 5))
                .assertNext(page -> {
                    assertFalse(page.items().isEmpty());
                    page.items().stream()
                            .flatMap(List::stream)
                            .filter(item -> !item.isDummy())
                            .forEach(item -> assertEquals(0, item.count()));
                })
                .verifyComplete();
    }

    @Test
    void getItems_buildsPagedRowsAgainstRealDb() {
        StepVerifier.create(itemService.getItems(1L, null, SortType.NO, 1, 5))
                .assertNext(page -> {
                    assertFalse(page.items().isEmpty());
                    page.items().forEach(row -> assertEquals(3, row.size()));
                })
                .verifyComplete();
    }
}
