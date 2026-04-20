package ru.yandex.practicum.accounts.config;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Собирает {@link RestClient}, который:
 * 1. Автоматически добавляет Bearer-токен (Client Credentials Flow),
 *    полученный у Keycloak под client-id {@code accounts-client};
 * 2. Резолвит {@code lb://service-name} через Eureka + Spring Cloud LoadBalancer.
 */
@Configuration
public class OAuth2RestClientConfig {

    /**
     * RestClient для вызовов в notifications-service.
     * Префикс клиента — {@code accounts-client} (см. application.yml
     * секцию spring.security.oauth2.client.registration).
     */
    @Bean
    public RestClient notificationsRestClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            LoadBalancerClient loadBalancerClient
    ) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(request -> "accounts-client");

        return RestClient.builder()
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .requestInterceptor(oauth2Interceptor)
                .build();
    }
}
