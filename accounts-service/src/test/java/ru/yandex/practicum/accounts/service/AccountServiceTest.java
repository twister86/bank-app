package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.BalanceChangeRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.Account;
import ru.yandex.practicum.accounts.exception.AccountNotFoundException;
import ru.yandex.practicum.accounts.exception.BusinessValidationException;
import ru.yandex.practicum.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private AccountService service;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .login("ivan")
                .name("Иванов Иван")
                .birthdate(LocalDate.of(1990, 5, 15))
                .balance(new BigDecimal("1000.00"))
                .version(0L)
                .build();
    }

    @Test
    void getByLogin_returnsAccount() {
        when(accountRepository.findByLogin("ivan")).thenReturn(Optional.of(account));

        AccountResponse result = service.getByLogin("ivan");

        assertThat(result.login()).isEqualTo("ivan");
        assertThat(result.balance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void getByLogin_throwsWhenMissing() {
        when(accountRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByLogin("ghost"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void update_rejectsUnder18() {

        UpdateAccountRequest req = new UpdateAccountRequest(
                "Иванов Иван Младший",
                LocalDate.now().minusYears(17)
        );

        assertThatThrownBy(() -> service.update("ivan", req))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("18");
    }

    @Test
    void withdraw_throwsWhenInsufficient() {
        when(accountRepository.findByLogin("ivan")).thenReturn(Optional.of(account));

        BalanceChangeRequest req = new BalanceChangeRequest(new BigDecimal("9999.00"));

        assertThatThrownBy(() -> service.withdraw("ivan", req))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void deposit_increasesBalanceAndSendsNotification() {
        when(accountRepository.findByLogin("ivan")).thenReturn(Optional.of(account));

        BalanceChangeRequest req = new BalanceChangeRequest(new BigDecimal("250.00"));
        AccountResponse result = service.deposit("ivan", req);

        assertThat(result.balance()).isEqualByComparingTo("1250.00");
        verify(notificationsClient).sendNotification(anyString(), anyString());
    }

    @Test
    void withdraw_decreasesBalanceAndSendsNotification() {
        when(accountRepository.findByLogin("ivan")).thenReturn(Optional.of(account));

        BalanceChangeRequest req = new BalanceChangeRequest(new BigDecimal("400.00"));
        AccountResponse result = service.withdraw("ivan", req);

        assertThat(result.balance()).isEqualByComparingTo("600.00");
        verify(notificationsClient).sendNotification(anyString(), anyString());
    }
}
