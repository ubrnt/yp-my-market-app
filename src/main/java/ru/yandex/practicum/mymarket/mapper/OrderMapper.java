package ru.yandex.practicum.mymarket.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;

@Component
public class OrderMapper {

    public OrderDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return new OrderDto(order.getId(), items, order.getTotalSum());
    }

    private OrderItemDto toItemDto(OrderItem orderItem) {
        Item item = orderItem.getItem();

        return new OrderItemDto(
                item.getId(),
                item.getTitle(),
                item.getPrice(),
                orderItem.getCount()
        );
    }
}
