package ru.yandex.practicum.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Security-конфиг Gateway'я.
 * <p>
 * Gateway выступает как OAuth2 Client (Authorization Code Flow):
 * <ol>
 *   <li>Пользователь идёт на защищённый URL (/api/**) → перенаправление на Keycloak;</li>
 *   <li>После логина Keycloak редиректит обратно на {@code /login/oauth2/code/keycloak}
 *       с authorization code;</li>
 *   <li>Spring Security обменивает code на access/refresh token и кладёт
 *       их в HTTP-сессию пользователя;</li>
 *   <li>TokenRelay фильтр (см. gateway.yml) достаёт access token из сессии
 *       и вставляет в заголовок Authorization при проксировании.</li>
 * </ol>
 * <p>
 * CSRF отключаем, т.к. downstream-сервисы — stateless API, а HTML-формы
 * (на которые CSRF рассчитан) обслуживает уже отдельный фронт-модуль.
 */
@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
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

    /**
     * После локального logout'а отправляем пользователя на Keycloak, чтобы
     * закрыть и SSO-сессию — иначе следующий логин пройдёт без ввода
     * пароля. Возврат — на корень Gateway'я.
     */
    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repo) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }
}
