package ru.yandex.practicum.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.domain.Account;
import ru.yandex.practicum.payment.repository.AccountRepository;
import ru.yandex.practicum.payment.service.PaymentService.PaymentOutcome.Status;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    AccountRepository accountRepository;

    PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(accountRepository);
    }

    @Test
    void getBalance_returnsBalance() {
        when(accountRepository.findById(1L)).thenReturn(Mono.just(account(1L, 100L)));

        StepVerifier.create(paymentService.getBalance(1L))
                .expectNext(100L)
                .verifyComplete();
    }

    @Test
    void getBalance_whenAccountMissing_returnsEmpty() {
        when(accountRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void pay_whenEnough_deductsAndReturnsSuccessWithNewBalance() {
        when(accountRepository.findById(1L)).thenReturn(Mono.just(account(1L, 100L)));
        when(accountRepository.deduct(1L, 30L)).thenReturn(Mono.just(1L));

        StepVerifier.create(paymentService.pay(1L, 30L))
                .assertNext(outcome -> {
                    assertEquals(Status.SUCCESS, outcome.status());
                    assertEquals(70L, outcome.balance());
                })
                .verifyComplete();
    }

    @Test
    void pay_whenNotEnough_returnsInsufficientWithoutDeducting() {
        when(accountRepository.findById(1L)).thenReturn(Mono.just(account(1L, 100L)));

        StepVerifier.create(paymentService.pay(1L, 200L))
                .assertNext(outcome -> assertEquals(Status.INSUFFICIENT_FUNDS, outcome.status()))
                .verifyComplete();

        verify(accountRepository, never()).deduct(any(), any());
    }

    @Test
    void pay_whenAccountMissing_returnsAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.pay(1L, 30L))
                .assertNext(outcome -> assertEquals(Status.ACCOUNT_NOT_FOUND, outcome.status()))
                .verifyComplete();
    }

    @Test
    void pay_whenRacedToZeroRows_returnsInsufficient() {
        when(accountRepository.findById(1L)).thenReturn(Mono.just(account(1L, 100L)));
        when(accountRepository.deduct(1L, 30L)).thenReturn(Mono.just(0L));

        StepVerifier.create(paymentService.pay(1L, 30L))
                .assertNext(outcome -> assertEquals(Status.INSUFFICIENT_FUNDS, outcome.status()))
                .verifyComplete();
    }

    private static Account account(Long id, long balance) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(balance);
        return account;
    }
}
