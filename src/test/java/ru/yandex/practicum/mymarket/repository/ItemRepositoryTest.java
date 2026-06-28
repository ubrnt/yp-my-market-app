package ru.yandex.practicum.mymarket.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.Item;

@DataR2dbcTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    @Test
    void seededItemMapingTest() {
        StepVerifier.create(itemRepository.findById(1L))
                .assertNext(item -> {
                    assertEquals("Бейсболка чёрная", item.getTitle());
                    assertEquals("Классическая бейсболка из хлопка с регулируемым размером.", item.getDescription());
                    assertEquals(990L, item.getPrice());
                    assertEquals("black-cap.png", item.getImagePath());
                })
                .verifyComplete();
    }

    @Test
    void itemSaveAndFindTest() {
        Item item = new Item();
        item.setTitle("Тестовый товар");
        item.setDescription("Описание");
        item.setPrice(1234L);
        item.setImagePath("test.png");

        StepVerifier.create(itemRepository.save(item).flatMap(saved -> itemRepository.findById(saved.getId())))
                .assertNext(found -> {
                    assertNotNull(found.getId());
                    assertEquals("Тестовый товар", found.getTitle());
                    assertEquals("Описание", found.getDescription());
                    assertEquals(1234L, found.getPrice());
                    assertEquals("test.png", found.getImagePath());
                })
                .verifyComplete();
    }
}
