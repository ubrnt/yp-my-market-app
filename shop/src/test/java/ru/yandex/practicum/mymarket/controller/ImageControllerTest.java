package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebFluxTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ItemService itemService;

    @Test
    void image_returnsPngBytes() {
        when(itemService.getImage(1L)).thenReturn(Mono.just(new byte[]{1, 2, 3, 4}));

        webTestClient.get().uri("/images/1").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectBody().consumeWith(result -> {
                    byte[] body = result.getResponseBody();
                    assertTrue(body != null && body.length > 0);
                });
    }

    @Test
    void image_whenItemNotFound_returnsNotFound() {
        when(itemService.getImage(99L)).thenReturn(Mono.empty());

        webTestClient.get().uri("/images/99").exchange()
                .expectStatus().isNotFound();
    }
}
