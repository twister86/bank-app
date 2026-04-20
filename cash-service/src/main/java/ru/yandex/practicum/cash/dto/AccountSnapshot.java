package ru.yandex.practicum.cash.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Срез данных аккаунта, возвращаемый accounts-service'ом.
 * Отдельный тип (не импорт из accounts) — сервисы не должны делить классы.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountSnapshot(
        String login,
        String name,
        LocalDate birthdate,
        BigDecimal balance
) {
}
