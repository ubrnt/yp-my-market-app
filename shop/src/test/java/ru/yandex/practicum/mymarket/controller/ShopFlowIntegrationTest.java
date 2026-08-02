package ru.yandex.practicum.mymarket.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.AbstractIntegrationTest;

class ShopFlowIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Autowired
    WebTestClient webTestClient;

    private record Session(String cookie) {
    }

    private Session login(String username, String password) {
        EntityExchangeResult<String> loginPage = webTestClient.get().uri("/login").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        String anonymousSession = loginPage.getResponseCookies().getFirst("SESSION").getValue();
        String csrf = extractCsrf(loginPage.getResponseBody());

        EntityExchangeResult<Void> loginResult = webTestClient.post().uri("/login")
                .cookie("SESSION", anonymousSession)
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password)
                        .with("_csrf", csrf))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/")
                .expectBody(Void.class).returnResult();

        String authenticatedSession = loginResult.getResponseCookies().getFirst("SESSION").getValue();
        assertNotEquals(anonymousSession, authenticatedSession, "session id must change on login");

        return new Session(authenticatedSession);
    }

    private String csrfFor(Session session) {
        String html = webTestClient.get().uri("/items")
                .cookie("SESSION", session.cookie())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        return extractCsrf(html);
    }

    private static String extractCsrf(String html) {
        assertNotNull(html);
        Matcher matcher = CSRF_INPUT.matcher(html);
        assertTrue(matcher.find(), "csrf input not found in page");
        return matcher.group(1);
    }

    private void addFirstItemToCart(Session session) {
        Long itemId = itemRepository.findAll().blockFirst().getId();

        webTestClient.post().uri("/items")
                .cookie("SESSION", session.cookie())
                .body(BodyInserters.fromFormData("id", itemId.toString())
                        .with("action", "PLUS")
                        .with("search", "")
                        .with("sort", "NO")
                        .with("pageNumber", "1")
                        .with("pageSize", "5")
                        .with("_csrf", csrfFor(session)))
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    private URI buy(Session session) {
        URI orderLocation = webTestClient.post().uri("/buy")
                .cookie("SESSION", session.cookie())
                .body(BodyInserters.fromFormData("_csrf", csrfFor(session)))
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getLocation();

        assertNotNull(orderLocation);
        return orderLocation;
    }

    @Test
    void fullFlow_loginAddToCartBuy_thenOrderAppearsAndCartCleared() {
        Session user1 = login("user1", "password1");

        addFirstItemToCart(user1);
        assertEquals(1L, cartItemRepository.count().block());

        URI orderLocation = buy(user1);
        assertTrue(orderLocation.toString().matches("/orders/\\d+\\?newOrder=true"));

        assertEquals(1L, orderRepository.count().block());
        assertEquals(0L, cartItemRepository.count().block());

        webTestClient.get().uri("/orders")
                .cookie("SESSION", user1.cookie())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains(orderLocation.getPath())));
    }

    @Test
    void isolation_user2SeesNeitherForeignCartNorForeignOrder() {
        Session user1 = login("user1", "password1");
        addFirstItemToCart(user1);
        URI orderLocation = buy(user1);

        Session user2 = login("user2", "password2");

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION", user2.cookie())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertFalse(html.contains("card-title")));

        webTestClient.get().uri(orderLocation.getPath())
                .cookie("SESSION", user2.cookie())
                .exchange()
                .expectStatus().isNotFound();

        webTestClient.get().uri("/orders")
                .cookie("SESSION", user2.cookie())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> assertFalse(html.contains(orderLocation.getPath())));
    }

    @Test
    void logout_invalidatesSessionCompletely() {
        Session user1 = login("user1", "password1");

        webTestClient.post().uri("/logout")
                .cookie("SESSION", user1.cookie())
                .body(BodyInserters.fromFormData("_csrf", csrfFor(user1)))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login\\?logout");

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION", user1.cookie())
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void registration_thenLoginAndCartWorks() {
        EntityExchangeResult<String> registerPage = webTestClient.get().uri("/register").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        String session = registerPage.getResponseCookies().getFirst("SESSION").getValue();
        String csrf = extractCsrf(registerPage.getResponseBody());

        webTestClient.post().uri("/register")
                .cookie("SESSION", session)
                .body(BodyInserters.fromFormData("username", "user3")
                        .with("password", "password3")
                        .with("confirmPassword", "password3")
                        .with("_csrf", csrf))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login?registered");

        Session user3 = login("user3", "password3");
        addFirstItemToCart(user3);
        assertEquals(1L, cartItemRepository.count().block());
    }

    @Test
    void login_protectsAgainstSessionFixation() {
        EntityExchangeResult<String> loginPage = webTestClient.get().uri("/login").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        String fixedSession = loginPage.getResponseCookies().getFirst("SESSION").getValue();
        String csrf = extractCsrf(loginPage.getResponseBody());

        String authenticatedSession = webTestClient.post().uri("/login")
                .cookie("SESSION", fixedSession)
                .body(BodyInserters.fromFormData("username", "user1")
                        .with("password", "password1")
                        .with("_csrf", csrf))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectBody(Void.class).returnResult()
                .getResponseCookies().getFirst("SESSION").getValue();

        assertNotEquals(fixedSession, authenticatedSession);

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION", fixedSession)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void login_withWrongPassword_redirectsToError() {
        EntityExchangeResult<String> loginPage = webTestClient.get().uri("/login").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult();

        String session = loginPage.getResponseCookies().getFirst("SESSION").getValue();
        String csrf = extractCsrf(loginPage.getResponseBody());

        webTestClient.post().uri("/login")
                .cookie("SESSION", session)
                .body(BodyInserters.fromFormData("username", "user1")
                        .with("password", "wrong")
                        .with("_csrf", csrf))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login\\?error");
    }
}
