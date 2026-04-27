package ru.yandex.practicum.accounts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Полные данные аккаунта (для владельца/сервисов).
 */
public record AccountResponse(
        String login,
        String name,
        LocalDate birthdate,
        BigDecimal balance
) {
}
