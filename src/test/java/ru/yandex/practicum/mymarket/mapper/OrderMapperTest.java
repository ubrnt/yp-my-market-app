package ru.yandex.practicum.mymarket.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.repository.projection.OrderItemDetailedRow;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    void toDto_mapsOrderWithItems() {
        List<OrderItemDetailedRow> rows = List.of(
                new OrderItemDetailedRow(7L, 2970L, 1L, "Мяч", 990L, 2),
                new OrderItemDetailedRow(7L, 2970L, 2L, "Ракетка", 990L, 1));

        OrderDto dto = orderMapper.toDto(rows);

        assertEquals(7L, dto.id());
        assertEquals(2970L, dto.totalSum());
        assertEquals(2, dto.items().size());

        assertEquals(1L, dto.items().get(0).id());
        assertEquals("Мяч", dto.items().get(0).title());
        assertEquals(990L, dto.items().get(0).price());
        assertEquals(2, dto.items().get(0).count());

        assertEquals(2L, dto.items().get(1).id());
        assertEquals("Ракетка", dto.items().get(1).title());
    }

    @Test
    void toDtoList_groupsRowsByOrder() {
        List<OrderItemDetailedRow> rows = List.of(
                new OrderItemDetailedRow(7L, 990L, 1L, "Мяч", 990L, 1),
                new OrderItemDetailedRow(8L, 1480L, 1L, "Мяч", 990L, 1),
                new OrderItemDetailedRow(8L, 1480L, 2L, "Ракетка", 490L, 1));

        List<OrderDto> orders = orderMapper.toDtoList(rows);

        assertEquals(2, orders.size());

        assertEquals(7L, orders.get(0).id());
        assertEquals(1, orders.get(0).items().size());

        assertEquals(8L, orders.get(1).id());
        assertEquals(1480L, orders.get(1).totalSum());
        assertEquals(2, orders.get(1).items().size());
    }
}
