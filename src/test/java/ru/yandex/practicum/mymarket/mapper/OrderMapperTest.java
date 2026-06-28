package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.domain.Item;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toDto_mapsOrderWithItems() {
        Item item = new Item();
        item.setId(5L);
        item.setTitle("Зонт-трость");
        item.setPrice(1490L);

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setCount(2);

        Order order = new Order();
        order.setId(42L);
        order.setTotalSum(2980L);
        order.addItem(orderItem);

        OrderDto dto = mapper.toDto(order);

        assertEquals(42L, dto.id());
        assertEquals(2980L, dto.totalSum());
        assertEquals(1, dto.items().size());

        OrderItemDto line = dto.items().get(0);
        assertEquals(5L, line.id());
        assertEquals("Зонт-трость", line.title());
        assertEquals(1490L, line.price());
        assertEquals(2, line.count());
    }
}
