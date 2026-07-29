package ru.yandex.practicum.payment.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.domain.Account;

public interface AccountRepository extends R2dbcRepository<Account, Long> {

    @Modifying
    @Query("UPDATE accounts SET balance = balance - :amount WHERE id = :id AND balance >= :amount")
    Mono<Long> deduct(@Param("id") Long id, @Param("amount") Long amount);

    @Modifying
    @Query("INSERT INTO accounts (id, balance) VALUES (:id, :balance)")
    Mono<Long> insert(@Param("id") Long id, @Param("balance") Long balance);
}
