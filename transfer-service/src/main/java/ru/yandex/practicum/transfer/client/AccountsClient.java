package ru.yandex.practicum.transfer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.transfer.dto.AccountSnapshot;
import ru.yandex.practicum.transfer.exception.AccountsOperationFailedException;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestClient accountsRestClient;
    private final ObjectMapper objectMapper;

    @Value("${services.accounts.url}")
    private String accountsUrl;

    public AccountSnapshot withdraw(String login, BigDecimal amount) {
        return exchange(login, "withdraw", amount);
    }

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
