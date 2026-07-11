package ru.yandex.practicum.mymarket.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.payment.api.DefaultApi;
import ru.yandex.practicum.mymarket.payment.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.payment.dto.PaymentRequest;

@Component
public class PaymentServiceClient {

    private final DefaultApi paymentApi;
    private final long accountId;

    public PaymentServiceClient(DefaultApi paymentApi, @Value("${app.payment.account-id}") long accountId) {
        this.paymentApi = paymentApi;
        this.accountId = accountId;
    }

    public Mono<Long> getBalance() {
        return paymentApi.getBalance(accountId).map(BalanceResponse::getBalance);
    }

    public Mono<PaymentResult> pay(long amount) {
        return paymentApi.makePayment(accountId, new PaymentRequest().amount(amount))
                .thenReturn(PaymentResult.SUCCESS)
                .onErrorResume(WebClientResponseException.class, e -> e.getStatusCode().value() == 422
                        ? Mono.just(PaymentResult.INSUFFICIENT_FUNDS)
                        : Mono.just(PaymentResult.UNAVAILABLE))
                .onErrorReturn(PaymentResult.UNAVAILABLE);
    }

    public enum PaymentResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        UNAVAILABLE
    }
}
