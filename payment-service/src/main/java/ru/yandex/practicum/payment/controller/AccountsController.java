package ru.yandex.practicum.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.AccountsApi;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentRequest;
import ru.yandex.practicum.payment.dto.PaymentResponse;
import ru.yandex.practicum.payment.exception.AccountNotFoundException;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.service.PaymentService;

@RestController
public class AccountsController implements AccountsApi {

    private final PaymentService paymentService;

    public AccountsController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> createAccount(Long accountId, ServerWebExchange exchange) {
        return paymentService.createAccount(accountId)
                .map(balance -> ResponseEntity.status(HttpStatus.CREATED).body(new BalanceResponse(balance)));
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(Long accountId, ServerWebExchange exchange) {
        return paymentService.getBalance(accountId)
                .map(balance -> ResponseEntity.ok(new BalanceResponse(balance)))
                .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> makePayment(Long accountId, Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange) {
        return paymentRequest.flatMap(request -> paymentService.pay(accountId, request.getAmount())
                .map(outcome -> switch (outcome.status()) {
                    case SUCCESS -> ResponseEntity.ok(new PaymentResponse(outcome.balance()));
                    case INSUFFICIENT_FUNDS -> throw new InsufficientFundsException(accountId);
                    case ACCOUNT_NOT_FOUND -> throw new AccountNotFoundException(accountId);
                }));
    }
}
