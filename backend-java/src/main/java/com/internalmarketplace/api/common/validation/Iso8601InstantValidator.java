package com.internalmarketplace.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class Iso8601InstantValidator implements ConstraintValidator<Iso8601Instant, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // optionality is enforced separately via @NotBlank/nullability
        }
        try {
            Instant.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
