package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    CartItemRepository cartItemRepository;

    CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, new ItemMapper("images/"));
    }

    @Test
    void getCartItemsTest() {
        when(cartItemRepository.findAllWithItems())
                .thenReturn(Flux.just(
                        new ItemDetailedRow(1L, "Мяч", "круглый", "ball.png", 990L, 2),
                        new ItemDetailedRow(2L, "Ракетка", "для тенниса", "racket.png", 1990L, 1)));

        StepVerifier.create(cartService.getCartItems())
                .assertNext(dto -> {
                    assertEquals(1L, dto.id());
                    assertEquals("Мяч", dto.title());
                    assertEquals("images/1", dto.imgPath());
                    assertEquals(990L, dto.price());
                    assertEquals(2, dto.count());
                })
                .assertNext(dto -> {
                    assertEquals(2L, dto.id());
                    assertEquals("Ракетка", dto.title());
                    assertEquals("images/2", dto.imgPath());
                    assertEquals(1990L, dto.price());
                    assertEquals(1, dto.count());
                })
                .verifyComplete();
    }

    @Test
    void plusIncrementsExistingItemTest() {
        CartItem existing = cartItem(10L, 1L, 2);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(cartService.changeCount(1L, Action.PLUS)).verifyComplete();

        assertEquals(3, existing.getCount());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void plusAddsNewItemWhenAbsentTest() {
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(new CartItem()));

        StepVerifier.create(cartService.changeCount(1L, Action.PLUS)).verifyComplete();

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getItemId());
        assertEquals(1, captor.getValue().getCount());
    }

    @Test
    void minusDecrementsTest() {
        CartItem existing = cartItem(10L, 1L, 3);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(cartService.changeCount(1L, Action.MINUS)).verifyComplete();

        assertEquals(2, existing.getCount());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void minusRemovesWhenReachesZeroTest() {
        CartItem existing = cartItem(10L, 1L, 1);
        when(cartItemRepository.findByItemId(1L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(1L, Action.MINUS)).verifyComplete();

        verify(cartItemRepository).delete(existing);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void deleteRemovesByItemIdTest() {
        when(cartItemRepository.deleteByItemId(1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(1L, Action.DELETE)).verifyComplete();

        verify(cartItemRepository).deleteByItemId(1L);
    }

    private static CartItem cartItem(Long id, Long itemId, int count) {
        CartItem cartItem = new CartItem();

        cartItem.setId(id);
        cartItem.setItemId(itemId);
        cartItem.setCount(count);

        return cartItem;
    }
}
