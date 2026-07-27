package ru.yandex.practicum.mymarket.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cache.RedisItemProvider;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemCountRow;


@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemMapper itemMapper;
    private final PaymentServiceClient paymentServiceClient;

    private final RedisItemProvider redisItemProvider;

    public CartService(CartItemRepository cartItemRepository, ItemMapper itemMapper,
                       PaymentServiceClient paymentServiceClient, RedisItemProvider redisItemProvider) {
        this.cartItemRepository = cartItemRepository;
        this.itemMapper = itemMapper;
        this.paymentServiceClient = paymentServiceClient;
        this.redisItemProvider = redisItemProvider;
    }

    public Mono<CartDto> getCart(Long userId) {
        return cartItemRepository.findAllIdsCount(userId)
                .collectList().flatMap(idCounts -> {
                    List<Long> ids = idCounts.stream().map(ItemCountRow::id).toList();
                    Map<Long, Integer> counts = idCounts.stream()
                            .collect(Collectors.toMap(ItemCountRow::id, ItemCountRow::count));

                    return redisItemProvider.getAll(ids)
                            .map(it -> itemMapper.toDto(it , counts.getOrDefault(it.getId(), 0)))
                            .collectList()
                            .map(items -> new CartDto(items, total(items)));
                });
    }

    public Mono<CheckoutState> checkoutState(long total) {
        return paymentServiceClient.getBalance().map(result -> switch (result.status()) {
            case AVAILABLE -> result.balance() >= total ? CheckoutState.OK : CheckoutState.INSUFFICIENT_FUNDS;
            case ACCOUNT_NOT_FOUND -> CheckoutState.ACCOUNT_NOT_FOUND;
            case UNAVAILABLE -> CheckoutState.UNAVAILABLE;
        });
    }

    public Mono<Void> changeCount(Long userId, Long itemId, Action action) {
        return switch (action) {
            case PLUS -> increase(userId, itemId);
            case MINUS -> decrease(userId, itemId);
            case DELETE -> cartItemRepository.deleteByUserIdAndItemId(userId, itemId);
        };
    }

    private Mono<Void> increase(Long userId, Long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .flatMap(cartItem -> {
                    cartItem.setCount(cartItem.getCount() + 1);
                    return cartItemRepository.save(cartItem);
                })
                .switchIfEmpty(Mono.defer(() -> addNewToCart(userId, itemId)))
                .then();
    }

    private Mono<CartItem> addNewToCart(Long userId, Long itemId) {
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setItemId(itemId);
        cartItem.setCount(1);

        return cartItemRepository.save(cartItem);
    }

    private Mono<Void> decrease(Long userId, Long itemId) {
        return cartItemRepository.findByUserIdAndItemId(userId, itemId)
                .flatMap(cartItem -> {
                    int newCount = cartItem.getCount() - 1;
                    if (newCount <= 0) {
                        return cartItemRepository.delete(cartItem);
                    }
                    cartItem.setCount(newCount);
                    return cartItemRepository.save(cartItem).then();
                });
    }

    private long total(List<ItemDto> items) {
        return items.stream().mapToLong(item -> item.price() * item.count()).sum();
    }

    public enum CheckoutState {
        OK,
        INSUFFICIENT_FUNDS,
        ACCOUNT_NOT_FOUND,
        UNAVAILABLE
    }
}
