package ru.yandex.practicum.mymarket.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.domain.OrderItem;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;
import ru.yandex.practicum.mymarket.repository.projection.OrderItemDetailedRow;

@Component
public class OrderMapper {

    public List<OrderDto> toDtoList(List<OrderItemDetailedRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(OrderItemDetailedRow::orderId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::toDto)
                .toList();
    }

    public OrderDto toDto(List<OrderItemDetailedRow> orderRows) {
        OrderItemDetailedRow first = orderRows.getFirst();
        List<OrderItemDto> items = orderRows.stream()
                .map(row -> new OrderItemDto(row.itemId(), row.title(), row.price(), row.count()))
                .toList();

        return new OrderDto(first.orderId(), items, first.totalSum());
    }

    public List<OrderItem> toOrderItems(Long orderId, List<ItemDetailedRow> cartRows) {
        return cartRows.stream()
                .map(row -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrderId(orderId);
                    orderItem.setItemId(row.id());
                    orderItem.setCount(row.count());
                    return orderItem;
                })
                .toList();
    }
}
