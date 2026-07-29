package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.User;
import ru.yandex.practicum.mymarket.dto.Action;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto;
import ru.yandex.practicum.mymarket.dto.ItemsPageDto.PagingDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.security.AppUserDetails;
import ru.yandex.practicum.mymarket.security.SecurityConfig;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@WebFluxTest(ItemController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class ItemControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ItemService itemService;
    @MockitoBean
    CartService cartService;

    AppUserDetails user;

    @BeforeEach
    void setUp() {
        User domainUser = new User();
        domainUser.setId(1L);
        domainUser.setUsername("user1");
        domainUser.setPassword("testPwd");
        domainUser.setAccountId(1L);
        user = new AppUserDetails(domainUser);
    }

    private static ItemsPageDto page(ItemDto item) {
        return new ItemsPageDto(List.of(List.of(item)), new PagingDto(5, 1, false, true));
    }

    @Test
    void items_anonymous_showsItemsWithoutCartControls() {
        ItemDto item = new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 0);
        when(itemService.getItems(isNull(), any(), any(), anyInt(), anyInt())).thenReturn(Mono.just(page(item)));

        webTestClient.get().uri("/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> {
                    assertTrue(html.contains("Кепка"));
                    assertFalse(html.contains("href=\"/cart/items\""));
                    assertTrue(html.contains("href=\"/login\""));
                });
    }

    @Test
    void items_authenticated_showsCartControls() {
        ItemDto item = new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 0);
        when(itemService.getItems(eq(1L), any(), any(), anyInt(), anyInt())).thenReturn(Mono.just(page(item)));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> {
                    assertTrue(html.contains("Кепка"));
                    assertTrue(html.contains("href=\"/cart/items\""));
                    assertFalse(html.contains("href=\"/login\""));
                });
    }

    @Test
    void changeCountFromList_redirectsToItems() {
        when(cartService.changeCount(1L, 1L, Action.PLUS)).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser(user)).mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "")
                        .with("sort", "NO")
                        .with("pageNumber", "1")
                        .with("pageSize", "5"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", "/items\\?.*");

        verify(cartService).changeCount(1L, 1L, Action.PLUS);
    }

    @Test
    void changeCountFromList_anonymous_redirectsToLogin() {
        webTestClient.mutateWith(csrf())
                .post().uri("/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void item_anonymous_returnsItemView() {
        when(itemService.getItem(1L, null))
                .thenReturn(Mono.just(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 0)));

        webTestClient.get().uri("/items/1").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));
    }

    @Test
    void changeCountFromCard_rerendersItemView() {
        when(cartService.changeCount(1L, 1L, Action.MINUS)).thenReturn(Mono.empty());
        when(itemService.getItem(1L, 1L))
                .thenReturn(Mono.just(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 1)));

        webTestClient.mutateWith(mockUser(user)).mutateWith(csrf())
                .post().uri("/items/1")
                .body(BodyInserters.fromFormData("action", "MINUS"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));

        verify(cartService).changeCount(1L, 1L, Action.MINUS);
    }

    @Test
    void changeCountFromCard_withoutCsrfToken_isForbidden() {
        webTestClient.mutateWith(mockUser(user))
                .post().uri("/items/1")
                .body(BodyInserters.fromFormData("action", "MINUS"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void item_whenNotFound_returns404() {
        when(itemService.getItem(999L, null))
                .thenReturn(Mono.error(new NotFoundException(NotFoundException.Resource.ITEM, 999L)));

        webTestClient.get().uri("/items/999").exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Товар не найден")));
    }
}
