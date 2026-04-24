package ru.yandex.practicum.cash.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cash.dto.AccountSnapshot;
import ru.yandex.practicum.cash.exception.AccountsOperationFailedException;
import ru.yandex.practicum.cash.exception.AccountsUnavailableException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Клиент accounts-service. Вызывается из cash-service напрямую
 * (не через Gateway), авторизуется Client Credentials токеном
 * под client-id {@code cash-client}.
 * <p>
 * Устойчивость к сбоям обеспечивается Resilience4j:
 * <ul>
 *   <li>{@code @Retry} — 3 попытки с экспоненциальной задержкой при сетевых
 *       ошибках (500/503/timeout). На бизнес-ошибки (409 Insufficient Funds)
 *       retry НЕ делается — см. {@code ignore-exceptions} в application.yml.</li>
 *   <li>{@code @CircuitBreaker} — если accounts-service падает чаще, чем в
 *       50% последних 10 вызовов, прекращаем стучаться на 30 секунд.</li>
 *   <li>{@code fallbackMethod} — при всех неудачах ВМЕСТО 500 во фронт уходит
 *       {@link AccountsUnavailableException} с понятным текстом. Главное —
 *       деньги НЕ меняются, уведомление НЕ отправляется.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountsClient {

    private static final String CIRCUIT = "accountsService";

    private final RestClient accountsRestClient;
    private final ObjectMapper objectMapper;

    @Value("${services.accounts.url}")
    private String accountsUrl;

    /** Пополнение счёта. */
    @Retry(name = CIRCUIT)
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "onAccountsUnavailable")
    public AccountSnapshot deposit(String login, BigDecimal amount) {
        return exchange(login, "deposit", amount);
    }

    /** Снятие со счёта. */
    @Retry(name = CIRCUIT)
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "onAccountsUnavailable")
    public AccountSnapshot withdraw(String login, BigDecimal amount) {
        return exchange(login, "withdraw", amount);
    }

    private AccountSnapshot exchange(String login, String op, BigDecimal amount) {
        return accountsRestClient.post()
                .uri(accountsUrl + "/accounts/{login}/{op}", login, op)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    String body = new String(resp.getBody().readAllBytes());
                    String message = extractMessage(body);
                    log.warn("accounts-service returned {}: {}", resp.getStatusCode(), message);
                    throw new AccountsOperationFailedException(
                            resp.getStatusCode().value(), message);
                })
                .body(AccountSnapshot.class);
    }

    /**
     * Fallback вызывается, когда все retry-попытки исчерпаны или circuit breaker
     * открыт. ВАЖНО: бизнес-ошибки ({@link AccountsOperationFailedException}) сюда
     * НЕ попадают (см. ignore-exceptions в application.yml) — они проходят
     * насквозь, чтобы пользователь получил внятное сообщение вроде "недостаточно
     * средств", а не технический fallback-текст.
     */
    @SuppressWarnings("unused") // вызывается рефлексией Resilience4j
    private AccountSnapshot onAccountsUnavailable(String login, BigDecimal amount,
                                                   Throwable ex) {
        log.error("Accounts-service недоступен (login={}, amount={}): {}",
                login, amount, ex.toString());
        throw new AccountsUnavailableException(
                "Сервис счетов временно недоступен, попробуйте через минуту");
    }

    /**
     * Accounts возвращает ошибки в формате {@code {"message": "...", ...}}
     * (см. GlobalExceptionHandler в accounts-service). Достаём message;
     * если парсинг не удался — возвращаем raw body.
     */
    private String extractMessage(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode msg = node.path("message");
            return msg.isMissingNode() ? body : msg.asText();
        } catch (Exception e) {
            return body;
        }
    }
}
