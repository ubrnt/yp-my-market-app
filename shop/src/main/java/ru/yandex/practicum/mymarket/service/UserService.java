package ru.yandex.practicum.mymarket.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.client.PaymentServiceClient;
import ru.yandex.practicum.mymarket.domain.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentServiceClient paymentServiceClient;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PaymentServiceClient paymentServiceClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.paymentServiceClient = paymentServiceClient;
    }

    public Mono<RegistrationResult> register(String username, String password) {
        return userRepository.findByUsername(username)
                .map(existing -> RegistrationResult.USERNAME_TAKEN)
                .switchIfEmpty(Mono.defer(() -> createAccountAndUser(username, password)));
    }

    private Mono<RegistrationResult> createAccountAndUser(String username, String password) {
        return paymentServiceClient.createAccount()
                .flatMap(result -> switch (result.status()) {
                    case CREATED -> saveUser(username, password, result.accountId());
                    case UNAVAILABLE -> Mono.just(RegistrationResult.PAYMENT_UNAVAILABLE);
                });
    }

    private Mono<RegistrationResult> saveUser(String username, String password, Long accountId) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountId(accountId);

        return userRepository.save(user)
                .map(saved -> RegistrationResult.SUCCESS)
                .onErrorReturn(DataIntegrityViolationException.class, RegistrationResult.USERNAME_TAKEN);
    }

    public enum RegistrationResult {
        SUCCESS,
        USERNAME_TAKEN,
        PAYMENT_UNAVAILABLE
    }
}
