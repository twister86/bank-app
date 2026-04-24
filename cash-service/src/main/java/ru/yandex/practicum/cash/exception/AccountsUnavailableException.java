package ru.yandex.practicum.cash.exception;

/**
 * Выбрасывается из fallback-метода {@code AccountsClient.onAccountsUnavailable},
 * когда accounts-service недоступен (сетевые проблемы, таймаут, открытый
 * circuit breaker). Обрабатывается в {@code GlobalExceptionHandler} как
 * HTTP 503 Service Unavailable.
 */
public class AccountsUnavailableException extends RuntimeException {
    public AccountsUnavailableException(String message) {
        super(message);
    }
}
