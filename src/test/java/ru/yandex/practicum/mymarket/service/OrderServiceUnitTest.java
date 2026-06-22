package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.cartItem;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.item;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.order;
import static ru.yandex.practicum.mymarket.util.TestDataFactory.orderItem;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, cartItemRepository, new OrderMapper());
    }

    @Test
    void buy_buildsOrderFromCart_clearsCart_returnsId() {
        when(cartItemRepository.findAll()).thenReturn(List.of(
                cartItem(item(1L, 990L, "Кепка"), 2),
                cartItem(item(2L, 1990L, "Мяч"), 1)));
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(42L);
            return order;
        });

        Long orderId = service.buy();

        assertEquals(42L, orderId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertEquals(990L * 2 + 1990L, saved.getTotalSum());
        assertEquals(2, saved.getItems().size());

        verify(cartItemRepository).deleteAll();
    }

    @Test
    void buy_emptyCart_throws_andDoesNotPersist() {
        when(cartItemRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.buy());

        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll();
    }

    @Test
    void getOrders_mapsOrdersToDto() {
        Order order = order(7L, 2980L, orderItem(item(5L, 1490L, "Зонт"), 2));
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderDto> orders = service.getOrders();

        assertEquals(1, orders.size());
        OrderDto dto = orders.get(0);
        assertEquals(7L, dto.id());
        assertEquals(2980L, dto.totalSum());
        assertEquals(1, dto.items().size());
        assertEquals("Зонт", dto.items().get(0).title());
    }

    @Test
    void getOrder_whenNotFound_throws() {
        when(orderRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.getOrder(9L));
    }

}
