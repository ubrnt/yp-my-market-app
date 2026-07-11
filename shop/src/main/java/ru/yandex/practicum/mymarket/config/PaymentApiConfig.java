package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.DefaultApi;

@Configuration
public class PaymentApiConfig {

    @Bean
    public DefaultApi paymentApi(@Value("${app.payment.base-url}") String baseUrl) {
        return new DefaultApi(new ApiClient().setBasePath(baseUrl));
    }
}
