package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;

    public CartService(CartItemRepository cartItemRepository, ItemMapper itemMapper) {
        this.cartItemRepository = cartItemRepository;
        this.itemMapper = itemMapper;
    }

    public Flux<ItemDto> getCartItems() {
        return cartItemRepository.findAllWithItems().map(itemMapper::toDto);
    }

    public Mono<Void> changeCount(Long itemId, Action action) {
        return switch (action) {
            case PLUS -> increase(itemId);
            case MINUS -> decrease(itemId);
            case DELETE -> cartItemRepository.deleteByItemId(itemId);
        };
    }

    private Mono<Void> increase(Long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItem -> {
                    cartItem.setCount(cartItem.getCount() + 1);
                    return cartItemRepository.save(cartItem);
                })
                .switchIfEmpty(Mono.defer(() -> addNewToCart(itemId)))
                .then();
    }

    private Mono<Void> decrease(Long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItem -> {
                    int newCount = cartItem.getCount() - 1;
                    if (newCount <= 0) {
                        return cartItemRepository.delete(cartItem);
                    }
                    cartItem.setCount(newCount);
                    return cartItemRepository.save(cartItem).then();
                });
    }

    private Mono<CartItem> addNewToCart(Long itemId) {
        CartItem cartItem = new CartItem();

        cartItem.setItemId(itemId);
        cartItem.setCount(1);

        return cartItemRepository.save(cartItem);
    }
}
