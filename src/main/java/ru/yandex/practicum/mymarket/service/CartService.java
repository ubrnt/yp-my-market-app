package ru.yandex.practicum.mymarket.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.domain.CartItem;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public CartService(CartItemRepository cartItemRepository,
                       ItemRepository itemRepository,
                       ItemMapper itemMapper) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true)
    public List<ItemDto> getCartItems() {
        return cartItemRepository.findAll().stream()
                .map(cartItem -> itemMapper.toDto(cartItem.getItem(), cartItem.getCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long getTotal() {
        return cartItemRepository.findAll().stream()
                .mapToLong(cartItem -> cartItem.getItem().getPrice() * cartItem.getCount())
                .sum();
    }

    @Transactional(readOnly = true)
    public int getCount(Long itemId) {
        return cartItemRepository.findByItemId(itemId)
                .map(CartItem::getCount)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> getCountByItemIds(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }

        return cartItemRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.toMap(
                        cartItem -> cartItem.getItem().getId(),
                        CartItem::getCount));
    }

    @Transactional
    public void changeCount(Long itemId, Action action) {
        CartItem cartItem = cartItemRepository.findByItemId(itemId).orElse(null);
        switch (action) {
            case PLUS -> plus(itemId, cartItem);
            case MINUS -> minus(cartItem);
            case DELETE -> {
                if (cartItem != null) {
                    cartItemRepository.delete(cartItem);
                }
            }
        }
    }

    private void plus(Long itemId, CartItem cartItem) {
        if (cartItem == null) {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
            CartItem created = new CartItem();
            created.setItem(item);
            created.setCount(1);
            cartItemRepository.save(created);
        } else {
            cartItem.setCount(cartItem.getCount() + 1);
            cartItemRepository.save(cartItem);
        }
    }

    private void minus(CartItem cartItem) {
        if (cartItem == null) {
            return;
        }
        int count = cartItem.getCount() - 1;
        if (count <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setCount(count);
            cartItemRepository.save(cartItem);
        }
    }
}
