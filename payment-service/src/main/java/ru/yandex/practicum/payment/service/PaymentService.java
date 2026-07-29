package ru.yandex.practicum.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.exception.AccountAlreadyExistsException;
import ru.yandex.practicum.payment.domain.Account;
import ru.yandex.practicum.payment.repository.AccountRepository;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final long defaultBalance;

    public PaymentService(AccountRepository accountRepository,
                          @Value("${app.account.default-balance}") long defaultBalance) {
        this.accountRepository = accountRepository;
        this.defaultBalance = defaultBalance;
    }

    public Mono<Long> createAccount(Long accountId) {
        return accountRepository.insert(accountId, defaultBalance)
                .onErrorMap(DataIntegrityViolationException.class,
                        e -> new AccountAlreadyExistsException(accountId))
                .thenReturn(defaultBalance);
    }

    public Mono<Long> getBalance(Long accountId) {
        return accountRepository.findById(accountId).map(Account::getBalance);
    }

    public Mono<PaymentOutcome> pay(Long accountId, long amount) {
        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    if (account.getBalance() < amount) {
                        return Mono.just(PaymentOutcome.insufficientFunds());
                    }
                    return accountRepository.deduct(accountId, amount)
                            .map(rows -> rows > 0
                                    ? PaymentOutcome.success(account.getBalance() - amount)
                                    : PaymentOutcome.insufficientFunds());
                })
                .switchIfEmpty(Mono.just(PaymentOutcome.accountNotFound()));
    }

    public record PaymentOutcome(Status status, long balance) {

        public enum Status {
            SUCCESS,
            INSUFFICIENT_FUNDS,
            ACCOUNT_NOT_FOUND
        }

        public static PaymentOutcome success(long balance) {
            return new PaymentOutcome(Status.SUCCESS, balance);
        }

        public static PaymentOutcome insufficientFunds() {
            return new PaymentOutcome(Status.INSUFFICIENT_FUNDS, 0);
        }

        public static PaymentOutcome accountNotFound() {
            return new PaymentOutcome(Status.ACCOUNT_NOT_FOUND, 0);
        }
    }
}
