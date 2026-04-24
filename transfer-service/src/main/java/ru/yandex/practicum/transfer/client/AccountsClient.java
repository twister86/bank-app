package ru.yandex.practicum.transfer.client;

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
import ru.yandex.practicum.transfer.dto.AccountSnapshot;
import ru.yandex.practicum.transfer.exception.AccountsOperationFailedException;
import ru.yandex.practicum.transfer.exception.AccountsUnavailableException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Клиент accounts-service для transfer-service. Устойчивость к сбоям
 * обеспечивается Resilience4j (@Retry + @CircuitBreaker + fallback).
 * <p>
 * Подробнее про логику — см. {@code ru.yandex.practicum.cash.client.AccountsClient}
 * (один и тот же паттерн).
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

    @Retry(name = CIRCUIT)
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "onAccountsUnavailable")
    public AccountSnapshot withdraw(String login, BigDecimal amount) {
        return exchange(login, "withdraw", amount);
    }

    @Retry(name = CIRCUIT)
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "onAccountsUnavailable")
    public AccountSnapshot deposit(String login, BigDecimal amount) {
        return exchange(login, "deposit", amount);
    }

    private AccountSnapshot exchange(String login, String op, BigDecimal amount) {
        return accountsRestClient.post()
                .uri(accountsUrl + "/accounts/{login}/{op}", login, op)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    String body = new String(resp.getBody().readAllBytes());
                    throw new AccountsOperationFailedException(
                            resp.getStatusCode().value(), extractMessage(body));
                })
                .body(AccountSnapshot.class);
    }

    @SuppressWarnings("unused") // вызывается рефлексией Resilience4j
    private AccountSnapshot onAccountsUnavailable(String login, BigDecimal amount,
                                                   Throwable ex) {
        log.error("Accounts-service недоступен (login={}, amount={}): {}",
                login, amount, ex.toString());
        throw new AccountsUnavailableException(
                "Сервис счетов временно недоступен, попробуйте через минуту");
    }

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
