package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.client.NotificationsClient;
import ru.yandex.practicum.transfer.dto.AccountSnapshot;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.exception.AccountsOperationFailedException;
import ru.yandex.practicum.transfer.exception.TransferValidationException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    /**
     * Перевод fromLogin → toLogin. Операция состоит из двух REST-вызовов
     * в accounts-service. Если второй шаг (deposit) упадёт, делается
     * компенсирующий deposit обратно отправителю (простейший Saga-подобный
     * паттерн без Transactional Outbox; в Sprint 9 этого достаточно).
     */
    public AccountSnapshot transfer(String fromLogin, TransferRequest request) {
        if (fromLogin.equals(request.toLogin())) {
            throw new TransferValidationException("Нельзя перевести на свой же счёт");
        }

        AccountSnapshot fromAfter = accountsClient.withdraw(fromLogin, request.amount());
        log.info("Снято {} у {}", request.amount(), fromLogin);

        try {
            AccountSnapshot toAfter = accountsClient.deposit(request.toLogin(), request.amount());
            log.info("Зачислено {} для {}", request.amount(), toAfter.login());
        } catch (AccountsOperationFailedException e) {
            // Компенсация: возвращаем деньги отправителю.
            log.error("Deposit failed ({}), compensating: вернуть {} → {}",
                    e.getMessage(), request.amount(), fromLogin);
            try {
                accountsClient.deposit(fromLogin, request.amount());
            } catch (Exception rollbackErr) {
                // Крайне неприятный случай: и перевод, и откат не прошли.
                // В настоящем приложении здесь — запись в outbox и retry.
                log.error("Compensating deposit failed for {}: {}", fromLogin,
                        rollbackErr.getMessage());
            }
            throw e;
        }

        notificationsClient.sendNotification(fromLogin,
                "Переведено %s руб клиенту %s".formatted(request.amount(), request.toLogin()));
        notificationsClient.sendNotification(request.toLogin(),
                "Получено %s руб от %s".formatted(request.amount(), fromLogin));

        return fromAfter;
    }
}
