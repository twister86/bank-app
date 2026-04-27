package ru.yandex.practicum.transfer.exception;

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
