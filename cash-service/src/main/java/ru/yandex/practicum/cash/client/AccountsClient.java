package ru.yandex.practicum.cash.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.cash.dto.AccountSnapshot;
import ru.yandex.practicum.cash.dto.CashOperationRequest;
import ru.yandex.practicum.cash.exception.AccountsOperationFailedException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Клиент accounts-service. Вызывается из cash-service напрямую
 * (не через Gateway), авторизуется Client Credentials токеном
 * под client-id {@code cash-client}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestClient accountsRestClient;
    private final ObjectMapper objectMapper;

    @Value("${services.accounts.url}")
    private String accountsUrl;

    /** Пополнение счёта (CashAction.PUT). */
    public AccountSnapshot deposit(String login, BigDecimal amount) {
        return exchange(login, "deposit", amount);
    }

    /** Снятие со счёта (CashAction.GET). */
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
