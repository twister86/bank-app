package ru.yandex.practicum.cash.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cash.exception.AccountsUnavailableException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест Resilience4j-обёрток вокруг {@link AccountsClient}.
 * <p>
 * Поднимает минимальный Spring-контекст (без web, без config-server, без
 * discovery) — достаточный для того, чтобы Resilience4j AOP-аспекты
 * реально проксировали {@code @Retry} и {@code @CircuitBreaker}.
 * <p>
 * Без Spring-прокси (в чистом Mockito-тесте) аннотации Resilience4j НЕ
 * срабатывают — вызов идёт напрямую в метод без обёртки. Поэтому тест
 * обязан быть уровня integration, не unit.
 */
@SpringBootTest(classes = AccountsClientResilienceTest.TestApp.class)
@TestPropertySource(properties = {
        "services.accounts.url=http://fake-accounts",
        // Быстрые ретраи для теста (без exponential backoff).
        "resilience4j.retry.instances.accountsService.max-attempts=3",
        "resilience4j.retry.instances.accountsService.wait-duration=10ms",
        "resilience4j.circuitbreaker.instances.accountsService.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.accountsService.minimum-number-of-calls=10",
        // Отключаем всё лишнее.
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import=optional:configserver:",
        "spring.main.web-application-type=none"
})
class AccountsClientResilienceTest {

    /**
     * Минимальное тестовое Spring-приложение: Resilience4j autoconfig
     * (подключится автоматически через spring.factories) плюс наш
     * {@link AccountsClient}.
     */
    @Configuration
    @EnableAutoConfiguration
    @Import(AccountsClient.class)
    static class TestApp {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @MockitoBean
    private RestClient accountsRestClient;

    @Autowired
    private AccountsClient accountsClient;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void reset() {
        circuitBreakerRegistry.circuitBreaker("accountsService").reset();
    }

    @Test
    void networkError_retries3Times_thenFallbackThrowsUnavailable() {
        // RestClient.post() сразу падает сетевой ошибкой — эмуляция полного
        // недоступного accounts-service.
        when(accountsRestClient.post()).thenThrow(
                new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> accountsClient.deposit("ivan", new BigDecimal("100")))
                .isInstanceOf(AccountsUnavailableException.class)
                .hasMessageContaining("временно недоступен");

        // max-attempts=3 в @Retry → RestClient.post() должен быть вызван
        // ровно 3 раза (1 исходный + 2 повтора).
        verify(accountsRestClient, times(3)).post();

        // Посмотрим на метрики retry: количество неуспешных попыток совпадает.
        var retryMetrics = retryRegistry.retry("accountsService").getMetrics();
        // numberOfFailedCallsWithRetryAttempt — сколько раз retry не помог.
        // Ожидаем хотя бы один такой (наш тест).
        org.assertj.core.api.Assertions
                .assertThat(retryMetrics.getNumberOfFailedCallsWithRetryAttempt())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void withdrawNetworkError_alsoFallsBack() {
        // Второй метод AccountsClient (withdraw) тоже должен отрабатывать
        // fallback — убеждаемся, что аннотации не только на deposit.
        when(accountsRestClient.post()).thenThrow(
                new ResourceAccessException("Timeout"));

        assertThatThrownBy(() -> accountsClient.withdraw("ivan", new BigDecimal("50")))
                .isInstanceOf(AccountsUnavailableException.class);

        verify(accountsRestClient, atLeast(2)).post();
    }
}
