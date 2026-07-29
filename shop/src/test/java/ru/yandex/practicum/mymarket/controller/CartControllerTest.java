package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
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
import ru.yandex.practicum.mymarket.dto.CartDto;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.security.AppUserDetails;
import ru.yandex.practicum.mymarket.security.SecurityConfig;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CartService.CheckoutState;

@WebFluxTest(CartController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class CartControllerTest {

    @Autowired
    WebTestClient webTestClient;

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

        when(cartService.checkoutState(anyLong(), anyLong())).thenReturn(Mono.just(CheckoutState.OK));
    }

    @Test
    void cart_returnsCartViewWithItems() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Кепка")));
    }

    @Test
    void cart_whenNotEnoughBalance_showsInsufficientMessage() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));
        when(cartService.checkoutState(anyLong(), anyLong())).thenReturn(Mono.just(CheckoutState.INSUFFICIENT_FUNDS));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Недостаточно средств")));
    }

    @Test
    void cart_whenAccountNotFound_showsAccountNotFoundMessage() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));
        when(cartService.checkoutState(anyLong(), anyLong())).thenReturn(Mono.just(CheckoutState.ACCOUNT_NOT_FOUND));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Счёт не найден")));
    }

    @Test
    void cart_whenServiceUnavailable_showsUnavailableMessage() {
        CartDto cart = new CartDto(
                List.of(new ItemDto(1L, "Кепка", "Чёрная кепка", "images/1", 990L, 2)), 1980L);
        when(cartService.getCart(1L)).thenReturn(Mono.just(cart));
        when(cartService.checkoutState(anyLong(), anyLong())).thenReturn(Mono.just(CheckoutState.UNAVAILABLE));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/cart/items").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Сервис оплаты недоступен")));
    }

    @Test
    void changeCount_rerendersCartView() {
        when(cartService.changeCount(1L, 1L, Action.DELETE)).thenReturn(Mono.empty());
        when(cartService.getCart(1L)).thenReturn(Mono.just(new CartDto(List.of(), 0L)));

        webTestClient.mutateWith(mockUser(user)).mutateWith(csrf())
                .post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "DELETE"))
                .exchange()
                .expectStatus().isOk();

        verify(cartService).changeCount(1L, 1L, Action.DELETE);
    }

    @Test
    void cart_requestsCartOfAuthenticatedUser() {
        User otherUser = new User();
        otherUser.setId(42L);
        otherUser.setUsername("user42");
        otherUser.setPassword("testPwd");
        otherUser.setAccountId(42L);
        when(cartService.getCart(42L)).thenReturn(Mono.just(new CartDto(List.of(), 0L)));

        webTestClient.mutateWith(mockUser(new AppUserDetails(otherUser)))
                .get().uri("/cart/items").exchange()
                .expectStatus().isOk();

        verify(cartService).getCart(42L);
    }

    @Test
    void cart_anonymous_redirectsToLogin() {
        webTestClient.get().uri("/cart/items").exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void changeCount_anonymous_redirectsToLogin() {
        webTestClient.mutateWith(csrf())
                .post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "DELETE"))
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void changeCount_withoutCsrfToken_isForbidden() {
        webTestClient.mutateWith(mockUser(user))
                .post().uri("/cart/items")
                .body(BodyInserters.fromFormData("id", "1").with("action", "DELETE"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
