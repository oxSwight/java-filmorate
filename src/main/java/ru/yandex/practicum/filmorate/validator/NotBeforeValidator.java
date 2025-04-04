package ru.yandex.practicum.filmorate.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.yandex.practicum.filmorate.annotations.NotBefore;

import java.time.LocalDate;

public class NotBeforeValidator implements ConstraintValidator<NotBefore, LocalDate> {
    private LocalDate minDate;

    @Override
    public void initialize(NotBefore constraintAnnotation) {
        minDate = LocalDate.parse(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value == null || value.isAfter(minDate);
    }
}