package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;

@WebFluxTest(CartController.class)
class CartControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    CartService cartService;

    @Test
    void cart_returnsCartViewWithItems() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart()).thenReturn(Mono.just(cart));

        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));
    }

    @Test
    void changeCount_rerendersCartView() {
        when(cartService.changeCount(1L, Action.DELETE)).thenReturn(Mono.empty());
        when(cartService.getCart()).thenReturn(Mono.just(new CartDto(List.of(), 0L)));

        webTestClient.post().uri("/cart/items?id=1&action=DELETE").exchange()
                .expectStatus().isOk();

        verify(cartService).changeCount(1L, Action.DELETE);
    }
}
