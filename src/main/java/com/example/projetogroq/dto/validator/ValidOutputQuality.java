package com.example.projetogroq.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = OutputQualityValidator.class)
public @interface ValidOutputQuality {
    String message() default "Invalid quality. Should be STANDARD or PREMIUM.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
