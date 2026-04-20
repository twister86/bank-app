package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.NotificationsClient;
import ru.yandex.practicum.cash.dto.AccountSnapshot;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.CashOperationRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    public AccountSnapshot perform(String login, CashOperationRequest request) {
        AccountSnapshot result = switch (request.action()) {
            case PUT -> accountsClient.deposit(login, request.amount());
            case GET -> accountsClient.withdraw(login, request.amount());
        };
        // Accounts и сам шлёт уведомление, но по ТЗ Cash тоже обязан
        // оповестить — возможно с другой формулировкой. Дублирующие
        // уведомления на этом этапе — осознанный компромисс.
        String verb = request.action() == CashAction.PUT ? "Пополнение" : "Снятие";
        notificationsClient.sendNotification(login,
                "%s на сумму %s руб выполнено".formatted(verb, request.amount()));
        return result;
    }
}
