package ru.yandex.practicum.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * REST-клиент к notifications-service. Логика та же, что в
 * {@code ru.yandex.practicum.cash.client.NotificationsClient}: уведомления —
 * "best-effort", при падении notifications основная операция
 * (в нашем случае перевод) всё равно успешна, мы просто пишем в лог.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private static final String CIRCUIT = "notificationsService";

    private final RestClient notificationsRestClient;

    @Value("${services.notifications.url}")
    private String notificationsUrl;

    @Retry(name = CIRCUIT)
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "onNotificationsUnavailable")
    public void sendNotification(String login, String message) {
        notificationsRestClient.post()
                .uri(notificationsUrl + "/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "message", message))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unused") // вызывается рефлексией Resilience4j
    private void onNotificationsUnavailable(String login, String message, Throwable ex) {
        log.warn("Notifications-service недоступен, уведомление потеряно "
                + "(login={}, message={}): {}", login, message, ex.toString());
    }
}
