package ru.yandex.practicum.payment.exception;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException(Long accountId) {
        super("Account " + accountId + " already exists");
    }
}
