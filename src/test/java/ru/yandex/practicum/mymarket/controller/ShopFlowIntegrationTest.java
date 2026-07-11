package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;

class ShopFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void addToCart_buy_thenOrderAppearsAndCartCleared() {
        Long itemId = itemRepository.findAll().blockFirst().getId();

        webTestClient.post().uri("/items")
                .body(BodyInserters.fromFormData("id", itemId.toString())
                        .with("action", "PLUS")
                        .with("search", "")
                        .with("sort", "NO")
                        .with("pageNumber", "1")
                        .with("pageSize", "5"))
                .exchange()
                .expectStatus().is3xxRedirection();

        assertEquals(1L, cartItemRepository.count().block());

        URI orderLocation = webTestClient.post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getLocation();

        assertNotNull(orderLocation);
        assertTrue(orderLocation.toString().matches("/orders/\\d+\\?newOrder=true"));

        assertEquals(1L, orderRepository.count().block());
        assertEquals(0L, cartItemRepository.count().block());

        webTestClient.get().uri("/orders").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains(orderLocation.getPath())));
    }
}
