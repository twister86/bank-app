package ru.yandex.practicum.mybankfront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient} для вызовов в Gateway. Ключевое отличие от cash/transfer/accounts:
 * здесь используется токен <b>залогиненного пользователя</b> (Authorization Code Flow),
 * а не Client Credentials.
 * <p>
 * {@link OAuth2ClientHttpRequestInterceptor} по умолчанию — в HTTP-контексте запроса —
 * обращается к {@code OAuth2AuthorizedClientRepository} (session-backed) и достаёт
 * access token текущего пользователя. Client-registration-id резолвится как
 * {@code "keycloak"} (см. {@code front.yml} в Config Server).
 * <p>
 * LoadBalancer здесь не подключаем: Gateway — единая точка входа, адрес фиксированный
 * ({@code services.gateway.url}). Регистрироваться в Eureka Gateway, конечно, можно
 * (и нужно — другие сервисы ведь тоже могут ходить в него), но фронт всегда идёт на
 * один и тот же URL.
 */
@Configuration
public class OAuth2RestClientConfig {

    @Bean
    public RestClient gatewayRestClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        OAuth2ClientHttpRequestInterceptor oauth2 =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2.setClientRegistrationIdResolver(req -> "keycloak");
        return RestClient.builder()
                .requestInterceptor(oauth2)
                .build();
    }
}


