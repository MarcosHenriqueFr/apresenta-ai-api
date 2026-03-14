package com.example.projetogroq.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SlideStyleValidator.class)
public @interface ValidSlideStyle {
    String message() default "Invalid slide style. Should be BASIC, ACADEMIC or CREATIVE.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
