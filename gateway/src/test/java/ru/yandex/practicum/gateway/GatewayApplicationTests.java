package ru.yandex.practicum.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke-тест Gateway'я.
 * <p>
 * Проверяет, что:
 * <ul>
 *   <li>контекст поднимается (т.е. все конфиги валидны и совместимы);</li>
 *   <li>анонимный запрос в защищённый роут перенаправляет на Keycloak OAuth2 login;</li>
 *   <li>actuator доступен без аутентификации.</li>
 * </ul>
 * Реальный проброс JWT в downstream проверяется на интеграционном уровне
 * (Этап 3, contract-тесты между cash и accounts).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GatewayApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // noop — если контекст не поднимется, тест упадёт автоматически
    }

    @Test
    void anonymousRequest_redirectsToKeycloakLogin() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().is3xxRedirection())
                // Spring Security отправит на /oauth2/authorization/keycloak
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
