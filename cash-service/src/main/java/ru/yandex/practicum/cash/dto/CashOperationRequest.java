package ru.yandex.practicum.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashOperationRequest(
        @NotNull
        @Positive(message = "Сумма должна быть положительной")
        BigDecimal amount,

        @NotNull
        CashAction action
) {
}
