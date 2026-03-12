package com.example.projetogroq.dto.validator;

import com.example.projetogroq.dto.SlideLevel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SlideLevelValidator implements ConstraintValidator<ValidSlideLevel, String> {

    @Override
    public void initialize(ValidSlideLevel constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }

        for(SlideLevel level : SlideLevel.values()){
            if(level.name().equals(value)){
                return true;
            }
        }

        return false;
    }
}
