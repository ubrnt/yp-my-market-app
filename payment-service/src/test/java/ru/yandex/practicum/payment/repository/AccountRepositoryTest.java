package ru.yandex.practicum.payment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.domain.Account;

@DataR2dbcTest
@ActiveProfiles("repo-test")
class AccountRepositoryTest {

    @Autowired
    AccountRepository accountRepository;
    @Autowired
    R2dbcEntityTemplate template;

    @BeforeEach
    void cleanDatabase() {
        StepVerifier.create(accountRepository.deleteAll()).verifyComplete();
    }

    private void insertAccount(Long id, long balance) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(balance);
        template.insert(account).block();
    }

    @Test
    void deduct_whenEnough_subtractsAndReturnsOneRow() {
        insertAccount(1L, 100L);

        StepVerifier.create(accountRepository.deduct(1L, 30L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(accountRepository.findById(1L))
                .assertNext(account -> assertEquals(70L, account.getBalance()))
                .verifyComplete();
    }

    @Test
    void deduct_whenNotEnough_leavesBalanceAndReturnsZeroRows() {
        insertAccount(1L, 100L);

        StepVerifier.create(accountRepository.deduct(1L, 200L))
                .expectNext(0L)
                .verifyComplete();

        StepVerifier.create(accountRepository.findById(1L))
                .assertNext(account -> assertEquals(100L, account.getBalance()))
                .verifyComplete();
    }

    @Test
    void deduct_whenAccountMissing_returnsZeroRows() {
        StepVerifier.create(accountRepository.deduct(999L, 10L))
                .expectNext(0L)
                .verifyComplete();
    }
}
