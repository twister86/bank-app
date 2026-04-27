package ru.yandex.practicum.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Получатель обязателен")
        String toLogin,

        @NotNull
        @Positive(message = "Сумма должна быть положительной")
        BigDecimal amount
) {
}
