package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cache.RedisItemProvider;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;

@ExtendWith(MockitoExtension.class)
class ItemServiceUnitTest {

    private static final int ROW_SIZE = 3;

    @Mock
    ItemRepository itemRepository;

    @Mock
    RedisItemProvider itemProvider;

    ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository, itemProvider, new ItemMapper("images/", ROW_SIZE), "images/");
    }

    @Test
    void getItem_returnsDtoWithCartCountAndImgPath() {
        when(itemProvider.get(1L)).thenReturn(Mono.just(item(1L, "Мяч", 990L)));
        when(itemRepository.countInCart(1L)).thenReturn(Mono.just(2));

        StepVerifier.create(itemService.getItem(1L))
                .assertNext(dto -> {
                    assertEquals(1L, dto.id());
                    assertEquals("Мяч", dto.title());
                    assertEquals("images/1", dto.imgPath());
                    assertEquals(990L, dto.price());
                    assertEquals(2, dto.count());
                })
                .verifyComplete();
    }

    @Test
    void getItem_whenNotInCart_countIsZero() {
        when(itemProvider.get(1L)).thenReturn(Mono.just(item(1L, "Мяч", 990L)));
        when(itemRepository.countInCart(1L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItem(1L))
                .assertNext(dto -> assertEquals(0, dto.count()))
                .verifyComplete();
    }

    @Test
    void getItem_whenNotFound_throws() {
        when(itemProvider.get(99L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItem(99L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void getImage_returnsImageBytes() {
        Item item = new Item();
        item.setImagePath("black-cap.png");
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));

        StepVerifier.create(itemService.getImage(1L))
                .assertNext(bytes -> assertTrue(bytes.length > 0))
                .verifyComplete();
    }

    @Test
    void getImage_whenFileMissing_returnsEmpty() {
        Item item = new Item();
        item.setImagePath("does-not-exist.png");
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));

        StepVerifier.create(itemService.getImage(1L))
                .verifyComplete();
    }

    @Test
    void getItems_groupsByRowSize_andPadsLastRow() {
        when(itemRepository.findPageIdsWithCount(null, SortType.NO, 6, 0L))
                .thenReturn(Flux.just(countRow(1), countRow(2), countRow(3), countRow(4)));
        when(itemProvider.getAll(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(Flux.just(item(1), item(2), item(3), item(4)));

        StepVerifier.create(itemService.getItems(null, SortType.NO, 1, 5))
                .assertNext(page -> {
                    assertEquals(2, page.items().size());
                    assertEquals(3, page.items().get(0).size());
                    assertFalse(page.items().get(0).get(0).isDummy());

                    assertEquals(3, page.items().get(1).size());
                    assertEquals(4L, page.items().get(1).get(0).id());
                    assertTrue(page.items().get(1).get(1).isDummy());
                    assertTrue(page.items().get(1).get(2).isDummy());

                    ItemsPageDto.PagingDto paging = page.paging();
                    assertEquals(5, paging.pageSize());
                    assertEquals(1, paging.pageNumber());
                    assertFalse(paging.hasPrevious());
                    assertFalse(paging.hasNext());
                })
                .verifyComplete();
    }

    @Test
    void paging_middlePage_hasPreviousAndNext() {
        when(itemRepository.findPageIdsWithCount(null, SortType.NO, 3, 2L))
                .thenReturn(Flux.just(countRow(1), countRow(2), countRow(3)));
        when(itemProvider.getAll(List.of(1L, 2L)))
                .thenReturn(Flux.just(item(1), item(2)));

        StepVerifier.create(itemService.getItems(null, SortType.NO, 2, 2))
                .assertNext(page -> {
                    long real = page.items().stream().flatMap(java.util.List::stream)
                            .filter(item -> !item.isDummy()).count();
                    assertEquals(2, real);

                    ItemsPageDto.PagingDto paging = page.paging();
                    assertTrue(paging.hasPrevious());
                    assertTrue(paging.hasNext());
                })
                .verifyComplete();

        verify(itemRepository).findPageIdsWithCount(null, SortType.NO, 3, 2L);
    }

    @Test
    void getItems_forwardsSearchAndSort() {
        when(itemRepository.findPageIdsWithCount("мяч", SortType.PRICE, 6, 0L)).thenReturn(Flux.empty());
        when(itemProvider.getAll(List.of())).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItems("мяч", SortType.PRICE, 1, 5))
                .assertNext(page -> assertTrue(page.items().isEmpty()))
                .verifyComplete();

        verify(itemRepository).findPageIdsWithCount("мяч", SortType.PRICE, 6, 0L);
    }

    private static ItemCountRow countRow(long id) {
        return new ItemCountRow(id, 0);
    }

    private static Item item(long id) {
        return item(id, "Товар " + id, 100L * id);
    }

    private static Item item(long id, String title, long price) {
        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setDescription("Описание");
        item.setImagePath("img" + id + ".png");
        item.setPrice(price);
        return item;
    }
}
