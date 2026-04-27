package ru.yandex.practicum.mybankfront.exception;

public class GatewayApiException extends RuntimeException {
    private final int status;

    public GatewayApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
