package ru.yandex.practicum.mymarket.validation;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.yandex.practicum.mymarket.controller.RegistrationController.RegistrationRequest;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, RegistrationRequest> {

    @Override
    public boolean isValid(RegistrationRequest request, ConstraintValidatorContext context) {
        return Objects.equals(request.password(), request.confirmPassword());
    }
}
