package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;

class ItemRepositoryTest extends AbstractRepositoryTest {

    @Test
    void save_thenFindById_returnsItem() {
        Item saved = saveItem("Тестовый товар", "Описание", 1234L, "test.png");

        StepVerifier.create(itemRepository.findById(saved.getId()))
                .assertNext(found -> {
                    assertEquals("Тестовый товар", found.getTitle());
                    assertEquals("Описание", found.getDescription());
                    assertEquals(1234L, found.getPrice());
                    assertEquals("test.png", found.getImagePath());
                })
                .verifyComplete();
    }

    @Test
    void findPageIdsWithCount_sortByPrice_ordersAscending() {
        Item c = saveItem("В", "", 300L, "c.png");
        Item a = saveItem("А", "", 100L, "a.png");
        Item b = saveItem("Б", "", 200L, "b.png");

        StepVerifier.create(itemRepository.findPageIdsWithCount(testUserId, null, SortType.PRICE, 10, 0).collectList())
                .assertNext(rows -> {
                    assertEquals(3, rows.size());
                    assertEquals(a.getId(), rows.get(0).id());
                    assertEquals(b.getId(), rows.get(1).id());
                    assertEquals(c.getId(), rows.get(2).id());
                })
                .verifyComplete();
    }

    @Test
    void findPageIdsWithCount_withSearch_filtersByTitleOrDescription() {
        Item ball = saveItem("Мяч футбольный", "круглый", 100L, "1.png");
        Item souvenir = saveItem("Сувенир", "внутри маленький мяч", 50L, "2.png");
        Item racket = saveItem("Ракетка", "для тенниса", 200L, "3.png");

        StepVerifier.create(itemRepository.findPageIdsWithCount(testUserId, "мяч", SortType.NO, 10, 0).collectList())
                .assertNext(rows -> {
                    assertEquals(2, rows.size());
                    Set<Long> ids = rows.stream().map(ItemCountRow::id).collect(Collectors.toSet());
                    assertTrue(ids.contains(ball.getId()));
                    assertTrue(ids.contains(souvenir.getId()));
                    assertFalse(ids.contains(racket.getId()));
                })
                .verifyComplete();
    }

    @Test
    void countInCart_returnsCartCount() {
        Item inCart = saveItem("В корзине", "опис", 100L, "a.png");
        Item notInCart = saveItem("Не в корзине", "опис", 200L, "b.png");

        saveCartItem(inCart.getId(), 4);

        StepVerifier.create(itemRepository.countInCart(inCart.getId(), testUserId))
                .assertNext(count -> assertEquals(4, count))
                .verifyComplete();

        StepVerifier.create(itemRepository.countInCart(notInCart.getId(), testUserId))
                .verifyComplete();
    }

    @Test
    void findPageIdsWithCount_setsCountFromCart() {
        Item inCart = saveItem("В корзине", "", 100L, "a.png");
        Item notInCart = saveItem("Не в корзине", "", 200L, "b.png");
        saveCartItem(inCart.getId(), 2);

        StepVerifier.create(itemRepository.findPageIdsWithCount(testUserId, null, SortType.NO, 10, 0).collectList())
                .assertNext(rows -> {
                    assertEquals(2, rows.size());
                    assertEquals(inCart.getId(), rows.get(0).id());
                    assertEquals(2, rows.get(0).count());
                    assertEquals(notInCart.getId(), rows.get(1).id());
                    assertEquals(0, rows.get(1).count());
                })
                .verifyComplete();
    }
}
