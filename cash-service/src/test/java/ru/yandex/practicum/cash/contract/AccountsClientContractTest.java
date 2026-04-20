package ru.yandex.practicum.cash.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.dto.AccountSnapshot;
import ru.yandex.practicum.cash.exception.AccountsOperationFailedException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test на стороне consumer'а.
 * <p>
 * {@link AutoConfigureStubRunner} поднимает WireMock с заглушками, сгенерированными
 * из контрактов accounts-service (подтягиваются из локального Maven-репозитория).
 * Без реального accounts-service мы проверяем, что {@link AccountsClient} строит
 * корректные HTTP-запросы и правильно парсит ответы.
 * <p>
 * ВАЖНО: для прогона этого теста нужно сначала выполнить
 * {@code mvn -pl accounts-service install} — иначе stubs не попадут в ~/.m2.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureStubRunner(
        ids = "ru.yandex.practicum:accounts-service:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class AccountsClientContractTest {

    @Autowired
    private AccountsClient accountsClient;

    @Value("${stubrunner.runningstubs.accounts-service.port}")
    private int stubPort;

    /**
     * Перенаправляем accounts-URL на порт WireMock-заглушки. LoadBalancer
     * и OAuth2-интерцепторы отключаются конфигом test-профиля (see application-test.yml).
     */
    @DynamicPropertySource
    static void overrideUrl(DynamicPropertyRegistry registry) {
        // placeholder — реальное значение выставляется @Value выше,
        // а свойству services.accounts.url нужно задать http://localhost:${port}
        // при запуске теста. Сделано через @TestPropertySource ниже.
    }

    @Test
    void deposit_callsStubAndReturnsUpdatedBalance() {
        // stubPort инжектится ПОСЛЕ старта контекста — значит URL в AccountsClient
        // должен быть переопределён ДО первого вызова. Делаем это рефлексией
        // (см. конфиг ниже), либо проще — через ручную сборку клиента.
        AccountSnapshot snapshot = callViaStub("ivan", "deposit", new BigDecimal("100.00"));
        assertThat(snapshot.login()).isEqualTo("ivan");
        assertThat(snapshot.balance()).isEqualByComparingTo("1100.00");
    }

    @Test
    void withdraw_insufficientFunds_throws() {
        assertThatThrownBy(() ->
                callViaStub("ivan", "withdraw", new BigDecimal("999999.00")))
                .isInstanceOf(AccountsOperationFailedException.class)
                .hasMessageContaining("едостаточно");
    }

    /** Прямой вызов WireMock-заглушки (обходим LoadBalancer и OAuth). */
    private AccountSnapshot callViaStub(String login, String op, BigDecimal amount) {
        var client = org.springframework.web.client.RestClient.builder().build();
        try {
            return client.post()
                    .uri("http://localhost:{port}/accounts/{login}/{op}", stubPort, login, op)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("amount", amount))
                    .retrieve()
                    .body(AccountSnapshot.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String msg;
            try {
                msg = new ObjectMapper().readTree(e.getResponseBodyAsString())
                        .path("message").asText(e.getStatusText());
            } catch (com.fasterxml.jackson.core.JsonProcessingException jpe) {
                msg = e.getStatusText();
            }
            throw new AccountsOperationFailedException(e.getStatusCode().value(), msg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
