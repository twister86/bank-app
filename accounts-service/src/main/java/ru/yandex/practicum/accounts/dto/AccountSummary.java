package ru.yandex.practicum.accounts.dto;

/**
 * Укороченные данные аккаунта для выбора получателя перевода.
 * Не содержит баланс и ПДн (дата рождения) — виден всем авторизованным.
 */
public record AccountSummary(
        String login,
        String name
) {
}
