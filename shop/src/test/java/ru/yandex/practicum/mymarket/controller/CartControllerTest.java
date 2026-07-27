package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CartService.CheckoutState;

@WebFluxTest(CartController.class)
//todo ubrnt
@Disabled("temporary")
class CartControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    CartService cartService;

    @BeforeEach
    void setUp() {
        when(cartService.checkoutState(anyLong())).thenReturn(Mono.just(CheckoutState.OK));
    }

    @Test
    void cart_returnsCartViewWithItems() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));
    }

    @Test
    void cart_whenNotEnoughBalance_showsInsufficientMessage() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));
        when(cartService.checkoutState(anyLong())).thenReturn(Mono.just(CheckoutState.INSUFFICIENT_FUNDS));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Недостаточно средств")));
    }

    @Test
    void cart_whenAccountNotFound_showsAccountNotFoundMessage() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));
        when(cartService.checkoutState(anyLong())).thenReturn(Mono.just(CheckoutState.ACCOUNT_NOT_FOUND));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Счёт не найден")));
    }

    @Test
    void cart_whenServiceUnavailable_showsUnavailableMessage() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));
        when(cartService.checkoutState(anyLong())).thenReturn(Mono.just(CheckoutState.UNAVAILABLE));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Сервис оплаты недоступен")));
    }

    @Test
    void changeCount_rerendersCartView() {
        when(cartService.changeCount(1L, 1L, Action.DELETE)).thenReturn(Mono.empty());
        when(cartService.getCart(1L)).thenReturn(Mono.just(new CartDto(List.of(), 0L)));

        webTestClient.post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk();

        verify(cartService).changeCount(1L, 1L, Action.DELETE);
    }
}
