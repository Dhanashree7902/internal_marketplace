package com.internalmarketplace.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a String is a UTC ISO-8601 instant (e.g. {@code 2026-01-31T00:00:00.000Z}),
 * matching zod's default {@code z.string().datetime()} behavior used for
 * {@code expiryDate} in the Node post.controller.js. The value is stored and
 * returned as a plain string, never coerced to a date type, exactly as today.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Iso8601InstantValidator.class)
public @interface Iso8601Instant {

    String message() default "must be a valid UTC ISO-8601 datetime string";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
