package com.example.projetogroq.dto.validator;

import com.example.projetogroq.dto.SlideStyle;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SlideStyleValidator implements ConstraintValidator<ValidSlideStyle, String> {
    @Override
    public void initialize(ValidSlideStyle constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null){
            return true;
        }

        for(SlideStyle layout : SlideStyle.values()){
            if(layout.name().equals(value)){
                return true;
            }
        }

        return false;
    }
}
