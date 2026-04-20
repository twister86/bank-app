package ru.yandex.practicum.mybankfront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Front — это OAuth2 Client с Authorization Code Flow.
 * <p>
 * Все страницы (кроме /actuator/health) требуют аутентификацию.
 * Неаутентифицированный запрос редиректит на Keycloak login.
 * После успешного входа Spring сохранит access/refresh tokens в сессии,
 * а {@link OAuth2RestClientConfig} использует их при обращении в Gateway.
 * <p>
 * CSRF отключаем — главная форма редактирования профиля отправляет обычный
 * form-post, и для учебного проекта простота > CSRF-защита. В проде — включить
 * обратно и добавить {@code _csrf} токен в шаблон.
 */
@Configuration
public class FrontSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                );
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repo) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }
}
