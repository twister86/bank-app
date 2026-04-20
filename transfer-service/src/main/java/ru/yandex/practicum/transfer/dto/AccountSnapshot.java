package ru.yandex.practicum.transfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountSnapshot(
        String login,
        String name,
        LocalDate birthdate,
        BigDecimal balance
) {
}
