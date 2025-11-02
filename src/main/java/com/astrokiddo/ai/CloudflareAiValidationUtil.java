package com.astrokiddo.ai;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public final class CloudflareAiValidationUtil {
    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private CloudflareAiValidationUtil() {}

    public static <T> T validateOrThrow(T obj) {
        Set<ConstraintViolation<T>> v = VALIDATOR.validate(obj);
        if (!v.isEmpty()) {
            String msg = v.stream()
                    .map(cv -> cv.getPropertyPath() + " " + cv.getMessage())
                    .collect(Collectors.joining("; "));
            throw new ConstraintViolationException(msg, v);
        }
        return obj;
    }
}
