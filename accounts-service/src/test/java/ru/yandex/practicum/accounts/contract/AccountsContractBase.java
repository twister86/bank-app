package ru.yandex.practicum.accounts.contract;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.controller.AccountController;
import ru.yandex.practicum.accounts.controller.GlobalExceptionHandler;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.BalanceChangeRequest;
import ru.yandex.practicum.accounts.exception.InsufficientFundsException;
import ru.yandex.practicum.accounts.repository.AccountRepository;
import ru.yandex.practicum.accounts.service.AccountService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Базовый класс для тестов, сгенерированных Spring Cloud Contract по
 * groovy-контрактам из {@code src/test/resources/contracts/accounts/}.
 * <p>
 * Тесты запускаются без реального контекста Spring — только MockMvc +
 * замоканные зависимости. Это делает контрактные тесты быстрыми и
 * не требующими внешних зависимостей (БД, Keycloak, Eureka).
 * <p>
 * Авторизация намеренно НЕ проверяется — контракт описывает только
 * форму запроса/ответа. Для тестов безопасности используется
 * отдельный IT-тест (AccountControllerIT).
 */
public abstract class AccountsContractBase {

    @BeforeEach
    void setUp() {
        NotificationsClient notificationsClient = Mockito.mock(NotificationsClient.class);
        AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
        AccountService accountService = new AccountService(accountRepository, notificationsClient);

        // Мокируем ответы, соответствующие контрактам.
        AccountResponse ivanAfterDeposit = new AccountResponse(
                "ivan", "Иванов Иван", LocalDate.of(1990, 5, 15), new BigDecimal("1100.00"));

        AccountService spiedService = Mockito.spy(accountService);
        Mockito.doReturn(ivanAfterDeposit)
                .when(spiedService).deposit(eq("ivan"), any(BalanceChangeRequest.class));
        Mockito.doThrow(new InsufficientFundsException())
                .when(spiedService).withdraw(eq("ivan"), any(BalanceChangeRequest.class));

        AccountController controller = new AccountController(spiedService);
        RestAssuredMockMvc.standaloneSetup(MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter()));
    }
}
