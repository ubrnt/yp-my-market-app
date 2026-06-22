package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@SpringBootTest
@Transactional
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void search_filtersByTitle_excludingNonMatching() {
        String titleWord = "уникальныйтайтл";
        Long matchId = saveItem(titleWord, "обычное описание").getId();
        Long otherId = saveItem("Другой товар", "другое описание").getId();

        ItemsPageDto page = itemService.getItems(titleWord, SortType.NO, 1, 50);

        assertTrue(containsId(page, matchId));
        assertFalse(containsId(page, otherId));
    }

    @Test
    void getImage_returnsStoredBytes() {
        Item item = itemRepository.findAll().getFirst();

        byte[] image = itemService.getImage(item.getId());

        assertTrue(image != null && image.length > 0);
    }

    @Test
    void search_matchesByDescription() {
        String descriptionWord = "редкоесловоизописания";
        Long id = saveItem("Бейсболка", descriptionWord).getId();

        ItemsPageDto page = itemService.getItems(descriptionWord, SortType.NO, 1, 50);

        assertTrue(containsId(page, id));
    }

    @Test
    void search_isCaseInsensitive() {
        String descriptionWord = "редкоесловоизописания";
        Long id = saveItem("Бейсболка", descriptionWord).getId();

        ItemsPageDto page = itemService.getItems(descriptionWord.toUpperCase(), SortType.NO, 1, 50);

        assertTrue(containsId(page, id));
    }

    private Item saveItem(String title, String description) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setPrice(100L);
        return itemRepository.save(item);
    }

    private boolean containsId(ItemsPageDto page, Long id) {
        return page.items().stream()
                .flatMap(List::stream)
                .filter(dto -> !dto.isDummy())
                .anyMatch(dto -> id.equals(dto.id()));
    }
}
