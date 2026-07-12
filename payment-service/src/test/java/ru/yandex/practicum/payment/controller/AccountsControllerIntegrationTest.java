package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.domain.Account;
import ru.yandex.practicum.payment.dto.PaymentRequest;
import ru.yandex.practicum.payment.repository.AccountRepository;

@SpringBootTest
@AutoConfigureWebTestClient
class AccountsControllerIntegrationTest {

    @Autowired
    WebTestClient webTestClient;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    R2dbcEntityTemplate template;

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
    void getBalance_returnsSeededBalance() {
        webTestClient.get().uri("/accounts/1/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(100000);
    }

    @Test
    void getBalance_whenAccountMissing_returns404() {
        webTestClient.get().uri("/accounts/999/balance")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("ACCOUNT_NOT_FOUND")
                .jsonPath("$.message").exists();
    }

    @Test
    void makePayment_whenEnough_deductsAndReturnsNewBalance() {
        webTestClient.post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(500L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(99500);
    }

    @Test
    void makePayment_whenNotEnough_returns422() {
        webTestClient.post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(200000L))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INSUFFICIENT_FUNDS")
                .jsonPath("$.message").exists();
    }

    @Test
    void makePayment_whenAccountMissing_returns404() {
        webTestClient.post().uri("/accounts/999/payment")
                .bodyValue(new PaymentRequest(500L))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void makePayment_whenAmountNotPositive_returns400() {
        webTestClient.post().uri("/accounts/1/payment")
                .bodyValue(new PaymentRequest(0L))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
