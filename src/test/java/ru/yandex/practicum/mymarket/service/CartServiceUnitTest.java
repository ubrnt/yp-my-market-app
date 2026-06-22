package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.cartItem;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceUnitTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ItemRepository itemRepository;

    private CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(cartItemRepository, itemRepository, new ItemMapper());
    }

    @Test
    void plus_whenNotInCart_createsCartItemWithCountOne() {
        Item item = item(1L, 990L);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        service.changeCount(1L, Action.PLUS);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getCount());
        assertSame(item, captor.getValue().getItem());
    }

    @Test
    void plus_whenAlreadyInCart_increments() {
        CartItem existing = cartItem(item(1L, 990L), 2);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(existing));

        service.changeCount(1L, Action.PLUS);

        assertEquals(3, existing.getCount());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void minus_whenCountAboveOne_decrements() {
        CartItem existing = cartItem(item(1L, 990L), 2);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(existing));

        service.changeCount(1L, Action.MINUS);

        assertEquals(1, existing.getCount());
        verify(cartItemRepository).save(existing);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void minus_whenCountReachesZero_deletes() {
        CartItem existing = cartItem(item(1L, 990L), 1);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(existing));

        service.changeCount(1L, Action.MINUS);

        verify(cartItemRepository).delete(existing);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void minus_whenNotInCart_doesNothing() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        service.changeCount(1L, Action.MINUS);

        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void delete_removesCartItem() {
        CartItem existing = cartItem(item(1L, 990L), 5);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(existing));

        service.changeCount(1L, Action.DELETE);

        verify(cartItemRepository).delete(existing);
    }

    @Test
    void plus_whenItemNotFound_throws() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.changeCount(1L, Action.PLUS));
    }

    @Test
    void getTotal_sumsPriceTimesCount() {
        when(cartItemRepository.findAll()).thenReturn(List.of(
                cartItem(item(1L, 990L), 2),
                cartItem(item(2L, 1990L), 1)));

        assertEquals(990L * 2 + 1990L, service.getTotal());
    }

    @Test
    void getCount_returnsCountOrZero() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.of(cartItem(item(1L, 990L), 4)));
        when(cartItemRepository.findByItemId(2L)).thenReturn(Optional.empty());

        assertEquals(4, service.getCount(1L));
        assertEquals(0, service.getCount(2L));
    }

    @Test
    void getCountByItemIds_mapsItemIdToCount() {
        when(cartItemRepository.findByItemIdIn(List.of(1L, 2L))).thenReturn(List.of(
                cartItem(item(1L, 990L), 2),
                cartItem(item(2L, 1990L), 3)));

        Map<Long, Integer> counts = service.getCountByItemIds(List.of(1L, 2L));

        assertEquals(2, counts.get(1L));
        assertEquals(3, counts.get(2L));
    }

    @Test
    void getCountByItemIds_whenEmpty_returnsEmptyMapWithoutQuery() {
        Map<Long, Integer> counts = service.getCountByItemIds(List.of());

        assertEquals(Map.of(), counts);
        verify(cartItemRepository, never()).findByItemIdIn(any());
    }

    @Test
    void getCartItems_mapsToDtoWithCount() {
        when(cartItemRepository.findAll()).thenReturn(List.of(
                cartItem(item(1L, 990L), 2),
                cartItem(item(2L, 1990L), 1)));

        Map<Long, Integer> byId = service.getCartItems().stream()
                .collect(Collectors.toMap(ru.yandex.practicum.mymarket.dto.ItemDto::id,
                        ru.yandex.practicum.mymarket.dto.ItemDto::count));

        assertEquals(2, byId.get(1L));
        assertEquals(1, byId.get(2L));
    }

    @Test
    void delete_whenNotInCart_doesNothing() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Optional.empty());

        service.changeCount(1L, Action.DELETE);

        verify(cartItemRepository, never()).delete(any());
    }

    @Test
    void getTotal_emptyCart_returnsZero() {
        when(cartItemRepository.findAll()).thenReturn(List.of());

        assertEquals(0L, service.getTotal());
    }

}
