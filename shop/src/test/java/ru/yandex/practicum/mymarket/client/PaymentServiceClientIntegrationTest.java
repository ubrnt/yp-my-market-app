package ru.yandex.practicum.mymarket.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.microcks.testcontainers.MicrocksContainer;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.BalanceResult;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient.PaymentResult;
import ru.yandex.practicum.mymarket.payment.ApiClient;
import ru.yandex.practicum.mymarket.payment.api.DefaultApi;

class PaymentServiceClientIntegrationTest {

    private static MicrocksContainer microcks;

    @BeforeAll
    static void startMock() throws Exception {
        microcks = new MicrocksContainer(DockerImageName.parse("quay.io/microcks/microcks-uber:1.11.0"));
        microcks.start();
        microcks.importAsMainArtifact(new File(System.getProperty("payment.openapi.spec")));
        microcks.importAsSecondaryArtifact(resource("payment-api-examples.yaml"));
    }

    @Test
    void getBalance_forKnownAccount_returnsAvailableBalance() {
        StepVerifier.create(clientFor(1).getBalance())
                .assertNext(result -> {
                    assertEquals(BalanceResult.Status.AVAILABLE, result.status());
                    assertEquals(100000, result.balance());
                })
                .verifyComplete();
    }

    @Test
    void getBalance_forUnknownAccount_returnsAccountNotFound() {
        StepVerifier.create(clientFor(999).getBalance())
                .assertNext(result -> assertEquals(BalanceResult.Status.ACCOUNT_NOT_FOUND, result.status()))
                .verifyComplete();
    }

    @Test
    void pay_succeeds() {
        StepVerifier.create(clientFor(1).pay(1000))
                .expectNext(PaymentResult.SUCCESS)
                .verifyComplete();
    }

    @Test
    void pay_returnsInsufficientFunds() {
        StepVerifier.create(clientFor(2).pay(1000))
                .expectNext(PaymentResult.INSUFFICIENT_FUNDS)
                .verifyComplete();
    }

    private static PaymentServiceClient clientFor(long accountId) {
        String baseUrl = microcks.getRestMockEndpoint("Payment Service API", "1.0.0");
        DefaultApi api = new DefaultApi(new ApiClient().setBasePath(baseUrl));
        return new PaymentServiceClient(api, accountId);
    }

    private static File resource(String name) throws URISyntaxException {
        return Path.of(PaymentServiceClientIntegrationTest.class.getClassLoader().getResource(name).toURI()).toFile();
    }
}
