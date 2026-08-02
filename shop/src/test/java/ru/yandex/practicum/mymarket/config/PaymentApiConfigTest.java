package ru.yandex.practicum.mymarket.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PaymentApiConfigTest {

    @Test
    void oauth2Filter_attachesBearerTokenToOutgoingRequests() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("payment")
                .clientId("shop-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost/token")
                .build();
        InMemoryReactiveClientRegistrationRepository registrations =
                new InMemoryReactiveClientRegistrationRepository(registration);
        InMemoryReactiveOAuth2AuthorizedClientService authorizedClients =
                new InMemoryReactiveOAuth2AuthorizedClientService(registrations);

        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-token", Instant.now(), Instant.now().plusSeconds(300));
        AnonymousAuthenticationToken principal = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        StepVerifier.create(authorizedClients.saveAuthorizedClient(
                        new OAuth2AuthorizedClient(registration, principal.getName(), token), principal))
                .verifyComplete();

        ServerOAuth2AuthorizedClientExchangeFilterFunction filter =
                new PaymentApiConfig().oauth2Filter(registrations, authorizedClients);

        AtomicReference<ClientRequest> sentRequest = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .filter(filter)
                .exchangeFunction(request -> {
                    sentRequest.set(request);
                    return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK).build());
                })
                .build();

        StepVerifier.create(webClient.get().uri("http://localhost/accounts/1/balance")
                        .retrieve().toBodilessEntity())
                .expectNextCount(1)
                .verifyComplete();

        assertEquals("Bearer test-token",
                sentRequest.get().headers().getFirst(HttpHeaders.AUTHORIZATION));
    }
}
