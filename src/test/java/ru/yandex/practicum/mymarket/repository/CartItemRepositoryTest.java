package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;

class CartItemRepositoryTest extends AbstractRepositoryTest {

    @Test
    void cartItemSaveAndFindTest() {
        Item item = saveItem("Товар", "Описание", 100L, "t.png");
        CartItem saved = saveCartItem(item.getId(), 3);

        StepVerifier.create(cartItemRepository.findById(saved.getId()))
                .assertNext(found -> {
                    assertEquals(item.getId(), found.getItemId());
                    assertEquals(3, found.getCount());
                })
                .verifyComplete();
    }

    @Test
    void findAllWithItemsTest() {
        Item item = saveItem("Мяч", "круглый", 990L, "ball.png");
        saveCartItem(item.getId(), 3);

        StepVerifier.create(cartItemRepository.findAllWithItems())
                .assertNext(row -> {
                    assertEquals(item.getId(), row.id());
                    assertEquals("Мяч", row.title());
                    assertEquals("круглый", row.description());
                    assertEquals("ball.png", row.imagePath());
                    assertEquals(990L, row.price());
                    assertEquals(3, row.count());
                })
                .verifyComplete();
    }
}
