package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.item;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.items;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class ItemServiceUnitTest {

    private static final int ROW_SIZE = 2;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    private ItemService service;

    @BeforeEach
    void setUp() {
        service = new ItemService(itemRepository, cartService, new ItemMapper(), ROW_SIZE);
    }

    @Test
    void getItems_groupsByRowSize_andPadsLastRowWithStubs() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(items(1, 2, 3, 4, 5)));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        ItemsPageDto page = service.getItems(null, SortType.NO, 1, ROW_SIZE);

        assertEquals(3, page.items().size());
        page.items().forEach(row -> assertEquals(ROW_SIZE, row.size()));
        assertEquals(5, count(page, dto -> !dto.isDummy()));
        assertEquals(1, count(page, ItemDto::isDummy));
    }

    @Test
    void getItems_blankSearch_usesFindAll() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        service.getItems("  ", SortType.NO, 1, ROW_SIZE);

        verify(itemRepository).findAll(any(Pageable.class));
        verify(itemRepository, never()).search(any(), any());
    }

    @Test
    void getItems_withSearch_usesSearch() {
        when(itemRepository.search(eq("ball"), any())).thenReturn(new PageImpl<>(List.of()));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        service.getItems("ball", SortType.NO, 1, ROW_SIZE);

        verify(itemRepository).search(eq("ball"), any());
        verify(itemRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getItems_buildsPageableWithSortAndZeroBasedPage() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        service.getItems(null, SortType.PRICE, 2, ROW_SIZE);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(ROW_SIZE, pageable.getPageSize());
        assertNotNull(pageable.getSort().getOrderFor("price"));
    }

    @Test
    void paging_firstPage_hasNextButNoPrevious() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(items(1, 2), PageRequest.of(0, 2), 6));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        var paging = service.getItems(null, SortType.NO, 1, 2).paging();

        assertEquals(1, paging.pageNumber());
        assertEquals(2, paging.pageSize());
        assertFalse(paging.hasPrevious());
        assertTrue(paging.hasNext());
    }

    @Test
    void paging_middlePage_hasPreviousAndNext() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(items(3, 4), PageRequest.of(1, 2), 6));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        var paging = service.getItems(null, SortType.NO, 2, 2).paging();

        assertEquals(2, paging.pageNumber());
        assertTrue(paging.hasPrevious());
        assertTrue(paging.hasNext());
    }

    @Test
    void paging_lastPage_hasPreviousButNoNext() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(items(5, 6), PageRequest.of(2, 2), 6));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of());

        var paging = service.getItems(null, SortType.NO, 3, 2).paging();

        assertEquals(3, paging.pageNumber());
        assertTrue(paging.hasPrevious());
        assertFalse(paging.hasNext());
    }

    @Test
    void getItems_setsCountFromCart() {
        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(items(1, 2)));
        when(cartService.getCountByItemIds(any())).thenReturn(Map.of(1L, 3));

        ItemsPageDto page = service.getItems(null, SortType.NO, 1, ROW_SIZE);

        Map<Long, Integer> byId = page.items().stream()
                .flatMap(List::stream)
                .filter(dto -> !dto.isDummy())
                .collect(java.util.stream.Collectors.toMap(ItemDto::id, ItemDto::count));
        assertEquals(3, byId.get(1L));
        assertEquals(0, byId.get(2L));
    }

    @Test
    void getItem_returnsDtoWithCartCountAndImgPath() {
        Item item = item(7L);
        when(itemRepository.findById(7L)).thenReturn(Optional.of(item));
        when(cartService.getCount(7L)).thenReturn(2);

        ItemDto dto = service.getItem(7L);

        assertEquals(7L, dto.id());
        assertEquals("images/7", dto.imgPath());
        assertEquals(2, dto.count());
    }

    @Test
    void getItem_whenNotFound_throws() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getItem(99L));
    }

    @Test
    void getImage_returnsBytesFromRepository() {
        byte[] bytes = {1, 2, 3};
        when(itemRepository.findImageById(7L)).thenReturn(bytes);

        assertArrayEquals(bytes, service.getImage(7L));
    }

    private long count(ItemsPageDto page, java.util.function.Predicate<ItemDto> match) {
        return page.items().stream()
                .flatMap(List::stream)
                .filter(match)
                .count();
    }

}
