package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.mymarket.domain.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
