package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.client.NotificationsClient;
import ru.yandex.practicum.transfer.dto.AccountSnapshot;
import ru.yandex.practicum.transfer.dto.TransferRequest;
import ru.yandex.practicum.transfer.exception.AccountsOperationFailedException;
import ru.yandex.practicum.transfer.exception.TransferValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private TransferService service;

    private final AccountSnapshot stub = new AccountSnapshot(
            "x", "X", LocalDate.of(1990, 1, 1), BigDecimal.ZERO);

    @Test
    void transfer_happyPath_callsWithdrawAndDeposit() {
        when(accountsClient.withdraw(eq("ivan"), any())).thenReturn(stub);
        when(accountsClient.deposit(eq("petr"), any())).thenReturn(stub);

        service.transfer("ivan", new TransferRequest("petr", new BigDecimal("100.00")));

        verify(accountsClient).withdraw(eq("ivan"), eq(new BigDecimal("100.00")));
        verify(accountsClient).deposit(eq("petr"), eq(new BigDecimal("100.00")));
        verify(notificationsClient).sendNotification(eq("ivan"), anyString());
        verify(notificationsClient).sendNotification(eq("petr"), anyString());
    }

    @Test
    void transfer_toSelf_isRejected() {
        assertThatThrownBy(() ->
                service.transfer("ivan", new TransferRequest("ivan", new BigDecimal("10"))))
                .isInstanceOf(TransferValidationException.class);

        verify(accountsClient, never()).withdraw(anyString(), any());
    }

    @Test
    void transfer_depositFails_compensatesWithdraw() {
        when(accountsClient.withdraw(eq("ivan"), any())).thenReturn(stub);
        // Первый deposit (получателю) падает, второй (компенсация отправителю) ок
        when(accountsClient.deposit(eq("petr"), any()))
                .thenThrow(new AccountsOperationFailedException(500, "upstream"));
        when(accountsClient.deposit(eq("ivan"), any())).thenReturn(stub);

        assertThatThrownBy(() ->
                service.transfer("ivan", new TransferRequest("petr", new BigDecimal("100"))))
                .isInstanceOf(AccountsOperationFailedException.class);

        verify(accountsClient).deposit(eq("ivan"), eq(new BigDecimal("100"))); // компенсация
    }
}
