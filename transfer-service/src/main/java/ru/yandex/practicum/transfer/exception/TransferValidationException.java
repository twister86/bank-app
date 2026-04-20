package ru.yandex.practicum.transfer.exception;

public class TransferValidationException extends RuntimeException {
    public TransferValidationException(String message) {
        super(message);
    }
}
