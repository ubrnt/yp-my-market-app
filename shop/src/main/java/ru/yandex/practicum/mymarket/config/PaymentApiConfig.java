package ru.yandex.practicum.mymarket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.DefaultApi;

import static java.lang.System.currentTimeMillis;

@Configuration
public class PaymentApiConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentApiConfig.class);

    @Bean
    public DefaultApi paymentApi(@Value("${app.payment.base-url}") String baseUrl,
                                 ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter) {
        WebClient webClient = ApiClient.buildWebClientBuilder()
                .filter(oauth2Filter)
                .filter(logExchange())
                .build();

        return new DefaultApi(new ApiClient(webClient).setBasePath(baseUrl));
    }

    @Bean
    public ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter(
            ReactiveClientRegistrationRepository clientRegistrations,
            ReactiveOAuth2AuthorizedClientService authorizedClients) {
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
        manager.setAuthorizedClientProvider(new ClientCredentialsReactiveOAuth2AuthorizedClientProvider());

        ServerOAuth2AuthorizedClientExchangeFilterFunction filter =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        filter.setDefaultClientRegistrationId("payment");

        return filter;
    }

    private ExchangeFilterFunction logExchange() {
        return (request, next) -> {
            long start = currentTimeMillis();

            log.info("call payment-service {} {}", request.method(), request.url());
            return next.exchange(request)
                    .doOnNext(response -> log.info("response from payment-service {} {} {} ({} ms)",
                            request.method(), request.url(), response.statusCode(), currentTimeMillis() - start));
        };
    }
}
