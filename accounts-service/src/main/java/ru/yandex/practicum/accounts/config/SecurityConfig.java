package ru.yandex.practicum.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Accounts — это OAuth2 Resource Server.
 * <p>
 * Запросы идут от двух типов клиентов:
 * <ul>
 *   <li>Пользователь (Authorization Code Flow через фронт/Gateway) —
 *       JWT содержит {@code scope=accounts.read accounts.write} и
 *       {@code preferred_username=<логин>};</li>
 *   <li>Другой сервис (Client Credentials Flow, cash/transfer) —
 *       JWT содержит {@code scope=accounts.write} и
 *       {@code client_id=<cash-client|transfer-client>}.</li>
 * </ul>
 * Доступ разграничиваем по scope-authorities: префикс {@code SCOPE_}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Чтение своего аккаунта / списка получателей — нужен scope чтения.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/accounts/**")
                            .hasAuthority("SCOPE_accounts.read")
                        // Редактирование профиля, изменение баланса — scope записи.
                        .requestMatchers("/accounts/**")
                            .hasAuthority("SCOPE_accounts.write")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }

    /**
     * Keycloak кладёт scope'ы в claim "scope" (строка через пробел) —
     * это стандарт OAuth2. JwtGrantedAuthoritiesConverter умеет это
     * и без настройки, но явно пропишем для наглядности.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthoritiesClaimName("scope");
        scopesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(scopesConverter);
        // Имя principal'а — preferred_username (логин пользователя),
        // либо client_id для межсервисных вызовов.
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        return converter;
    }
}
