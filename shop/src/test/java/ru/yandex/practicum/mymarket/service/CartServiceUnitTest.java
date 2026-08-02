package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.cache.RedisItemProvider;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;

@ExtendWith(MockitoExtension.class)
class CartServiceUnitTest {

    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    PaymentServiceClient paymentServiceClient;
    @Mock
    RedisItemProvider redisItemProvider;

    CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, new ItemMapper("images/", 3),
                paymentServiceClient, redisItemProvider);
    }

    @Test
    void getCart_mapsItemsFromProviderAndComputesTotal() {
        when(cartItemRepository.findAllIdsCount(1L))
                .thenReturn(Flux.just(new ItemCountRow(1L, 2), new ItemCountRow(2L, 1)));
        when(redisItemProvider.getAll(List.of(1L, 2L)))
                .thenReturn(Flux.just(item(1L, "Мяч", 990L), item(2L, "Ракетка", 1990L)));

        StepVerifier.create(cartService.getCart(1L))
                .assertNext(cart -> {
                    assertEquals(2, cart.items().size());
                    assertEquals(1L, cart.items().get(0).id());
                    assertEquals("images/1", cart.items().get(0).imgPath());
                    assertEquals(2, cart.items().get(0).count());
                    assertEquals(2L, cart.items().get(1).id());
                    assertEquals(990L * 2 + 1990L, cart.total());
                })
                .verifyComplete();
    }

    @Test
    void plus_whenAlreadyInCart_increments() {
        CartItem existing = cartItem(10L, 1L, 2);
        when(cartItemRepository.findByUserIdAndItemId(1L, 1L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(cartService.changeCount(1L, 1L, Action.PLUS)).verifyComplete();

        assertEquals(3, existing.getCount());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void plus_whenNotInCart_createsCartItemWithCountOne() {
        when(cartItemRepository.findByUserIdAndItemId(1L, 1L)).thenReturn(Mono.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(new CartItem()));

        StepVerifier.create(cartService.changeCount(1L, 1L, Action.PLUS)).verifyComplete();

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getItemId());
        assertEquals(1, captor.getValue().getCount());
    }

    @Test
    void minus_whenCountAboveOne_decrements() {
        CartItem existing = cartItem(10L, 1L, 3);
        when(cartItemRepository.findByUserIdAndItemId(1L, 1L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.save(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(cartService.changeCount(1L, 1L, Action.MINUS)).verifyComplete();

        assertEquals(2, existing.getCount());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void minus_whenCountReachesZero_deletes() {
        CartItem existing = cartItem(10L, 1L, 1);
        when(cartItemRepository.findByUserIdAndItemId(1L, 1L)).thenReturn(Mono.just(existing));
        when(cartItemRepository.delete(existing)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(1L, 1L, Action.MINUS)).verifyComplete();

        verify(cartItemRepository).delete(existing);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void delete_removesCartItem() {
        when(cartItemRepository.deleteByUserIdAndItemId(1L, 1L)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.changeCount(1L, 1L, Action.DELETE)).verifyComplete();

        verify(cartItemRepository).deleteByUserIdAndItemId(1L, 1L);
    }

    private static CartItem cartItem(Long id, Long itemId, int count) {
        CartItem cartItem = new CartItem();

        cartItem.setId(id);
        cartItem.setItemId(itemId);
        cartItem.setCount(count);

        return cartItem;
    }

    private static Item item(long id, String title, long price) {
        Item item = new Item();

        item.setId(id);
        item.setTitle(title);
        item.setDescription("desc");
        item.setImagePath("img.png");
        item.setPrice(price);

        return item;
    }
}
