package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.AccountSummary;
import ru.yandex.practicum.accounts.dto.BalanceChangeRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.util.List;

/**
 * REST API аккаунтов.
 * <p>
 * Endpoints делятся на две группы:
 * <ul>
 *   <li>Пользовательские — работают с текущим пользователем, логин берётся
 *       из JWT claim {@code preferred_username};</li>
 *   <li>Сервисные — принимают явный {@code {login}} в пути, вызываются
 *       cash/transfer-сервисами.</li>
 * </ul>
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** Мой аккаунт — логин из JWT текущего пользователя. */
    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getByLogin(currentLogin(jwt));
    }

    /** Аккаунт по логину (для межсервисных вызовов). */
    @GetMapping("/{login}")
    public AccountResponse getByLogin(@PathVariable String login) {
        return accountService.getByLogin(login);
    }

    /** Список остальных аккаунтов для выбора получателя перевода. */
    @GetMapping
    public List<AccountSummary> listOthers(@AuthenticationPrincipal Jwt jwt) {
        return accountService.listOthers(currentLogin(jwt));
    }

    /** Редактирование профиля текущего пользователя. */
    @PutMapping("/me")
    public AccountResponse updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return accountService.update(currentLogin(jwt), request);
    }

    /** Пополнение счёта. Вызывается cash-service'ом. */
    @PostMapping("/{login}/deposit")
    public AccountResponse deposit(
            @PathVariable String login,
            @Valid @RequestBody BalanceChangeRequest request
    ) {
        return accountService.deposit(login, request);
    }

    /** Снятие со счёта. Вызывается cash-service и transfer-service. */
    @PostMapping("/{login}/withdraw")
    public AccountResponse withdraw(
            @PathVariable String login,
            @Valid @RequestBody BalanceChangeRequest request
    ) {
        return accountService.withdraw(login, request);
    }

    /**
     * Логин вошедшего пользователя.
     * <p>
     * Keycloak кладёт его в claim {@code preferred_username}.
     * Fallback на {@code sub} на случай нестандартной конфигурации realm'а.
     */
    private String currentLogin(Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");
        return login != null ? login : jwt.getSubject();
    }
}
