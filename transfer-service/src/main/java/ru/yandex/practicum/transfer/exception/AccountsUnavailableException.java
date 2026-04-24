package ru.yandex.practicum.transfer.exception;

/**
 * Выбрасывается, когда accounts-service недоступен (все retry исчерпаны
 * или открыт circuit breaker). Обрабатывается в GlobalExceptionHandler
 * как HTTP 503.
 */
public class AccountsUnavailableException extends RuntimeException {
    public AccountsUnavailableException(String message) {
        super(message);
    }
}
