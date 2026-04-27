package ru.yandex.practicum.accounts.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Запрос на изменение баланса (пополнение/снятие/перевод).
 * Вызывается cash-service и transfer-service.
 *
 * @param amount положительная сумма; знак операции определяется эндпоинтом.
 */
public record BalanceChangeRequest(
        @NotNull
        @Positive(message = "Сумма должна быть положительной")
        BigDecimal amount
) {
}
