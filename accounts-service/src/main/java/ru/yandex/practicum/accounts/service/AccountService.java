package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.AccountSummary;
import ru.yandex.practicum.accounts.dto.BalanceChangeRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.exception.AccountNotFoundException;
import ru.yandex.practicum.accounts.exception.BusinessValidationException;
import ru.yandex.practicum.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int MIN_AGE_YEARS = 18;

    private final AccountRepository accountRepository;
    private final NotificationsClient notificationsClient;

    @Transactional(readOnly = true)
    public AccountResponse getByLogin(String login) {
        Account account = findByLoginOrThrow(login);
        return toResponse(account);
    }

    /**
     * Возвращает все аккаунты, кроме запрашивающего — это список получателей
     * для блока переводов на фронте.
     */
    @Transactional(readOnly = true)
    public List<AccountSummary> listOthers(String currentLogin) {
        return accountRepository.findAll().stream()
                .filter(a -> !a.getLogin().equals(currentLogin))
                .map(a -> new AccountSummary(a.getLogin(), a.getName()))
                .toList();
    }

    @Transactional
    public AccountResponse update(String login, UpdateAccountRequest request) {
        validateAge(request.birthdate());
        Account account = findByLoginOrThrow(login);
        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        // @Transactional + dirty checking → UPDATE при коммите
        return toResponse(account);
    }

    /**
     * Пополнение счёта. Вызывается cash-service'ом по JWT Client Credentials.
     */
    @Transactional
    public AccountResponse deposit(String login, BalanceChangeRequest request) {
        Account account = findByLoginOrThrow(login);
        account.setBalance(account.getBalance().add(request.amount()));
        notificationsClient.sendNotification(login,
                "На ваш счёт зачислено %s руб".formatted(request.amount()));
        return toResponse(account);
    }

    /**
     * Снятие со счёта. Вызывается cash-service и transfer-service.
     * Проверяет достаточность средств.
     */
    @Transactional
    public AccountResponse withdraw(String login, BalanceChangeRequest request) {
        Account account = findByLoginOrThrow(login);
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException();
        }
        account.setBalance(account.getBalance().subtract(request.amount()));
        notificationsClient.sendNotification(login,
                "Со счёта списано %s руб".formatted(request.amount()));
        return toResponse(account);
    }

    /* ---------- helpers ---------- */

    private Account findByLoginOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    private void validateAge(LocalDate birthdate) {
        int age = Period.between(birthdate, LocalDate.now()).getYears();
        if (age < MIN_AGE_YEARS) {
            throw new BusinessValidationException(
                    "Возраст должен быть не менее " + MIN_AGE_YEARS + " лет");
        }
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getLogin(), a.getName(), a.getBirthdate(),
                a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO);
    }
}
