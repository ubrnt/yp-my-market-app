package ru.yandex.practicum.mymarket.controller;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.validation.PasswordsMatch;

@Controller
public class RegistrationController {

    private final UserService userService;
    private final MessageSource messageSource;

    public RegistrationController(UserService userService, MessageSource messageSource) {
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/register")
    public String registrationForm() {
        return "registration";
    }

    @PostMapping("/register")
    public Mono<String> register(@Valid @ModelAttribute RegistrationRequest request,
                                 BindingResult bindingResult,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            String error = bindingResult.getAllErrors().getFirst().getDefaultMessage();
            return Mono.just(renderError(request, error, model));
        }

        return userService.register(request.username().trim(), request.password())
                .map(result -> switch (result) {
                    case SUCCESS -> "redirect:/login?registered";
                    case USERNAME_TAKEN -> renderError(request, message("registration.username.taken"), model);
                    case PAYMENT_UNAVAILABLE -> renderError(request, message("registration.payment.unavailable"), model);
                });
    }

    private String message(String code) {
        return messageSource.getMessage(code, null, Locale.getDefault());
    }

    private String renderError(RegistrationRequest request, String error, Model model) {
        model.addAttribute("error", error);
        model.addAttribute("username", request.username());
        return "registration";
    }

    @PasswordsMatch
    public record RegistrationRequest(
            @NotBlank(message = "{registration.fields.required}") String username,
            @NotBlank(message = "{registration.fields.required}") String password,
            String confirmPassword) {
    }
}
