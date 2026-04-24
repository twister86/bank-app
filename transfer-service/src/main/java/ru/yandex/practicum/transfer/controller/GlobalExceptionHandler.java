package ru.yandex.practicum.transfer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.transfer.exception.AccountsOperationFailedException;
import ru.yandex.practicum.transfer.exception.AccountsUnavailableException;
import ru.yandex.practicum.transfer.exception.TransferValidationException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountsOperationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAccounts(AccountsOperationFailedException ex) {
        return build(HttpStatusCode.valueOf(ex.status()), ex.getMessage());
    }

    /**
     * Accounts-service недоступен (открыт circuit breaker либо все retry
     * исчерпаны). 503 Service Unavailable — клиенту сигнал попробовать позже.
     */
    @ExceptionHandler(AccountsUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(AccountsUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(TransferValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(TransferValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", message
        ));
    }
}
