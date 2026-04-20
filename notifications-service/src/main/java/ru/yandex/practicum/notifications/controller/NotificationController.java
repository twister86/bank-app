package ru.yandex.practicum.notifications.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.notifications.dto.NotificationRequest;

/**
 * Принимает уведомления от accounts/cash/transfer и пишет их в лог.
 * По ТЗ реализация уведомления — на выбор (лог/почта/alert); выбран
 * наиболее простой и воспроизводимый вариант — лог.
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void notify(@Valid @RequestBody NotificationRequest request) {
        log.info("[NOTIFICATION] user={} msg={}", request.login(), request.message());
    }
}
