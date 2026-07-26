package ru.yandex.practicum.payment.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.domain.Account;
import ru.yandex.practicum.payment.repository.AccountRepository;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;

    public PaymentService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
