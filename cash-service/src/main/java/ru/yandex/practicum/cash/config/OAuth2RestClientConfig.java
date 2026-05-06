package ru.yandex.practicum.cash.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cash-service использует один Client Credentials регистрационный id —
 * {@code cash-client} — со scope'ами {@code accounts.write, notifications.write}.
 * Поэтому оба RestClient'а получают один и тот же интерцептор.
 *
 * <p>Балансировка между инстансами downstream-сервисов выполняется
 * на уровне Kubernetes: каждый Service ({@code accounts}, {@code notifications})
 * — это виртуальный IP, за которым kube-proxy делает round-robin
 * по подам. Дополнительный client-side load balancer не нужен.
 */
@Configuration
public class OAuth2RestClientConfig {

    @Bean
    public RestClient accountsRestClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        return build(authorizedClientManager);
    }

    @Bean
    public RestClient notificationsRestClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        return build(authorizedClientManager);
    }

    private RestClient build(OAuth2AuthorizedClientManager mgr) {
        OAuth2ClientHttpRequestInterceptor oauth2 =
                new OAuth2ClientHttpRequestInterceptor(mgr);
        oauth2.setClientRegistrationIdResolver(req -> "cash-client");
        oauth2.setPrincipalResolver(request ->
                new AnonymousAuthenticationToken(
                        "cash-client",
                        "cash-client",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        return RestClient.builder()
                .requestInterceptor(oauth2)
                .build();
    }
}