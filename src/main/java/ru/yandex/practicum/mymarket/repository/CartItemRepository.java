package ru.yandex.practicum.mymarket.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mymarket.domain.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByItemId(Long itemId);

    List<CartItem> findByItemIdIn(Collection<Long> itemIds);
}
