package ru.yandex.practicum.mymarket.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

    public Mono<BalanceResult> getBalance() {
        return paymentApi.getBalance(accountId)
                .map(response -> BalanceResult.available(response.getBalance()))
                .onErrorResume(WebClientResponseException.class, e -> Mono.just(
                        e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                                ? BalanceResult.accountNotFound()
                                : BalanceResult.unavailable()))
                .onErrorReturn(BalanceResult.unavailable());
    }

    public Mono<PaymentResult> pay(long amount) {
        return paymentApi.makePayment(accountId, new PaymentRequest().amount(amount))
                .thenReturn(PaymentResult.SUCCESS)
                .onErrorResume(WebClientResponseException.class, e -> {
                    HttpStatusCode status = e.getStatusCode();
                    if (status.isSameCodeAs(HttpStatus.UNPROCESSABLE_CONTENT)) {
                        return Mono.just(PaymentResult.INSUFFICIENT_FUNDS);
                    }
                    if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Mono.just(PaymentResult.ACCOUNT_NOT_FOUND);
                    }
                    return Mono.just(PaymentResult.UNAVAILABLE);
                })
                .onErrorReturn(PaymentResult.UNAVAILABLE);
    }

    public enum PaymentResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        ACCOUNT_NOT_FOUND,
        UNAVAILABLE
    }

    public record BalanceResult(Status status, long balance) {

        public enum Status {
            AVAILABLE,
            ACCOUNT_NOT_FOUND,
            UNAVAILABLE
        }

        public static BalanceResult available(long balance) {
            return new BalanceResult(Status.AVAILABLE, balance);
        }

        public static BalanceResult accountNotFound() {
            return new BalanceResult(Status.ACCOUNT_NOT_FOUND, 0);
        }

        public static BalanceResult unavailable() {
            return new BalanceResult(Status.UNAVAILABLE, 0);
        }
    }
}
