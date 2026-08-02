package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.security.SecurityConfig;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.service.UserService.RegistrationResult;

@WebFluxTest(RegistrationController.class)
@Import({SecurityConfig.class, SecurityTestConfig.class})
class RegistrationControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    UserService userService;

    @Test
    void registrationForm_anonymous_returnsForm() {
        webTestClient.get().uri("/register").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Регистрация")));
    }

    @Test
    void register_success_redirectsToLoginWithFlag() {
        when(userService.register("user3", "pwd3")).thenReturn(Mono.just(RegistrationResult.SUCCESS));

        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .body(BodyInserters.fromFormData("username", "user3")
                        .with("password", "pwd3")
                        .with("confirmPassword", "pwd3"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login?registered");
    }

    @Test
    void register_whenPasswordsDiffer_showsErrorWithoutServiceCall() {
        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .body(BodyInserters.fromFormData("username", "user3")
                        .with("password", "pwd3")
                        .with("confirmPassword", "other"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Пароли не совпадают")));

        verifyNoInteractions(userService);
    }

    @Test
    void register_whenBlankFields_showsErrorWithoutServiceCall() {
        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .body(BodyInserters.fromFormData("username", " ")
                        .with("password", "")
                        .with("confirmPassword", ""))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Заполните логин и пароль")));

        verifyNoInteractions(userService);
    }

    @Test
    void register_whenUsernameTaken_showsError() {
        when(userService.register("user1", "pwd")).thenReturn(Mono.just(RegistrationResult.USERNAME_TAKEN));

        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .body(BodyInserters.fromFormData("username", "user1")
                        .with("password", "pwd")
                        .with("confirmPassword", "pwd"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Логин уже занят")));
    }

    @Test
    void register_whenPaymentUnavailable_showsError() {
        when(userService.register("user3", "pwd")).thenReturn(Mono.just(RegistrationResult.PAYMENT_UNAVAILABLE));

        webTestClient.mutateWith(csrf())
                .post().uri("/register")
                .body(BodyInserters.fromFormData("username", "user3")
                        .with("password", "pwd")
                        .with("confirmPassword", "pwd"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("Регистрация временно недоступна")));
    }

    @Test
    void register_withoutCsrfToken_isForbidden() {
        webTestClient.post().uri("/register")
                .body(BodyInserters.fromFormData("username", "user3")
                        .with("password", "pwd")
                        .with("confirmPassword", "pwd"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
