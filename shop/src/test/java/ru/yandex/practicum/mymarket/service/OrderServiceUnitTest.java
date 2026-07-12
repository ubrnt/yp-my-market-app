package ru.yandex.practicum.mymarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.PaymentResult;
import ru.yandex.practicum.mymarket.domain.Order;
import ru.yandex.practicum.mymarket.domain.OrderItem;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.repository.projection.ItemDetailedRow;
import ru.yandex.practicum.mymarket.repository.projection.OrderItemDetailedRow;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderItemRepository orderItemRepository;
    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    PaymentServiceClient paymentServiceClient;
    @Mock
    TransactionalOperator transactionalOperator;

    OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, cartItemRepository,
                new OrderMapper(), paymentServiceClient, transactionalOperator);
    }

    @Test
    void getOrders_mapsRowsToDtos() {
        when(orderRepository.findAllWithItems()).thenReturn(Flux.just(
                new OrderItemDetailedRow(7L, 990L, 1L, "Мяч", 990L, 1),
                new OrderItemDetailedRow(8L, 500L, 2L, "Ракетка", 500L, 1)));

        StepVerifier.create(orderService.getOrders())
                .assertNext(order -> assertEquals(7L, order.id()))
                .assertNext(order -> assertEquals(8L, order.id()))
                .verifyComplete();
    }

    @Test
    void getOrder_returnsOrderWithItems() {
        when(orderRepository.findByIdWithItems(7L)).thenReturn(Flux.just(
                new OrderItemDetailedRow(7L, 1480L, 1L, "Мяч", 990L, 1),
                new OrderItemDetailedRow(7L, 1480L, 2L, "Ракетка", 490L, 1)));

        StepVerifier.create(orderService.getOrder(7L))
                .assertNext(order -> {
                    assertEquals(7L, order.id());
                    assertEquals(1480L, order.totalSum());
                    assertEquals(2, order.items().size());
                })
                .verifyComplete();
    }

    @Test
    void getOrder_whenNotFound_throws() {
        when(orderRepository.findByIdWithItems(99L)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrder(99L))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void buy_whenPaymentSucceeds_createsOrderAndClearsCart() {
        when(cartItemRepository.findAllWithItems()).thenReturn(Flux.just(
                new ItemDetailedRow(1L, "Мяч", "о", "ball.png", 990L, 2),
                new ItemDetailedRow(2L, "Ракетка", "о", "racket.png", 500L, 1)));
        when(paymentServiceClient.pay(2480L)).thenReturn(Mono.just(PaymentResult.SUCCESS));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));

        Order savedOrder = new Order();
        savedOrder.setId(7L);
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyIterable())).thenReturn(Flux.just(new OrderItem()));
        when(cartItemRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(orderService.buy())
                .expectNext(7L)
                .verifyComplete();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(2480L, orderCaptor.getValue().getTotalSum());
        verify(cartItemRepository).deleteAll();
    }

    @Test
    void buy_whenInsufficientFunds_doesNotCreateOrder() {
        when(cartItemRepository.findAllWithItems()).thenReturn(Flux.just(
                new ItemDetailedRow(1L, "Мяч", "о", "ball.png", 990L, 2)));
        when(paymentServiceClient.pay(1980L)).thenReturn(Mono.just(PaymentResult.INSUFFICIENT_FUNDS));

        StepVerifier.create(orderService.buy())
                .verifyComplete();

        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAll();
    }

    @Test
    void buy_whenPaymentUnavailable_doesNotCreateOrder() {
        when(cartItemRepository.findAllWithItems()).thenReturn(Flux.just(
                new ItemDetailedRow(1L, "Мяч", "о", "ball.png", 990L, 2)));
        when(paymentServiceClient.pay(1980L)).thenReturn(Mono.just(PaymentResult.UNAVAILABLE));

        StepVerifier.create(orderService.buy())
                .verifyComplete();

        verify(orderRepository, never()).save(any());
    }

    @Test
    void buy_whenCartEmpty_throws() {
        when(cartItemRepository.findAllWithItems()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.buy())
                .expectError(IllegalStateException.class)
                .verify();
    }
}
