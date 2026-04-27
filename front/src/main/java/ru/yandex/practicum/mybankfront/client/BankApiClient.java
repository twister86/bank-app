package ru.yandex.practicum.mybankfront.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.dto.AccountOption;
import ru.yandex.practicum.mybankfront.dto.AccountView;
import ru.yandex.practicum.mybankfront.dto.CashAction;
import ru.yandex.practicum.mybankfront.exception.GatewayApiException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Клиент Gateway API. Все HTTP-запросы идут через единую точку входа
 * (Gateway) с пробросом JWT пользователя (TokenRelay на стороне Gateway).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BankApiClient {

    private final RestClient gatewayRestClient;
    private final ObjectMapper objectMapper;

    @Value("${services.gateway.url}")
    private String gatewayUrl;

    public AccountView getMyAccount() {
        return gatewayRestClient.get()
                .uri(gatewayUrl + "/api/accounts/me")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayException)
                .body(AccountView.class);
    }

    public List<AccountOption> getOtherAccounts() {
        return gatewayRestClient.get()
                .uri(gatewayUrl + "/api/accounts")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayException)
                .body(new ParameterizedTypeReference<>() {});
    }

    public AccountView updateMyAccount(String name, LocalDate birthdate) {
        return gatewayRestClient.put()
                .uri(gatewayUrl + "/api/accounts/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "birthdate", birthdate.toString()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayException)
                .body(AccountView.class);
    }

    public AccountView cashOperation(BigDecimal amount, CashAction action) {
        return gatewayRestClient.post()
                .uri(gatewayUrl + "/api/cash")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount, "action", action.name()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayException)
                .body(AccountView.class);
    }

    public AccountView transfer(String toLogin, BigDecimal amount) {
        return gatewayRestClient.post()
                .uri(gatewayUrl + "/api/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("toLogin", toLogin, "amount", amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayException)
                .body(AccountView.class);
    }

    private void toGatewayException(
            org.springframework.http.HttpRequest req,
            org.springframework.http.client.ClientHttpResponse resp
    ) throws java.io.IOException {
        String body = new String(resp.getBody().readAllBytes());
        String message;
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode m = node.path("message");
            message = m.isMissingNode() ? body : m.asText();
        } catch (Exception e) {
            message = body;
        }
        log.warn("Gateway returned {}: {}", resp.getStatusCode(), message);
        throw new GatewayApiException(resp.getStatusCode().value(), message);
    }
}