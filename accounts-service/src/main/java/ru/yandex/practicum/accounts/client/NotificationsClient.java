package ru.yandex.practicum.accounts.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * REST-клиент к сервису уведомлений.
 * <p>
 * Вызов идёт напрямую (не через Gateway) — так требует паттерн
 * inter-service communication в микросервисах. Discovery через Eureka
 * (URL {@code lb://notifications-service}) + bearer token от Keycloak
 * по Client Credentials Flow (см. OAuth2RestClientConfig).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private final RestClient notificationsRestClient;

    @Value("${services.notifications.url}")
    private String notificationsUrl;

    public void sendNotification(String login, String message) {
        try {
            notificationsRestClient.post()
                    .uri(notificationsUrl + "/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("login", login, "message", message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Уведомления не должны ломать основную операцию.
            log.warn("Не удалось отправить уведомление для {}: {}", login, e.getMessage());
        }
    }
}
