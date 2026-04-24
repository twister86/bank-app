package ru.yandex.practicum.cash.client;

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
 * REST-клиент к notifications-service.
 * <p>
 * В отличие от {@link AccountsClient}, уведомления считаются "best-effort":
 * если notifications упал — основная операция (пополнение / перевод) должна
 * пройти успешно. Поэтому fallback просто пишет в лог, не выкидывая ошибку.
 * <p>
 * {@code @Retry} даёт 3 попытки при временных ошибках; {@code @CircuitBreaker}
 * бережёт ресурсы — при массовом падении notifications-service перестаёт
 * стучаться на 30 секунд.
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

    /**
     * Fallback: уведомления некритичны, поэтому ошибку НЕ пробрасываем.
     * В настоящем бою здесь был бы outbox-паттерн — положить в очередь и
     * повторить позже. Для учебного проекта достаточно лога.
     */
    @SuppressWarnings("unused") // вызывается рефлексией Resilience4j
    private void onNotificationsUnavailable(String login, String message, Throwable ex) {
        log.warn("Notifications-service недоступен, уведомление потеряно "
                + "(login={}, message={}): {}", login, message, ex.toString());
    }
}
