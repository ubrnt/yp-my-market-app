package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.User;
import ru.yandex.practicum.mymarket.security.AppUserDetails;
import ru.yandex.practicum.mymarket.security.SecurityConfig;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebFluxTest(ImageController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ImageControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ItemService itemService;

    @Test
    void image_anonymous_returnsPngBytes() {
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
    void image_authenticated_returnsPngBytes() {
        User domainUser = new User();
        domainUser.setId(1L);
        domainUser.setUsername("user1");
        domainUser.setPassword("testPwd");
        domainUser.setAccountId(1L);
        when(itemService.getImage(1L)).thenReturn(Mono.just(new byte[]{1, 2, 3, 4}));

        webTestClient.mutateWith(mockUser(new AppUserDetails(domainUser)))
                .get().uri("/images/1").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG);
    }

    @Test
    void image_whenItemNotFound_returnsNotFound() {
        when(itemService.getImage(99L)).thenReturn(Mono.empty());

        webTestClient.get().uri("/images/99").exchange()
                .expectStatus().isNotFound();
    }
}
