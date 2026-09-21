package com.rev.otus.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NoArgsConstructor;

/**
 * Валидатор для аннотации {@link NotBlankIfPresent}.
 *
 * <p>Проверяет, что если строка не null, то она не пустая и не состоит только из пробелов.
 * null считается валидным значением.
 *
 * <p>Используется в PATCH операциях, где null означает "поле не передано",
 * а если поле передано, оно должно быть непустым.
 *
 * @see NotBlankIfPresent
 */
@NoArgsConstructor
public class NotBlankIfPresentValidator implements ConstraintValidator<NotBlankIfPresent, String> {

    /**
     * Проверяет строку на валидность.
     *
     * @param value   проверяемая строка
     * @param context контекст валидации (не используется)
     * @return true если строка null или не пустая и не из пробелов, иначе false
     */
    @Override
    public boolean isValid(final String value, ConstraintValidatorContext context) {
        return value == null || !value.isBlank();
    }
}