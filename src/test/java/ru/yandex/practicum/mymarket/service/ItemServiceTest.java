package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final int ROW_SIZE = 3;

    @Mock
    ItemRepository itemRepository;

    ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository, new ItemMapper("images/", ROW_SIZE), "images/");
    }

    @Test
    void getItem_returnsDtoWithCartCountAndImgPath() {
        when(itemRepository.findByIdWithCountInCart(1L))
                .thenReturn(Mono.just(new ItemDetailedRow(1L, "Мяч", "круглый", "ball.png", 990L, 2)));

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
    void getItem_whenNotFound_throws() {
        when(itemRepository.findByIdWithCountInCart(99L)).thenReturn(Mono.empty());

        StepVerifier.create(itemService.getItem(99L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void getItems_groupsByRowSize_andPadsLastRow() {
        when(itemRepository.findForPage(null, SortType.NO, 6, 0L))
                .thenReturn(Flux.just(row(1), row(2), row(3), row(4)));

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
        when(itemRepository.findForPage(null, SortType.NO, 3, 2L))
                .thenReturn(Flux.just(row(1), row(2), row(3)));

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

        verify(itemRepository).findForPage(null, SortType.NO, 3, 2L);
    }

    @Test
    void getItems_forwardsSearchAndSort() {
        when(itemRepository.findForPage("мяч", SortType.PRICE, 6, 0L)).thenReturn(Flux.empty());

        StepVerifier.create(itemService.getItems("мяч", SortType.PRICE, 1, 5))
                .assertNext(page -> assertTrue(page.items().isEmpty()))
                .verifyComplete();

        verify(itemRepository).findForPage("мяч", SortType.PRICE, 6, 0L);
    }

    private static ItemDetailedRow row(long id) {
        return new ItemDetailedRow(id, "Товар " + id, "Описание", "img" + id + ".png", 100L * id, 0);
    }
}
