package ru.yandex.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.cash.dto.AccountSnapshot;
import ru.yandex.practicum.cash.dto.CashOperationRequest;
import ru.yandex.practicum.cash.service.CashService;

/**
 * Cash API. Логин берётся из JWT вошедшего пользователя, клиент
 * не может вносить/снимать деньги на чужой счёт.
 */
@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping
    public AccountSnapshot perform(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CashOperationRequest request
    ) {
        String login = jwt.getClaimAsString("preferred_username");
        if (login == null) {
            login = jwt.getSubject();
        }
        return cashService.perform(login, request);
    }
}
