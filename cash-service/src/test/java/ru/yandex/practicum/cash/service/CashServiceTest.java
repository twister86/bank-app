package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.NotificationsClient;
import ru.yandex.practicum.cash.dto.AccountSnapshot;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.CashOperationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private CashService service;

    @Test
    void put_callsDepositAndNotifies() {
        AccountSnapshot stub = new AccountSnapshot(
                "ivan", "Иванов Иван", LocalDate.of(1990, 1, 1), new BigDecimal("1100.00"));
        when(accountsClient.deposit(eq("ivan"), any())).thenReturn(stub);

        AccountSnapshot result = service.perform(
                "ivan",
                new CashOperationRequest(new BigDecimal("100"), CashAction.PUT));

        assertThat(result.balance()).isEqualByComparingTo("1100.00");
        verify(accountsClient).deposit(eq("ivan"), eq(new BigDecimal("100")));
        verify(notificationsClient).sendNotification(eq("ivan"), anyString());
    }

    @Test
    void get_callsWithdrawAndNotifies() {
        AccountSnapshot stub = new AccountSnapshot(
                "ivan", "Иванов Иван", LocalDate.of(1990, 1, 1), new BigDecimal("900.00"));
        when(accountsClient.withdraw(eq("ivan"), any())).thenReturn(stub);

        AccountSnapshot result = service.perform(
                "ivan",
                new CashOperationRequest(new BigDecimal("100"), CashAction.GET));

        assertThat(result.balance()).isEqualByComparingTo("900.00");
        verify(accountsClient).withdraw(eq("ivan"), eq(new BigDecimal("100")));
        verify(notificationsClient).sendNotification(eq("ivan"), anyString());
    }
}
