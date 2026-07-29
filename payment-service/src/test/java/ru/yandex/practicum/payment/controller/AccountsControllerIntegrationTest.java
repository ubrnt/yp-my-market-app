package ru.yandex.practicum.payment.controller;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.domain.Account;
import ru.yandex.practicum.payment.dto.PaymentRequest;
import ru.yandex.practicum.payment.repository.AccountRepository;

@SpringBootTest
@AutoConfigureWebTestClient
class AccountsControllerIntegrationTest {

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        MockServerConfigurer springSecurityMockServerConfigurer() {
            return SecurityMockServerConfigurers.springSecurity();
        }
    }

    @Autowired
    WebTestClient webTestClient;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    R2dbcEntityTemplate template;

    private static SecurityMockServerConfigurers.JwtMutator readJwt() {
        return mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_payment:read"));
    }

    private static SecurityMockServerConfigurers.JwtMutator writeJwt() {
        return mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_payment:write"));
    }

    @BeforeEach
    void resetAccount() {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(100000L);

        StepVerifier.create(accountRepository.deleteAll().then(template.insert(account)))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getBalance_withoutToken_returns401() {
        webTestClient.get().uri("/accounts/1/balance")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void makePayment_withoutToken_returns401() {
        webTestClient.post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(500L))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_withWriteScopeOnly_returns403() {
        webTestClient.mutateWith(writeJwt())
                .get().uri("/accounts/1/balance")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void makePayment_withReadScopeOnly_returns403() {
        webTestClient.mutateWith(readJwt())
                .post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(500L))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getBalance_returnsSeededBalance() {
        webTestClient.mutateWith(readJwt())
                .get().uri("/accounts/1/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(100000);
    }

    @Test
    void getBalance_whenAccountMissing_returns404() {
        webTestClient.mutateWith(readJwt())
                .get().uri("/accounts/999/balance")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCOUNT_NOT_FOUND")
                .jsonPath("$.message").exists();
    }

    @Test
    void makePayment_whenEnough_deductsAndReturnsNewBalance() {
        webTestClient.mutateWith(writeJwt())
                .post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(500L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(99500);
    }

    @Test
    void makePayment_whenNotEnough_returns422() {
        webTestClient.mutateWith(writeJwt())
                .post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(200000L))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS")
                .jsonPath("$.message").exists();
    }

    @Test
    void makePayment_whenAccountMissing_returns404() {
        webTestClient.mutateWith(writeJwt())
                .post().uri("/accounts/999/payment")
                .bodyValue(new PaymentRequest(500L))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void makePayment_whenAmountNotPositive_returns400() {
        webTestClient.mutateWith(writeJwt())
                .post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(0L))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
