package ru.yandex.practicum.mymarket.client;

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

    public PaymentServiceClient(DefaultApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    public Mono<BalanceResult> getBalance(long accountId) {
        return paymentApi.getBalance(accountId)
                .map(response -> BalanceResult.available(response.getBalance()))
                .onErrorResume(WebClientResponseException.class, e -> Mono.just(
                        e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)
                                ? BalanceResult.accountNotFound()
                                : BalanceResult.unavailable()))
                .onErrorReturn(BalanceResult.unavailable());
    }

    public Mono<CreateAccountResult> createAccount() {
        return paymentApi.createAccount()
                .map(response -> CreateAccountResult.created(response.getAccountId()))
                .onErrorReturn(CreateAccountResult.unavailable());
    }

    public Mono<PaymentResult> pay(long accountId, long amount) {
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

    public record CreateAccountResult(Status status, Long accountId) {

        public enum Status {
            CREATED,
            UNAVAILABLE
        }

        public static CreateAccountResult created(Long accountId) {
            return new CreateAccountResult(Status.CREATED, accountId);
        }

        public static CreateAccountResult unavailable() {
            return new CreateAccountResult(Status.UNAVAILABLE, null);
        }
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
