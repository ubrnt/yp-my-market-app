package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.UserService;

@Controller
public class RegistrationController {

    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registrationForm() {
        return "registration";
    }

    @PostMapping("/register")
    public Mono<String> register(@ModelAttribute RegistrationRequest request, Model model) {
        String error = validate(request);
        if (error != null) {
            return Mono.just(renderError(request, error, model));
        }

        return userService.register(request.username().trim(), request.password())
                .map(result -> switch (result) {
                    case SUCCESS -> "redirect:/login?registered";
                    case USERNAME_TAKEN -> renderError(request, "Логин уже занят", model);
                    case PAYMENT_UNAVAILABLE ->
                            renderError(request, "Регистрация временно недоступна: сервис счетов не отвечает", model);
                });
    }

    private String validate(RegistrationRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return "Заполните логин и пароль";
        }
        if (!request.password().equals(request.confirmPassword())) {
            return "Пароли не совпадают";
        }
        return null;
    }

    private String renderError(RegistrationRequest request, String error, Model model) {
        model.addAttribute("error", error);
        model.addAttribute("username", request.username());
        return "registration";
    }

    public record RegistrationRequest(String username, String password, String confirmPassword) {
    }
}
