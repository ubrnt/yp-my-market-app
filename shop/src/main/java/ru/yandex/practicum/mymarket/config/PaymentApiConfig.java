package ru.yandex.practicum.mymarket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.DefaultApi;

import static java.lang.System.currentTimeMillis;

@Configuration
public class PaymentApiConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentApiConfig.class);

    @Bean
    public DefaultApi paymentApi(@Value("${app.payment.base-url}") String baseUrl) {
        WebClient webClient = ApiClient.buildWebClientBuilder()
                .filter(logExchange())
                .build();

        return new DefaultApi(new ApiClient(webClient).setBasePath(baseUrl));
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
