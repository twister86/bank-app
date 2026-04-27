package ru.yandex.practicum.cash.exception;

/** Проброс ошибок клиента accounts (например, 409 insufficient funds). */
public class AccountsOperationFailedException extends RuntimeException {
    private final int status;

    public AccountsOperationFailedException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
