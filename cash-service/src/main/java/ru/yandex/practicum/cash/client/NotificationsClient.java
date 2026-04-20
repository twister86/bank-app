package ru.yandex.practicum.cash.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

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
            log.warn("Notifications call failed for {}: {}", login, e.getMessage());
        }
    }
}
