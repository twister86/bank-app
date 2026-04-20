package ru.yandex.practicum.mybankfront.controller;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybankfront.client.BankApiClient;
import ru.yandex.practicum.mybankfront.dto.AccountOption;
import ru.yandex.practicum.mybankfront.dto.AccountView;
import ru.yandex.practicum.mybankfront.dto.CashAction;
import ru.yandex.practicum.mybankfront.exception.GatewayApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Контроллер main.html. После редизайна на этапе 4 вся работа делегируется
 * {@link BankApiClient} — контроллер только транслирует HTTP формы в вызовы
 * Gateway API и наполняет Thymeleaf-модель.
 * <p>
 * Поля модели (name, birthdate, sum, accounts, errors, info) соответствуют
 * контракту {@code main.html} из исходного скелета — template можно не менять.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final BankApiClient api;

    @GetMapping("/")
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model) {
        fillModel(model, null, null);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        try {
            api.updateMyAccount(name, birthdate);
            fillModel(model, null, "Профиль успешно обновлён");
        } catch (GatewayApiException e) {
            fillModel(model, List.of(e.getMessage()), null);
        }
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") CashAction action
    ) {
        try {
            api.cashOperation(value, action);
            String msg = action == CashAction.PUT
                    ? "Положено %s руб".formatted(value)
                    : "Снято %s руб".formatted(value);
            fillModel(model, null, msg);
        } catch (GatewayApiException e) {
            fillModel(model, List.of(e.getMessage()), null);
        }
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") BigDecimal value,
            @RequestParam("login") String toLogin
    ) {
        try {
            api.transfer(toLogin, value);
            fillModel(model, null,
                    "Успешно переведено %s руб клиенту %s".formatted(value, toLogin));
        } catch (GatewayApiException e) {
            fillModel(model, List.of(e.getMessage()), null);
        }
        return "main";
    }

    /**
     * Наполняет модель main.html актуальными данными аккаунта пользователя.
     * Если получить данные не удаётся — показываем форму с пустыми полями
     * и сообщением об ошибке (но не падаем).
     */
    private void fillModel(Model model, @Nullable List<String> errors, @Nullable String info) {
        AccountView me;
        List<AccountOption> others;
        try {
            me = api.getMyAccount();
            others = api.getOtherAccounts();
        } catch (GatewayApiException e) {
            log.error("Не удалось получить данные аккаунта: {}", e.getMessage());
            // Аккуратная деградация: страница отрисуется с ошибкой, но не упадёт.
            model.addAttribute("name", "");
            model.addAttribute("birthdate", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            model.addAttribute("sum", BigDecimal.ZERO);
            model.addAttribute("accounts", Collections.emptyList());
            model.addAttribute("errors", List.of("Сервис недоступен: " + e.getMessage()));
            model.addAttribute("info", null);
            return;
        }

        model.addAttribute("name", me.name());
        model.addAttribute("birthdate",
                me.birthdate() != null ? me.birthdate().format(DateTimeFormatter.ISO_DATE) : "");
        model.addAttribute("sum", me.balance());
        model.addAttribute("accounts", others);
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }
}
