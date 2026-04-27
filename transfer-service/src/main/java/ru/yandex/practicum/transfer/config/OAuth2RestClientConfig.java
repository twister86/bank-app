package ru.yandex.practicum.transfer.config;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class OAuth2RestClientConfig {

    @Bean
    public RestClient accountsRestClient(
            OAuth2AuthorizedClientManager mgr, LoadBalancerClient lb) {
        return build(mgr, lb);
    }

    @Bean
    public RestClient notificationsRestClient(
            OAuth2AuthorizedClientManager mgr, LoadBalancerClient lb) {
        return build(mgr, lb);
    }

    private RestClient build(OAuth2AuthorizedClientManager mgr, LoadBalancerClient lb) {
        OAuth2ClientHttpRequestInterceptor oauth2 =
                new OAuth2ClientHttpRequestInterceptor(mgr);
        oauth2.setClientRegistrationIdResolver(req -> "transfer-client");
        oauth2.setPrincipalResolver(request ->
                new AnonymousAuthenticationToken(
                        "transfer-client",
                        "transfer-client",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        return RestClient.builder()
                .requestInterceptor(new LoadBalancerInterceptor(lb))
                .requestInterceptor(oauth2)
                .build();
    }
}
