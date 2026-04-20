package ru.yandex.practicum.cash.config;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Cash-service использует один Client Credentials регистрационный id —
 * {@code cash-client} — со scope'ами {@code accounts.write, notifications.write}.
 * Поэтому оба RestClient'а получают один и тот же интерцептор.
 */
@Configuration
public class OAuth2RestClientConfig {

    @Bean
    public RestClient accountsRestClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            LoadBalancerClient loadBalancerClient
    ) {
        return build(authorizedClientManager, loadBalancerClient);
    }

    @Bean
    public RestClient notificationsRestClient(
            OAuth2AuthorizedClientManager authorizedClientManager,
            LoadBalancerClient loadBalancerClient
    ) {
        return build(authorizedClientManager, loadBalancerClient);
    }

    private RestClient build(OAuth2AuthorizedClientManager mgr, LoadBalancerClient lb) {
        OAuth2ClientHttpRequestInterceptor oauth2 =
                new OAuth2ClientHttpRequestInterceptor(mgr);
        oauth2.setClientRegistrationIdResolver(req -> "cash-client");
        return RestClient.builder()
                .requestInterceptor(new LoadBalancerInterceptor(lb))
                .requestInterceptor(oauth2)
                .build();
    }
}
