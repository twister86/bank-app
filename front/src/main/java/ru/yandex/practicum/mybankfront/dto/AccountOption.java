package ru.yandex.practicum.mybankfront.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Пара логин+имя для списка получателей перевода.
 * Важно: field-names {@code login} и {@code name} используются в main.html
 * через {@code account.login} / {@code account.name} — переименовывать нельзя.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountOption(String login, String name) {
}
