package ru.yandex.practicum.mymarket.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.MockServerConfigurer;
import reactor.core.publisher.Mono;

@TestConfiguration
public class SecurityTestConfig {

    @Bean
    MockServerConfigurer springSecurityMockServerConfigurer() {
        return SecurityMockServerConfigurers.springSecurity();
    }

    @Bean
    ReactiveUserDetailsService reactiveUserDetailsService() {
        return username -> Mono.empty();
    }
}
