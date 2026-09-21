package com.rev.otus.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Проверяет, что если строка не null, то она не пустая и не из пробелов.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotBlankIfPresentValidator.class)
@Documented
public @interface NotBlankIfPresent {

    /**
     * Сообщение об ошибке валидации.
     */
    String message() default "{validation.notblank.ifpresent}";

    /**
     * Группы валидации.
     */
    Class<?>[] groups() default {};

    /**
     * Payload для расширения валидации.
     */
    Class<? extends Payload>[] payload() default {};
}