package ru.yandex.practicum.mymarket.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.BalanceResult;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.PaymentResult;
import ru.yandex.practicum.mymarket.payment.api.DefaultApi;
import ru.yandex.practicum.mymarket.payment.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.payment.dto.PaymentRequest;
import ru.yandex.practicum.mymarket.payment.dto.PaymentResponse;

@ExtendWith(MockitoExtension.class)
class PaymentServiceClientUnitTest {

    @Mock
    DefaultApi paymentApi;

    PaymentServiceClient paymentServiceClient;

    @BeforeEach
    void setUp() {
        paymentServiceClient = new PaymentServiceClient(paymentApi);
    }

    @Test
    void getBalance_whenAvailable_returnsBalance() {
        when(paymentApi.getBalance(1L)).thenReturn(Mono.just(new BalanceResponse().balance(100L)));

        StepVerifier.create(paymentServiceClient.getBalance(1L))
                .assertNext(result -> {
                    assertEquals(BalanceResult.Status.AVAILABLE, result.status());
                    assertEquals(100L, result.balance());
                })
                .verifyComplete();
    }

    @Test
    void getBalance_when404_returnsNotFound() {
        when(paymentApi.getBalance(1L))
                .thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));

        StepVerifier.create(paymentServiceClient.getBalance(1L))
                .assertNext(result -> assertEquals(BalanceResult.Status.ACCOUNT_NOT_FOUND, result.status()))
                .verifyComplete();
    }

    @Test
    void getBalance_whenServiceDown_returnsUnavailable() {
        when(paymentApi.getBalance(1L)).thenReturn(Mono.error(new RuntimeException("connection refused")));

        StepVerifier.create(paymentServiceClient.getBalance(1L))
                .assertNext(result -> assertEquals(BalanceResult.Status.UNAVAILABLE, result.status()))
                .verifyComplete();
    }

    @Test
    void pay_whenSuccess_returnsSuccess() {
        when(paymentApi.makePayment(eq(1L), any(PaymentRequest.class)))
                .thenReturn(Mono.just(new PaymentResponse().balance(50L)));

        StepVerifier.create(paymentServiceClient.pay(1L, 50L))
                .expectNext(PaymentResult.SUCCESS)
                .verifyComplete();
    }

    @Test
    void pay_when422_returnsInsufficientFunds() {
        when(paymentApi.makePayment(eq(1L), any(PaymentRequest.class)))
                .thenReturn(Mono.error(new WebClientResponseException(422, "Unprocessable Content", null, null, null)));

        StepVerifier.create(paymentServiceClient.pay(1L, 200_000L))
                .expectNext(PaymentResult.INSUFFICIENT_FUNDS)
                .verifyComplete();
    }

    @Test
    void pay_when404_returnsAccountNotFound() {
        when(paymentApi.makePayment(eq(1L), any(PaymentRequest.class)))
                .thenReturn(Mono.error(new WebClientResponseException(404, "Not Found", null, null, null)));

        StepVerifier.create(paymentServiceClient.pay(1L, 50L))
                .expectNext(PaymentResult.ACCOUNT_NOT_FOUND)
                .verifyComplete();
    }

    @Test
    void pay_whenServiceDown_returnsUnavailable() {
        when(paymentApi.makePayment(eq(1L), any(PaymentRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("connection refused")));

        StepVerifier.create(paymentServiceClient.pay(1L, 50L))
                .expectNext(PaymentResult.UNAVAILABLE)
                .verifyComplete();
    }
}
