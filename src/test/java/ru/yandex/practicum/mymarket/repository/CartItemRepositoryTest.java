package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.CartItem;

@DataR2dbcTest
class CartItemRepositoryTest {

    @Autowired
    CartItemRepository cartItemRepository;

    @BeforeEach
    void clearCart() {
        cartItemRepository.deleteAll().block();
    }

    @Test
    void cartItemSaveAndFindTest() {
        CartItem cartItem = new CartItem();
        cartItem.setItemId(1L);
        cartItem.setCount(3);

        Mono<CartItem> saved = cartItemRepository.save(cartItem);
        StepVerifier.create(saved.flatMap(it -> cartItemRepository.findById(it.getId())))
                .assertNext(found -> {
                    assertNotNull(found.getId());
                    assertEquals(1L, found.getItemId());
                    assertEquals(3, found.getCount());
                })
                .verifyComplete();
    }

    @Test
    void findAllWithItemsTest() {
        CartItem cartItem = new CartItem();
        cartItem.setItemId(1L);
        cartItem.setCount(3);

        StepVerifier.create(cartItemRepository.save(cartItem).thenMany(cartItemRepository.findAllWithItems()))
                .assertNext(row -> {
                    assertEquals(1L, row.id());
                    assertEquals("Бейсболка чёрная", row.title());
                    assertEquals("black-cap.png", row.imagePath());
                    assertEquals(990L, row.price());
                    assertEquals(3, row.count());
                })
                .verifyComplete();
    }
}
