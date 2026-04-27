package ru.yandex.practicum.accounts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.practicum.accounts.dto.BalanceChangeRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест: поднимает реальный контекст Spring + реальный PostgreSQL в Testcontainer.
 * <p>
 * Keycloak не поднимаем — для security используем {@code jwt()} post-processor из
 * spring-security-test, который подсовывает mock-JWT в SecurityContext.
 * Конфигурация resource server при этом остаётся реальной.
 */
// Properties через @TestPropertySource применяются ДО bootstrap (в отличие от
// application-test.yml, который читается слишком поздно). Это единственный способ
// отключить config client, если в classpath есть spring-cloud-starter-bootstrap.
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import=optional:configserver:"
})
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AccountControllerIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bank")
                    .withUsername("bank")
                    .withPassword("bank");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Liquibase создаст схему accounts сам (liquibase-schema), но
        // для Hibernate default_schema уже должна существовать → создаём public alias.
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "accounts");
        registry.add("spring.liquibase.default-schema", () -> "accounts");
        registry.add("spring.liquibase.liquibase-schema", () -> "accounts");
        // Выключаем config client, eureka, discovery для теста.
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        // Fake issuer URI — resource server не валидирует с сервером, т.к.
        // мы используем mock-JWT, но property должно быть задано.
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost/realms/test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMe_returnsCurrentUserAccount() throws Exception {
        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "ivan"))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_accounts.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is("ivan")))
                .andExpect(jsonPath("$.name", is("Иванов Иван")));
    }

    @Test
    void listOthers_excludesCurrentUser() throws Exception {
        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "ivan"))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_accounts.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[?(@.login=='ivan')]").doesNotExist());
    }

    @Test
    void updateMe_validInputUpdatesAccount() throws Exception {
        UpdateAccountRequest req = new UpdateAccountRequest(
                "Иванов Иван Обновлённый", LocalDate.of(1990, 5, 15));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "ivan"))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Иванов Иван Обновлённый")));
    }

    @Test
    void updateMe_rejectsUnder18() throws Exception {
        UpdateAccountRequest req = new UpdateAccountRequest(
                "Малолетка", LocalDate.now().minusYears(16));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(j -> j.claim("preferred_username", "ivan"))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_rejectsInsufficientFunds() throws Exception {
        BalanceChangeRequest req = new BalanceChangeRequest(new BigDecimal("999999.00"));

        mockMvc.perform(post("/accounts/ivan/withdraw")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void anonymousAccess_isRejected() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }
}
