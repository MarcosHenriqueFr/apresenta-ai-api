package com.example.projetogroq.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SlideLevelValidator.class)
public @interface ValidSlideLevel {
    String message() default "Invalid Slide Level. Should be BASIC, INTERMEDIARY or ADVANCED";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
