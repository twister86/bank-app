package ru.yandex.practicum.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * Запрос на редактирование профиля (имя + дата рождения).
 * Бизнес-валидация "старше 18 лет" — в сервисе (JSR-380 не умеет
 * выражать "сегодня минус 18 лет" без кастомного аннотатора).
 */
public record UpdateAccountRequest(
        @NotBlank(message = "Имя обязательно")
        String name,

        @NotNull(message = "Дата рождения обязательна")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthdate
) {
}
