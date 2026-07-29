package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.domain.User;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.exception.NotFoundException;
import ru.yandex.practicum.mymarket.security.AppUserDetails;
import ru.yandex.practicum.mymarket.security.SecurityConfig;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebFluxTest(OrderController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class OrderControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    OrderService orderService;

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

    private static OrderDto order(long id) {
        return new OrderDto(id, List.of(new OrderItemDto(1L, "Мяч", 990L, 1)), 990L);
    }

    @Test
    void orders_returnsOrdersView() {
        when(orderService.getOrders(1L)).thenReturn(Flux.just(order(7L)));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/orders").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заказ №7")));
    }

    @Test
    void orders_anonymous_redirectsToLogin() {
        webTestClient.get().uri("/orders").exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void order_returnsOrderView() {
        when(orderService.getOrder(7L, 1L)).thenReturn(Mono.just(order(7L)));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/orders/7").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заказ №7")));
    }

    @Test
    void orders_requestsOrdersOfAuthenticatedUser() {
        AppUserDetails otherUser = otherUser(42L);
        when(orderService.getOrders(42L)).thenReturn(Flux.empty());

        webTestClient.mutateWith(mockUser(otherUser))
                .get().uri("/orders").exchange()
                .expectStatus().isOk();

        verify(orderService).getOrders(42L);
    }

    @Test
    void order_ofAnotherUser_returns404() {
        AppUserDetails otherUser = otherUser(42L);
        when(orderService.getOrder(7L, 42L))
                .thenReturn(Mono.error(new NotFoundException(NotFoundException.Resource.ORDER, 7L)));

        webTestClient.mutateWith(mockUser(otherUser))
                .get().uri("/orders/7").exchange()
                .expectStatus().isNotFound();

        verify(orderService).getOrder(7L, 42L);
    }

    private static AppUserDetails otherUser(long id) {
        User domainUser = new User();
        domainUser.setId(id);
        domainUser.setUsername("user" + id);
        domainUser.setPassword("testPwd");
        domainUser.setAccountId(id);
        return new AppUserDetails(domainUser);
    }

    @Test
    void order_whenNotFound_returns404() {
        when(orderService.getOrder(99L, 1L))
                .thenReturn(Mono.error(new NotFoundException(NotFoundException.Resource.ORDER, 99L)));

        webTestClient.mutateWith(mockUser(user))
                .get().uri("/orders/99").exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заказ не найден")));
    }

    @Test
    void buy_whenSuccess_redirectsToNewOrder() {
        when(orderService.buy(1L, 1L)).thenReturn(Mono.just(7L));

        webTestClient.mutateWith(mockUser(user)).mutateWith(csrf())
                .post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/7?newOrder=true");
    }

    @Test
    void buy_whenPaymentFails_redirectsToCart() {
        when(orderService.buy(1L, 1L)).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockUser(user)).mutateWith(csrf())
                .post().uri("/buy").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");
    }

    @Test
    void buy_anonymous_redirectsToLogin() {
        webTestClient.mutateWith(csrf())
                .post().uri("/buy").exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }
}
