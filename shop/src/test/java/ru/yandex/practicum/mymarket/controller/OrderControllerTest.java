package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebFluxTest(OrderController.class)
//todo ubrnt
@Disabled("temporary")
class OrderControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    OrderService orderService;

    private static OrderDto order(long id) {
        return new OrderDto(id, List.of(new OrderItemDto(1L, "Мяч", 990L, 1)), 990L);
    }

    @Test
    void orders_returnsOrdersView() {
        when(orderService.getOrders(1L)).thenReturn(Flux.just(order(7L)));

        webTestClient.get().uri("/orders").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заказ №7")));
    }

    @Test
    void order_returnsOrderView() {
        when(orderService.getOrder(7L, 1L)).thenReturn(Mono.just(order(7L)));

        webTestClient.get().uri("/orders/7").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заказ №7")));
    }

    @Test
    void order_whenNotFound_returns404() {
        when(orderService.getOrder(99L, 1L))
                .thenReturn(Mono.error(new NotFoundException(NotFoundException.Resource.ORDER, 99L)));

        webTestClient.get().uri("/orders/99").exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заказ не найден")));
    }

    @Test
    void buy_whenSuccess_redirectsToNewOrder() {
        when(orderService.buy(1L, 1L)).thenReturn(Mono.just(7L));

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/7?newOrder=true");
    }

    @Test
    void buy_whenPaymentFails_redirectsToCart() {
        when(orderService.buy(1L, 1L)).thenReturn(Mono.empty());

        webTestClient.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");
    }
}
