package ru.yandex.practicum.mybankfront.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountView(
        String login,
        String name,
        LocalDate birthdate,
        BigDecimal balance
) {
}
