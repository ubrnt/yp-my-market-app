package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.SortType;

class ItemRepositoryTest extends AbstractRepositoryTest {

    @Test
    void itemSaveAndFindTest() {
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
    void findItemsForPageSortsByPriceTest() {
        saveItem("В", "", 300L, "c.png");
        saveItem("А", "", 100L, "a.png");
        saveItem("Б", "", 200L, "b.png");

        var rows = itemRepository.findForPage(null, SortType.PRICE, 10, 0).collectList().block();

        assertEquals(3, rows.size());
        assertEquals(100L, rows.get(0).price());
        assertEquals(200L, rows.get(1).price());
        assertEquals(300L, rows.get(2).price());
    }

    @Test
    void findItemsForPageFiltersBySearchTest() {
        saveItem("Мяч футбольный", "круглый", 100L, "1.png");
        saveItem("Сувенир", "внутри маленький мяч", 50L, "2.png");
        saveItem("Ракетка", "для тенниса", 200L, "3.png");

        var rows = itemRepository.findForPage("мяч", SortType.NO, 10, 0).collectList().block();

        assertEquals(2, rows.size());
        rows.forEach(row -> assertTrue(
                row.title().toLowerCase().contains("мяч") || row.description().toLowerCase().contains("мяч")));
    }

    @Test
    void findByIdWithCountInCartTest() {
        Item inCart = saveItem("В корзине", "опис", 100L, "a.png");
        Item notInCart = saveItem("Не в корзине", "опис", 200L, "b.png");
        saveCartItem(inCart.getId(), 4);

        StepVerifier.create(itemRepository.findByIdWithCountInCart(inCart.getId()))
                .assertNext(row -> {
                    assertEquals(inCart.getId(), row.id());
                    assertEquals("В корзине", row.title());
                    assertEquals("a.png", row.imagePath());
                    assertEquals(100L, row.price());
                    assertEquals(4, row.count());
                })
                .verifyComplete();

        StepVerifier.create(itemRepository.findByIdWithCountInCart(notInCart.getId()))
                .assertNext(row -> assertEquals(0, row.count()))
                .verifyComplete();
    }

    @Test
    void findItemsForPageReflectsCartCountTest() {
        Item inCart = saveItem("В корзине", "", 100L, "a.png");
        Item notInCart = saveItem("Не в корзине", "", 200L, "b.png");
        saveCartItem(inCart.getId(), 2);

        var rows = itemRepository.findForPage(null, SortType.NO, 10, 0).collectList().block();

        assertEquals(2, rows.size());
        assertEquals(inCart.getId(), rows.get(0).id());
        assertEquals(2, rows.get(0).count());
        assertEquals(notInCart.getId(), rows.get(1).id());
        assertEquals(0, rows.get(1).count());
    }
}
