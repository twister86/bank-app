package ru.yandex.practicum.accounts.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super("Недостаточно средств на счету");
    }
}
